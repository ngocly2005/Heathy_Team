package com.app_health.app.feature.adminconnected

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app_health.app.feature.integrated.*
import com.google.gson.GsonBuilder
import java.math.BigDecimal

internal fun canChangeUserStatus(row: AdminUserRow, ownerId: Long?): Boolean = row.role != "admin" && row.id != ownerId

@Composable
internal fun AdminUsers(vm: NativeAdminViewModel, state: NativeAdminState, enabled: Boolean) {
    var selected by remember { mutableStateOf<AdminUserRow?>(null) }
    if (state.users.isEmpty() && !state.loading) Text("Không có người dùng phù hợp.", color = Muted)
    state.users.forEach { user -> HealthCard(user.full_name) {
        SelectionContainer { Text(user.email) }
        Text("ID ${user.id} • ${if (user.role == "admin") "Admin" else "Người dùng"}", color = Muted)
        Text(if (user.status == "active") "Đang hoạt động" else "Đã khóa", color = Mint)
        if (canChangeUserStatus(user, state.identity?.id))
            OutlinedButton(enabled = enabled, onClick = { selected = user }) { Text(if (user.status == "active") "Khóa tài khoản" else "Mở khóa") }
        else Text("Tài khoản Admin được bảo vệ khỏi khóa tại đây.", style = MaterialTheme.typography.bodySmall, color = Muted)
    } }
    selected?.let { target ->
        AlertDialog(onDismissRequest = { if (!state.busy) selected = null }, title = { Text("Đổi trạng thái?") },
            text = { Text("${target.email}\n${if (target.status == "active") "Khóa tài khoản và thu hồi phiên đăng nhập." else "Cho phép tài khoản hoạt động trở lại."}") },
            confirmButton = { TextButton(enabled = enabled, onClick = {
                vm.write(operation = { owner -> vm.api.status(owner, target.id,
                    mapOf("status" to if (target.status == "active") "disabled" else "active")) }, success = { selected = null })
            }) { Text("Xác nhận") } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = { selected = null }) { Text("Hủy") } })
    }
}

data class CatalogDraft(val name: String = "", val values: List<String> = listOf("0", "0", "0", "0"), val active: Boolean = true)
internal fun catalogError(draft: CatalogDraft, activity: Boolean): String? {
    if (draft.name.trim().isEmpty() || draft.name.trim().length > if (activity) 120 else 160)
        return "Nhập tên hợp lệ, tối đa ${if (activity) 120 else 160} ký tự."
    val numbers = draft.values.map { inputNumber(it) }
    if (numbers.size != (if (activity) 1 else 4) || numbers.any { it == null }) return "Nhập đầy đủ các giá trị số hợp lệ."
    if (activity) return if (numbers[0]!! in 0.01..30.0) null else "MET phải từ 0,01 đến 30."
    val limits = listOf(20000.0, 2000.0, 3000.0, 2000.0)
    return if (numbers.indices.any { numbers[it]!! < 0 || numbers[it]!! > limits[it] })
        "Giá trị /100 g: calo 0–20.000; Protein/Fat 0–2.000; Carb 0–3.000." else null
}
internal fun catalogPayload(draft: CatalogDraft, activity: Boolean): Map<String, Any?> {
    require(catalogError(draft, activity) == null)
    val fields = if (activity) listOf("met") else listOf("calories_per_100g", "protein_per_100g", "carb_per_100g", "fat_per_100g")
    return buildMap { put("name", draft.name.trim()); put("is_active", draft.active)
        fields.forEachIndexed { index, field -> put(field, inputNumber(draft.values[index])!!) } }
}
private fun editNumber(value: Double) = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

