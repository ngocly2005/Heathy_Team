package com.app_health.app.feature.integrated

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

data class MonthlyWeightPoint(val date: LocalDate, val entry: WeightDto)
data class WeightChartRange(val minKg: Double, val maxKg: Double)

fun monthlyWeightPoints(entries: List<WeightDto>, month: YearMonth): List<MonthlyWeightPoint> =
    entries.mapNotNull { entry ->
        val date = runCatching { LocalDate.parse(entry.measured_on) }.getOrNull()
        if (date != null && YearMonth.from(date) == month && entry.weight_kg.isFinite())
            MonthlyWeightPoint(date, entry) else null
    }.sortedBy { it.date }

fun weightDayFraction(date: LocalDate, month: YearMonth): Float =
    ((date.dayOfMonth - 1).toFloat() / (month.lengthOfMonth() - 1)).coerceIn(0f, 1f)

fun weightChartRange(points: List<MonthlyWeightPoint>): WeightChartRange {
    if (points.isEmpty()) return WeightChartRange(0.0, 1.0)
    val low = points.minOf { it.entry.weight_kg }
    val high = points.maxOf { it.entry.weight_kg }
    val margin = maxOf(0.5, (high - low) * 0.15)
    return WeightChartRange(low - margin, high + margin)
}

/** Editing an existing measurement must not round its stored hundredths to tenths. */
fun weightInputText(value: Double): String = BigDecimal.valueOf(value)
    .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

fun weightLabel(value: Double): String = weightInputText(value).replace('.', ',')

fun weightInputError(value: String): String? {
    val text = value.trim()
    if (text.isEmpty()) return "Nhập cân nặng để lưu số đo."
    if (!Regex("^[0-9]+([.,][0-9]{0,2})?$").matches(text))
        return "Nhập số hợp lệ, tối đa 2 chữ số sau dấu phẩy."
    val amount = text.replace(',', '.').toDoubleOrNull()
    if (amount == null || !amount.isFinite() || amount <= 20 || amount > 400)
        return "Cân nặng phải lớn hơn 20 kg và không quá 400 kg."
    return null
}

fun weightInputAmount(value: String): Double? = if (weightInputError(value) == null)
    value.trim().replace(',', '.').toDouble() else null

fun allowedWeightDate(date: LocalDate, today: LocalDate): Boolean = date <= today

// Material DatePicker represents a calendar date as midnight UTC, independent of device timezone.
fun weightPickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun weightPickerDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
