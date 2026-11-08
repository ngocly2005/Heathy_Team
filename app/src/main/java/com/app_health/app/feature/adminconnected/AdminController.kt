package com.app_health.app.feature.adminconnected

import com.app_health.app.core.network.ApiFailure
import com.app_health.app.core.network.SessionRequestTag
import com.app_health.app.core.network.toUserMessage
import com.google.gson.JsonParseException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class AdminTab(val title: String) {
    DASHBOARD("Tổng quan"), USERS("Người dùng"), FOODS("Thực phẩm"), ACTIVITIES("Bài tập"), LOGS("Lịch sử AI")
}

data class AdminQuery(val tab: AdminTab = AdminTab.DASHBOARD, val page: Int = 1,
    val search: String = "", val status: String = "", val active: Boolean? = null,
    val from: String = LocalDate.now().minusDays(29).toString(), val to: String = LocalDate.now().toString(),
    val userId: String = "", val kind: String = "") {
    fun parameters(): Map<String, String> = buildMap {
        put("page", page.toString()); put("per_page", "20"); put("sort_field", "id"); put("sort_order", "DESC")
        if (search.isNotBlank()) put("q", search.trim())
        if (tab == AdminTab.USERS && status.isNotBlank()) put("status", status)
        if (tab == AdminTab.FOODS || tab == AdminTab.ACTIVITIES) active?.let { put("is_active", it.toString()) }
        if (tab == AdminTab.LOGS) {
            put("date_from", from); put("date_to", to)
            if (userId.isNotBlank()) put("user_id", userId.trim())
            if (kind.isNotBlank()) put("kind", kind)
        }
    }
}

fun adminQueryError(query: AdminQuery): String? {
    if (query.search.trim().length > 100) return "Từ khóa tối đa 100 ký tự."
    if (query.tab == AdminTab.DASHBOARD || query.tab == AdminTab.LOGS) {
        val from = runCatching { LocalDate.parse(query.from) }.getOrNull()
        val to = runCatching { LocalDate.parse(query.to) }.getOrNull()
        if (from == null || to == null || from > to) return "Nhập ngày YYYY-MM-DD và chọn Từ ngày không sau Đến ngày."
        if (query.tab == AdminTab.DASHBOARD && ChronoUnit.DAYS.between(from, to) > 365)
            return "Báo cáo hỗ trợ tối đa 366 ngày."
    }
    if (query.userId.isNotBlank() && query.userId.trim().toLongOrNull()?.let { it > 0 } != true)
        return "ID người dùng phải là số nguyên dương."
    return null
}

data class NativeAdminState(val query: AdminQuery = AdminQuery(), val loading: Boolean = false,
    val busy: Boolean = false, val authorized: Boolean = false, val denied: Boolean = false,
    val uncertain: Boolean = false, val error: String? = null, val notice: String? = null,
    val identity: AdminIdentity? = null, val report: AdminReport? = null, val users: List<AdminUserRow> = emptyList(),
    val foods: List<AdminFood> = emptyList(), val activities: List<AdminActivity> = emptyList(),
    val logs: List<AdminAiLog> = emptyList(), val total: Int = 0)

