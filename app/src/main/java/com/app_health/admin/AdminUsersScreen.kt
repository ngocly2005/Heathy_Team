
package com.app_health.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ================= COLORS =================

private val AdminGreen = Color(0xFF119B72)
private val AdminGreenDark = Color(0xFF087653)
private val AdminBackground = Color(0xFFF4F7F6)
private val AdminText = Color(0xFF172B3A)
private val AdminMuted = Color(0xFF84929B)
private val AdminBorder = Color(0xFFE8EFEC)

// ================= MODEL =================

data class UserItem(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val isActive: Boolean
)

// ================= MAIN SCREEN =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen() {

    val users = remember {
        mutableStateListOf(
            UserItem("1", "Nguyễn Văn An", "an@gmail.com", "Admin", true),
            UserItem("2", "Trần Minh Anh", "minhanh@gmail.com", "User", true),
            UserItem("3", "Lê Hoàng Nam", "nam@gmail.com", "User", false),
            UserItem("4", "Phạm Thu Hà", "ha@gmail.com", "User", true),
            UserItem("5", "Đỗ Minh Quân", "quan@gmail.com", "User", true)
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var showAddDialog by remember { mutableStateOf(false) }

    val activeCount = users.count { it.isActive }
    val inactiveCount = users.size - activeCount

    val filteredUsers = users.filter { user ->

        val matchesSearch =
            user.name.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Đang hoạt động" -> user.isActive
            "Đã khóa" -> !user.isActive
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBackground)
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ================= HEADER =================

            item {
                Column {

                    Text(
                        text = "Quản lý người dùng",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminText
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = "Quản lý tài khoản và trạng thái thành viên FitChat",
                        fontSize = 13.sp,
                        color = AdminMuted
                    )
                }
            }

            // ================= STATISTICS =================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    UserStatCard(
                        title = "Tổng người dùng",
                        value = users.size.toString(),
                        icon = "people",
                        modifier = Modifier.weight(1f)
                    )

                    UserStatCard(
                        title = "Hoạt động",
                        value = activeCount.toString(),
                        icon = "active",
                        modifier = Modifier.weight(1f)
                    )

                    UserStatCard(
                        title = "Đã khóa",
                        value = inactiveCount.toString(),
                        icon = "blocked",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ================= SEARCH =================

            item {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Tìm tên hoặc email...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = AdminMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = AdminBorder,
                        focusedBorderColor = AdminGreen
                    )
                )
            }

            // ================= FILTER =================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    listOf(
                        "Tất cả",
                        "Đang hoạt động",
                        "Đã khóa"
                    ).forEach { filter ->

                        val selected = selectedFilter == filter

                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedFilter = filter
                            },
                            label = {
                                Text(
                                    filter,
                                    fontSize = 11.sp
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdminGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = AdminText
                            )
                        )
                    }
                }
            }

            // ================= SECTION HEADER =================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Column {

                        Text(
                            "Danh sách thành viên",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdminText
                        )

                        Text(
                            "${filteredUsers.size} tài khoản được tìm thấy",
                            fontSize = 12.sp,
                            color = AdminMuted
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AdminGreen
                        ),
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                    ) {

                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )

                        Spacer(Modifier.width(4.dp))

                        Text(
                            "Thêm mới",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ================= USER LIST =================

            if (filteredUsers.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = AdminMuted,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                "Không tìm thấy người dùng",
                                fontWeight = FontWeight.Bold,
                                color = AdminText
                            )

                            Text(
                                "Thử thay đổi từ khóa tìm kiếm hoặc bộ lọc.",
                                fontSize = 12.sp,
                                color = AdminMuted
                            )
                        }
                    }
                }

            } else {

                items(
                    filteredUsers,
                    key = { it.id }
                ) { user ->

                    UserCard(
                        user = user,
                        onToggleStatus = {

                            val index = users.indexOfFirst {
                                it.id == user.id
                            }

                            if (index != -1) {
                                users[index] = users[index].copy(
                                    isActive = !users[index].isActive
                                )
                            }
                        }
                    )
                }
            }

            // ================= FOOTER =================

            item {

                Text(
                    text = "FitChat Admin • User Management",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = AdminMuted,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // ================= ADD USER DIALOG =================

    if (showAddDialog) {

        AddUserDialog(
            onDismiss = {
                showAddDialog = false
            },
            onAdd = { newUser ->

                users.add(newUser)
                showAddDialog = false
            }
        )
    }
}

// ================= STAT CARD =================

@Composable
private fun UserStatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {

    val iconColor = when (icon) {
        "people" -> AdminGreen
        "active" -> Color(0xFF16A36A)
        else -> Color(0xFFEF8A45)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(11.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {

                when (icon) {

                    "people" -> Icon(
                        Icons.Default.People,
                        null,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )

                    "active" -> Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )

                    else -> Icon(
                        Icons.Default.Block,
                        null,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AdminText
            )

            Text(
                title,
                fontSize = 10.sp,
                color = AdminMuted,
                maxLines = 2
            )
        }
    }
}

// ================= USER CARD =================

@Composable
private fun UserCard(
    user: UserItem,
    onToggleStatus: () -> Unit
) {

    val activeColor = Color(0xFF159B68)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Avatar

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (user.isActive)
                                Color(0xFFE0F4EC)
                            else
                                Color(0xFFFDEAEA)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "?",
                        color = if (user.isActive)
                            AdminGreenDark
                        else
                            Color(0xFFDC5555),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        user.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminText
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        user.email,
                        fontSize = 11.sp,
                        color = AdminMuted
                    )
                }

                // Status

                Surface(
                    color = if (user.isActive)
                        Color(0xFFE4F6EC)
                    else
                        Color(0xFFFFEAEA),
                    shape = RoundedCornerShape(20.dp)
                ) {

                    Text(
                        text = if (user.isActive)
                            "Hoạt động"
                        else
                            "Đã khóa",
                        color = if (user.isActive)
                            Color(0xFF168454)
                        else
                            Color(0xFFD34F4F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            HorizontalDivider(
                color = AdminBorder
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        "Vai trò:",
                        fontSize = 12.sp,
                        color = AdminMuted
                    )

                    Spacer(Modifier.width(7.dp))

                    Surface(
                        color = if (user.role == "Admin")
                            Color(0xFFE7F0FF)
                        else
                            Color(0xFFF0F2F4),
                        shape = RoundedCornerShape(7.dp)
                    ) {

                        Text(
                            user.role,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (user.role == "Admin")
                                Color(0xFF3473C5)
                            else
                                Color(0xFF64748B),
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                        )
                    }
                }

                TextButton(
                    onClick = onToggleStatus,
                    contentPadding = PaddingValues(
                        horizontal = 8.dp
                    )
                ) {

                    Icon(
                        imageVector = if (user.isActive)
                            Icons.Default.Block
                        else
                            Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (user.isActive)
                            Color(0xFFE16B58)
                        else
                            activeColor
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = if (user.isActive)
                            "Khóa tài khoản"
                        else
                            "Mở khóa",
                        fontSize = 11.sp,
                        color = if (user.isActive)
                            Color(0xFFE16B58)
                        else
                            activeColor
                    )
                }
            }
        }
    }
}

// ================= ADD USER DIALOG =================

@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onAdd: (UserItem) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("User") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,

        title = {
            Text(
                "Thêm người dùng mới",
                fontWeight = FontWeight.Bold,
                color = AdminText
            )
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và tên") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    "Vai trò tài khoản",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AdminText
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    listOf("User", "Admin").forEach { option ->

                        FilterChip(
                            selected = role == option,
                            onClick = { role = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdminGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (
                        name.isNotBlank() &&
                        android.util.Patterns.EMAIL_ADDRESS
                            .matcher(email.trim()).matches()
                    ) {

                        onAdd(
                            UserItem(
                                id = System.currentTimeMillis().toString(),
                                name = name.trim(),
                                email = email.trim(),
                                role = role,
                                isActive = true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminGreen
                ),
                shape = RoundedCornerShape(10.dp)
            ) {

                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(Modifier.width(5.dp))

                Text("Thêm người dùng")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    "Hủy",
                    color = AdminMuted
                )
            }
        }
    )
}
