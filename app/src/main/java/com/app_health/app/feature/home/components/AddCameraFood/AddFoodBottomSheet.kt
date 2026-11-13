package com.app_health.app.feature.home.components.AddCameraFood

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToSelectFood: () -> Unit,
    onNavigateToCaptureFood: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Thêm bữa ăn",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Nút Chọn món ăn (Chuyển sang FoodSelectScreen)
            Button(
                onClick = {
                    onDismiss()
                    onNavigateToSelectFood()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Chọn món ăn thủ công", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Chụp món ăn
            FilledTonalButton(
                onClick = {
                    onDismiss()
                    onNavigateToCaptureFood()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE9E0F7),
                    contentColor = androidx.compose.ui.graphics.Color(0xFF232D3C))
            ) {
                Text("Chụp ảnh món ăn", fontSize = 16.sp)
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun AddFoodBottomSheetPreview() {
    AddFoodBottomSheet(
        onDismiss = {},
        onNavigateToSelectFood = {},
        onNavigateToCaptureFood = {}
    )
}