/** Independent from Android, so cancellation, owner changes and uncertain writes are JVM-testable. */
internal class AdminController(private val scope: CoroutineScope, private val api: AdminApi,
    private val currentOwner: () -> Long?, private val identityUpdated: suspend (AdminIdentity) -> Unit = {}) {
    private val mutable = MutableStateFlow(NativeAdminState())
    val state = mutable.asStateFlow()
    private var owner: Long? = null
    private var revision = 0L
    private var readJob: Job? = null
    private var writeJob: Job? = null
    private var uncertainQuery: AdminQuery? = null

    fun activate() {
        val id = currentOwner() ?: return
        if (owner != id) { release(); owner = id }
        refresh()
    }
    fun release() {
        revision++; owner = null; readJob?.cancel(); writeJob?.cancel(); uncertainQuery = null
        mutable.value = NativeAdminState()
    }
    private fun valid(id: Long, version: Long) = owner == id && currentOwner() == id && revision == version
    private fun requireCurrent(id: Long, version: Long) {
        if (!valid(id, version)) throw CancellationException("Admin account or request changed")
    }
    private suspend fun permission(id: Long, version: Long): Boolean {
        val user = api.me(SessionRequestTag(id))
        currentCoroutineContext().ensureActive(); requireCurrent(id, version)
        if (user.id != id || user.status != "active" || user.role != "admin") {
            // Revocation is authoritative even if encrypted identity persistence subsequently fails.
            deny()
            if (user.id == id) identityUpdated(user)
            requireCurrent(id, version); return false
        }
        identityUpdated(user)
        requireCurrent(id, version)
        mutable.update { it.copy(identity = user, authorized = true, denied = false) }
        return true
    }
    private fun deny() {
        // Remove privileged data immediately when the live server rejects access.
        val query = mutable.value.query
        mutable.value = NativeAdminState(query = query, denied = true,
            error = "Tài khoản hiện không có quyền quản trị hoặc phiên không còn hợp lệ.")
    }
    fun select(query: AdminQuery) {
        if (mutable.value.busy || mutable.value.uncertain) return
        val error = adminQueryError(query)
        if (error != null) { mutable.update { it.copy(error = error) }; return }
        mutable.update { it.copy(query = query.copy(page = query.page.coerceAtLeast(1)), report = null,
            users = emptyList(), foods = emptyList(), activities = emptyList(), logs = emptyList(), total = 0) }
        refresh()
    }
    fun refresh() {
        val id = owner ?: return
        if (mutable.value.busy) return
        revision++; readJob?.cancel()
        val version = revision
        val query = uncertainQuery ?: mutable.value.query
        // Reconciliation must finish on the originating resource before any further mutation.
        mutable.update { it.copy(query = query, loading = true, error = null, notice = null) }
        readJob = scope.launch {
            try {
                if (!permission(id, version)) return@launch
                load(query, id, version)
                requireCurrent(id, version)
                uncertainQuery = null
                mutable.update { it.copy(uncertain = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (valid(id, version)) {
                    if (error is HttpException && error.code() in listOf(401, 403)) deny()
                    else if (!mutable.value.denied) mutable.update { it.copy(error = if (it.uncertain)
                        "Chưa đối chiếu được thao tác trước: ${error.toUserMessage()}" else error.toUserMessage()) }
                }
            } finally { if (valid(id, version)) mutable.update { it.copy(loading = false) } }
        }
    }
    private suspend fun load(query: AdminQuery, id: Long, version: Long) {
        val tag = SessionRequestTag(id)
        val params = query.parameters()
        val update: (NativeAdminState) -> NativeAdminState = when (query.tab) {
            AdminTab.DASHBOARD -> { val result = api.analytics(tag, query.from, query.to); { it.copy(report = result) } }
            AdminTab.USERS -> { val result = api.users(tag, params); { it.copy(users = result.items, total = result.total) } }
            AdminTab.FOODS -> { val result = api.foods(tag, params); { it.copy(foods = result.items, total = result.total) } }
            AdminTab.ACTIVITIES -> { val result = api.activities(tag, params); { it.copy(activities = result.items, total = result.total) } }
            AdminTab.LOGS -> { val result = api.logs(tag, params); { it.copy(logs = result.items, total = result.total) } }
        }
        currentCoroutineContext().ensureActive(); requireCurrent(id, version)
        mutable.update(update)
    }
    fun write(operation: suspend (SessionRequestTag) -> Unit, success: () -> Unit = {}) {
        val id = owner ?: return
        val current = mutable.value
        if (!current.authorized || current.busy || current.loading || current.uncertain || current.denied) return
        revision++; readJob?.cancel()
        val version = revision
        val query = current.query
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        writeJob = scope.launch {
            var submitted = false
            var saved = false
            try {
                if (!permission(id, version)) return@launch
                requireCurrent(id, version)
                submitted = true
                operation(SessionRequestTag(id))
                currentCoroutineContext().ensureActive(); requireCurrent(id, version)
                saved = true
                success()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (valid(id, version)) {
                    if (error is HttpException && error.code() in listOf(401, 403)) deny()
                    else if (!mutable.value.denied) {
                        val unknown = submitted && !saved && ((error is IOException && error !is ApiFailure) ||
                            error is JsonParseException || error is HttpException && error.code() >= 500)
                        if (unknown) uncertainQuery = query
                        mutable.update { it.copy(uncertain = unknown, error = if (unknown)
                            "Chưa rõ kết quả thao tác. Bấm Tải lại, kiểm tra dữ liệu trước khi gửi tiếp."
                            else error.toUserMessage()) }
                    }
                }
            } finally {
                if (valid(id, version)) {
                    mutable.update { it.copy(busy = false) }
                    if (saved) {
                        refresh()
                        mutable.update { it.copy(notice = "Đã lưu.") }
                    }
                }
            }
        }
    }
}