@Composable
internal fun AdminCatalog(vm: NativeAdminViewModel, state: NativeAdminState, enabled: Boolean) {
    val activity = state.query.tab == AdminTab.ACTIVITIES
    var editing by remember(state.query.tab) { mutableStateOf(false) }
    var editId by remember(state.query.tab) { mutableStateOf<Long?>(null) }
    var draft by remember(state.query.tab) { mutableStateOf(CatalogDraft()) }
    var deactivate by remember(state.query.tab) { mutableStateOf<Pair<Long, String>?>(null) }
    Button(enabled = enabled, onClick = { editId = null; draft = CatalogDraft(values = if (activity) listOf("1") else listOf("0", "0", "0", "0")); editing = true }) {
        Text(if (activity) "Thêm bài tập" else "Thêm thực phẩm")
    }
    if ((if (activity) state.activities.isEmpty() else state.foods.isEmpty()) && !state.loading) Text("Không có mục phù hợp.", color = Muted)
    if (activity) state.activities.forEach { item -> HealthCard(item.name) {
        Text("MET ${item.met.uiNumber(2)} • ${if (item.is_active) "Đang sử dụng" else "Đã ngừng"}", color = Muted)
        CatalogActions(enabled, item.is_active, onEdit = {
            editId = item.id; draft = CatalogDraft(item.name, listOf(editNumber(item.met)), item.is_active); editing = true
        }, onDeactivate = { deactivate = item.id to item.name })
    } } else state.foods.forEach { item -> HealthCard(item.name) {
        Text("${item.calories_per_100g.uiNumber(2)} kcal / 100 g", color = Mint)
        Text("Protein ${item.protein_per_100g.uiNumber(2)} g • Carb ${item.carb_per_100g.uiNumber(2)} g • Fat ${item.fat_per_100g.uiNumber(2)} g", color = Muted)
        Text(if (item.is_active) "Đang sử dụng" else "Đã ngừng", color = Muted)
        CatalogActions(enabled, item.is_active, onEdit = {
            editId = item.id; draft = CatalogDraft(item.name,
                listOf(item.calories_per_100g, item.protein_per_100g, item.carb_per_100g, item.fat_per_100g).map(::editNumber), item.is_active); editing = true
        }, onDeactivate = { deactivate = item.id to item.name })
    } }
    if (editing) CatalogEditor(draft, activity, editId != null, enabled, state.busy, { draft = it }, { editing = false }) {
        val payload = catalogPayload(draft, activity)
        val id = editId
        vm.write(operation = { owner ->
            if (activity) { if (id == null) vm.api.createActivity(owner, payload) else vm.api.editActivity(owner, id, payload) }
            else { if (id == null) vm.api.createFood(owner, payload) else vm.api.editFood(owner, id, payload) }
        }, success = { editing = false })
    }
    deactivate?.let { target -> AlertDialog(onDismissRequest = { if (!state.busy) deactivate = null },
        title = { Text("Ngừng sử dụng?") }, text = { Text("${target.second}\nNhật ký đã ghi vẫn được giữ nguyên.") },
        confirmButton = { TextButton(enabled = enabled, onClick = { vm.write(operation = { owner ->
            vm.api.deactivate(owner, if (activity) "activities" else "foods", target.first)
        }, success = { deactivate = null }) }) { Text("Ngừng sử dụng") } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = { deactivate = null }) { Text("Hủy") } }) }
}

@Composable
private fun CatalogActions(enabled: Boolean, active: Boolean, onEdit: () -> Unit, onDeactivate: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = enabled, onClick = onEdit) { Text("Sửa") }
        TextButton(enabled = enabled && active, onClick = onDeactivate) { Text("Ngừng sử dụng") }
    }
}

@Composable
internal fun CatalogEditor(draft: CatalogDraft, activity: Boolean, existing: Boolean,
    enabled: Boolean, busy: Boolean, change: (CatalogDraft) -> Unit, dismiss: () -> Unit, save: () -> Unit) {
    val error = catalogError(draft, activity)
    AlertDialog(onDismissRequest = { if (!busy) dismiss() },
        title = { Text("${if (existing) "Sửa" else "Thêm"} ${if (activity) "bài tập" else "thực phẩm"}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(draft.name, { change(draft.copy(name = it.take(if (activity) 120 else 160))) },
                enabled = enabled, label = { Text("Tên") }, modifier = Modifier.fillMaxWidth())
            val labels = if (activity) listOf("MET") else listOf("Calo /100 g", "Protein /100 g", "Carb /100 g", "Fat /100 g")
            labels.forEachIndexed { index, label -> NumberInput(label, draft.values.getOrElse(index) { "" }, enabled) { value ->
                change(draft.copy(values = draft.values.toMutableList().apply { set(index, value) }))
            } }
            Row { Checkbox(draft.active, onCheckedChange = { change(draft.copy(active = it)) }, enabled = enabled); Text("Đang sử dụng") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        } },
        confirmButton = { TextButton(enabled = enabled && error == null, onClick = save) { Text("Lưu") } },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Hủy") } })
}

@Composable
internal fun AdminLogs(state: NativeAdminState) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    if (state.logs.isEmpty() && !state.loading) Text("Không có lịch sử AI trong bộ lọc này.", color = Muted)
    state.logs.forEach { log -> HealthCard("#${log.id} • ${if (log.kind == "chat") "Chat" else "Phân tích ảnh"}") {
        Text("User ${log.user_id} • ngày dữ liệu ${log.context_date}", color = Muted)
        Text("${if (log.status == "success") "Thành công" else "Lỗi"}${log.error_code?.let { " • HTTP $it" } ?: ""}")
        log.question?.let { Text(it) }
        TextButton(onClick = { selectedId = log.id }) { Text("Xem đầy đủ phản hồi") }
    } }
    state.logs.firstOrNull { it.id == selectedId }?.let { log ->
        AlertDialog(onDismissRequest = { selectedId = null }, title = { Text("Tương tác AI #${log.id}") },
            text = { SelectionContainer { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("User ID: ${log.user_id}\nLoại: ${log.kind}\nNgày dữ liệu: ${log.context_date}\nThời điểm: ${log.created_at}\nTrạng thái: ${log.status}")
                log.error_code?.let { Text("Mã lỗi: $it") }
                log.question?.let { Text("Câu hỏi:\n$it") }
                Text("Phản hồi:\n${log.response?.let { GsonBuilder().setPrettyPrinting().create().toJson(it) } ?: "Không có phản hồi hợp lệ."}")
            } } }, confirmButton = { TextButton(onClick = { selectedId = null }) { Text("Đóng") } })
    }
}
