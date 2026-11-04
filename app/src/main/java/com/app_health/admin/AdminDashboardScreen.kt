
package com.app_health.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ================= COLORS =================

private val AdminGreen = Color(0xFF10B981)
private val AdminGreenDark = Color(0xFF047857)
private val AdminGreenLight = Color(0xFFD1FAE5)
private val AdminBackground = Color(0xFFF4F8F7)
private val AdminText = Color(0xFF172B4D)
private val AdminMuted = Color(0xFF84939F)
private val AdminBorder = Color(0xFFE7EEEB)
private val AdminWhite = Color.White

// ================= DATA =================

data class AILogItem(
    val id: String,
    val userEmail: String,
    val prompt: String,
    val time: LocalDateTime
)

// ================= MAIN DASHBOARD =================

@Composable
fun AdminDashboardScreen() {

    val aiLogs = listOf(
        AILogItem(
            "1",
            "giakhanh@gmail.com",
            "Gợi ý thực đơn 1500 kcal",
            LocalDateTime.now().minusMinutes(10)
        ),
        AILogItem(
            "2",
            "nhuquynh@gmail.com",
            "Cách giảm mỡ bụng nhanh",
            LocalDateTime.now().minusHours(1)
        ),
        AILogItem(
            "3",
            "vanvinh@gmail.com",
            "Tính lượng Protein mỗi ngày",
            LocalDateTime.now().minusHours(3)
        ),
        AILogItem(
            "4",
            "vanmanh@gmail.com",
            "Gợi ý bài tập thể dục",
            LocalDateTime.now().minusHours(5)
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBackground),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 18.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // HEADER

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Tổng quan hệ thống",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminText
                )

                Text(
                    text = "Theo dõi và quản lý hoạt động ứng dụng",
                    fontSize = 13.sp,
                    color = AdminMuted
                )
            }
        }

        // STATISTICS

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Tổng người dùng",
                    value = "1,835",
                    trend = "+2.1%",
                    bgColor = AdminGreenDark,
                    iconType = "users"
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Lượt dùng AI",
                    value = "5,024",
                    trend = "+15%",
                    bgColor = Color.White,
                    textColor = AdminText,
                    iconType = "ai"
                )
            }
        }

        // QUICK INFORMATION

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = AdminGreenDark,
                    modifier = Modifier.size(19.dp)
                )

                Text(
                    text = "Hiệu suất hoạt động",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminText
                )
            }
        }

        // CHART

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AdminWhite
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(17.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hoạt động 7 ngày qua",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminText
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Thống kê lượt truy cập",
                                fontSize = 12.sp,
                                color = AdminMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = AdminGreenLight
                        ) {
                            Text(
                                text = "7 ngày",
                                modifier = Modifier.padding(
                                    horizontal = 11.dp,
                                    vertical = 7.dp
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AdminGreenDark
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    DetailedBarChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    )
                }
            }
        }

        // AI HISTORY HEADER

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "Lịch sử tư vấn AI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminText
                    )

                    Text(
                        text = "Hoạt động tư vấn gần đây",
                        fontSize = 12.sp,
                        color = AdminMuted
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = AdminGreenLight
                ) {
                    Text(
                        text = aiLogs.size.toString(),
                        modifier = Modifier.padding(
                            horizontal = 11.dp,
                            vertical = 6.dp
                        ),
                        color = AdminGreenDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // AI LOG ITEMS

        items(
            items = aiLogs,
            key = { it.id }
        ) { log ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AdminWhite
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // AI ICON

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                AdminGreenLight,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AdminGreenDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // USER INFORMATION

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text = log.prompt,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AdminText
                        )

                        Text(
                            text = log.userEmail,
                            fontSize = 11.sp,
                            color = AdminMuted
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // TIME

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        Text(
                            text = log.time.format(
                                DateTimeFormatter.ofPattern("HH:mm")
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AdminGreenDark
                        )

                        Text(
                            text = log.time.format(
                                DateTimeFormatter.ofPattern("dd/MM")
                            ),
                            fontSize = 10.sp,
                            color = AdminMuted
                        )
                    }
                }
            }
        }
    }
}

