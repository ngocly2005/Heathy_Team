package com.app_health.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app_health.admin.viewmodel.AdminDataViewModel
import com.app_health.app.feature.home.model.FoodItem
import com.app_health.app.feature.home.model.ExerciseItem
import com.app_health.app.feature.home.model.ActivityInputType

private val AdminGreen = Color(0xFF14453D)
private val AdminMint = Color(0xFF10B981)
private val AdminBg = Color(0xFFF4F7F6)
private val AdminText = Color(0xFF172B4D)
private val AdminMuted = Color(0xFF84919E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDataScreen(
    viewModel: AdminDataViewModel = viewModel()
) {
    val foods by viewModel.foodList.collectAsState()
    val exercises by viewModel.exerciseList.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var showFoodDialog by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }

    val filteredFoods = foods.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    val filteredExercises = exercises.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    if (showFoodDialog) {
        AddFoodDialog(
            onDismiss = { showFoodDialog = false },
            onAdd = { newFood ->
                viewModel.addFood(newFood)
                showFoodDialog = false
            }
        )
    }

    if (showExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showExerciseDialog = false },
            onAdd = { newExercise ->
                viewModel.addExercise(newExercise)
                showExerciseDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column {
                    Text("Quản lý dữ liệu", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = AdminText)
                    Spacer(Modifier.height(6.dp))
                    Text("Quản lý kho thực phẩm và danh mục bài tập của FitChat.", fontSize = 13.sp, color = AdminMuted)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatDataCard(Modifier.weight(1f), "🥗", "Thực phẩm", "${foods.size}", "Mục dữ liệu")
                    StatDataCard(Modifier.weight(1f), "🏃", "Bài tập", "${exercises.size}", "Hoạt động")
                    StatDataCard(Modifier.weight(1f), "📊", "Tổng cộng", "${foods.size + exercises.size}", "Dữ liệu")
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Row(modifier = Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        DataTab(Modifier.weight(1f), "🥗  Thực phẩm", selectedTab == 0) {
                            selectedTab = 0
                            searchQuery = ""
                        }
                        DataTab(Modifier.weight(1f), "🏋️  Bài tập", selectedTab == 1) {
                            selectedTab = 1
                            searchQuery = ""
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm kiếm dữ liệu...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = AdminMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = AdminMint,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    FilledTonalButton(
                        onClick = {
                            if (selectedTab == 0) showFoodDialog = true
                            else showExerciseDialog = true
                        },
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AdminGreen, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm", fontSize = 12.sp)
                    }
                }
            }

            if (selectedTab == 0) {
                if (filteredFoods.isEmpty()) {
                    item { EmptyDataView() }
                } else {
                    items(filteredFoods) { food -> FoodDataCard(food) }
                }
            } else {
                if (filteredExercises.isEmpty()) {
                    item { EmptyDataView() }
                } else {
                    items(filteredExercises) { exercise -> ExerciseDataCard(exercise) }
                }
            }
        }
    }
}

@Composable
private fun AddFoodDialog(onDismiss: () -> Unit, onAdd: (FoodItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm thực phẩm", fontWeight = FontWeight.Bold, color = AdminText) },
        containerColor = Color.White,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên thực phẩm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories (kcal)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && calories.isNotBlank()) {
                        onAdd(
                            FoodItem(
                                name = name.trim(),
                                calories = calories.toIntOrNull() ?: 0,
                                protein = protein.toIntOrNull() ?: 0,
                                carbs = carbs.toIntOrNull() ?: 0,
                                fat = fat.toIntOrNull() ?: 0
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminMint)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = AdminMuted) } }
    )
}

@Composable
private fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (ExerciseItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm bài tập", fontWeight = FontWeight.Bold, color = AdminText) },
        containerColor = Color.White,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên bài tập") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Danh mục (VD: C)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            ExerciseItem(
                                name = name.trim(),
                                category = category.ifBlank { "A" },
                                inputType = ActivityInputType.LEVEL,
                                levelOptions = listOf("Cơ bản", "Nâng cao")
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminMint)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = AdminMuted) } }
    )
}

@Composable
private fun StatDataCard(modifier: Modifier, emoji: String, title: String, value: String, subtitle: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(11.dp)) {
            Box(modifier = Modifier.size(34.dp).background(Color(0xFFE8F5F0), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 19.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = AdminGreen)
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AdminText)
            Text(subtitle, fontSize = 10.sp, color = AdminMuted)
        }
    }
}

@Composable
private fun DataTab(modifier: Modifier, title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(43.dp),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) AdminGreen else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else AdminMuted)
        }
    }
}

@Composable
private fun FoodDataCard(food: FoodItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(52.dp).background(Color(0xFFF0F7F3), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Text("🥗", fontSize = 28.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(food.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AdminText)
                    Spacer(Modifier.height(5.dp))
                    Text("P: ${food.protein}g | C: ${food.carbs}g | F: ${food.fat}g", fontSize = 11.sp, color = AdminMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${food.calories}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = AdminMint)
                    Text("kcal", fontSize = 11.sp, color = AdminMuted)
                }
            }
        }
    }
}

@Composable
private fun ExerciseDataCard(exercise: ExerciseItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(54.dp).background(Color(0xFFE8F5F0), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text("🏃", fontSize = 27.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AdminText)
                Spacer(Modifier.height(5.dp))
                Text("Danh mục: ${exercise.category}", fontSize = 12.sp, color = AdminMuted)
                Spacer(Modifier.height(4.dp))
                Text("Kiểu nhập: ${exercise.inputType.name}", fontSize = 11.sp, color = AdminMuted)
            }
        }
    }
}

@Composable
private fun EmptyDataView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", fontSize = 35.sp)
            Spacer(Modifier.height(10.dp))
            Text("Không tìm thấy dữ liệu", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AdminText)
        }
    }
}