// ================= STATISTIC CARD =================

@Composable
fun StatCard(
    modifier: Modifier,
    title: String,
    value: String,
    trend: String,
    bgColor: Color,
    textColor: Color = Color.White,
    iconType: String = "users"
) {

    val iconColor =
        if (textColor == Color.White) Color.White
        else AdminGreenDark

    val secondaryColor =
        if (textColor == Color.White) Color.White.copy(alpha = 0.75f)
        else AdminMuted

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (textColor == Color.White)
                                Color.White.copy(alpha = 0.15f)
                            else AdminGreenLight,
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = when (iconType) {
                            "ai" -> Icons.Default.AutoAwesome
                            else -> Icons.Default.People
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = if (textColor == Color.White)
                        Color.White.copy(alpha = 0.8f)
                    else AdminGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                color = secondaryColor
            )

            Text(
                text = value,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (textColor == Color.White)
                        Color.White.copy(alpha = 0.15f)
                    else AdminGreenLight
                ) {
                    Text(
                        text = trend,
                        modifier = Modifier.padding(
                            horizontal = 7.dp,
                            vertical = 4.dp
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (textColor == Color.White)
                            Color.White
                        else AdminGreenDark
                    )
                }

                Spacer(Modifier.width(5.dp))

                Text(
                    text = "so với trước",
                    fontSize = 9.sp,
                    color = secondaryColor
                )
            }
        }
    }
}

// ================= BAR CHART =================

@Composable
fun DetailedBarChart(modifier: Modifier) {

    val data = listOf(
        400f, 700f, 500f, 900f,
        600f, 1000f, 800f
    )

    val labels = listOf(
        "T2", "T3", "T4", "T5",
        "T6", "T7", "CN"
    )

    val maxValue = 1000f

    Canvas(
        modifier = modifier
    ) {

        val leftPadding = 37.dp.toPx()
        val rightPadding = 5.dp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 25.dp.toPx()

        val chartWidth =
            size.width - leftPadding - rightPadding

        val chartHeight =
            size.height - topPadding - bottomPadding

        val barWidth = 19.dp.toPx()

        val spacing =
            (chartWidth - barWidth * data.size) /
                    (data.size - 1)

        // Y AXIS PAINT

        val yPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = android.graphics.Color.rgb(
                132, 147, 159
            )
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        // X AXIS PAINT

        val xPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = android.graphics.Color.rgb(
                132, 147, 159
            )
            textSize = 11.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // GRID LINES

        for (i in 0..4) {

            val ratio = i / 4f

            val y = topPadding + chartHeight * ratio

            drawLine(
                color = AdminBorder,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx()
            )

            val label =
                (maxValue * (1f - ratio)).toInt().toString()

            drawContext.canvas.nativeCanvas.drawText(
                label,
                leftPadding - 7.dp.toPx(),
                y + 3.dp.toPx(),
                yPaint
            )
        }

        // BARS

        data.forEachIndexed { index, value ->

            val barHeight =
                (value / maxValue) * chartHeight

            val x =
                leftPadding + index * (barWidth + spacing)

            val y =
                topPadding + chartHeight - barHeight

            // Bar background

            drawRoundRect(
                color = AdminGreenLight.copy(alpha = 0.5f),
                topLeft = Offset(
                    x,
                    topPadding
                ),
                size = Size(
                    barWidth,
                    chartHeight
                ),
                cornerRadius = CornerRadius(
                    6.dp.toPx(),
                    6.dp.toPx()
                )
            )

            // Main bar

            drawRoundRect(
                color = if (index == 5)
                    AdminGreenDark
                else AdminGreen,
                topLeft = Offset(x, y),
                size = Size(
                    barWidth,
                    barHeight
                ),
                cornerRadius = CornerRadius(
                    6.dp.toPx(),
                    6.dp.toPx()
                )
            )

            // X LABEL

            drawContext.canvas.nativeCanvas.drawText(
                labels[index],
                x + barWidth / 2,
                size.height - 4.dp.toPx(),
                xPaint
            )
        }
    }
}
