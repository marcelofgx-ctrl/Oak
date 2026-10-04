package com.oakandember.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CalendarInk = Color(0xFF2C2621)
private val CalendarOak = Color(0xFF7B5A3C)
private val CalendarEmber = Color(0xFFB14C24)
private val CalendarMoss = Color(0xFF4F6A55)
private val CalendarSoftGray = Color(0xFFF1EEE9)
private val CalendarDanger = Color(0xFF9E3D32)

fun calendarToday(): LocalDate = LocalDate.now(ZoneId.of("America/New_York"))

fun parseScheduledDate(value: String): LocalDate? = try {
    LocalDate.parse(value)
} catch (_: Exception) {
    null
}

fun calendarDayLabel(value: String): String {
    val date = parseScheduledDate(value) ?: return value.ifBlank { "Unscheduled" }
    val today = calendarToday()
    val pretty = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)).uppercase(Locale.US)
    return when {
        date == today -> "TODAY · $pretty"
        date == today.plusDays(1) -> "TOMORROW · $pretty"
        date.isBefore(today) -> "OVERDUE · $pretty"
        else -> pretty
    }
}

fun isOverdue(request: OakRequest): Boolean {
    if (request.status == "Cerrada") return false
    val date = parseScheduledDate(request.scheduledDate()) ?: return false
    return date.isBefore(calendarToday())
}

fun isToday(request: OakRequest): Boolean {
    val date = parseScheduledDate(request.scheduledDate()) ?: return false
    return date == calendarToday()
}

@Composable
fun CollapsibleDayHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    overdue: Boolean = false,
    today: Boolean = false,
    onToggle: () -> Unit
) {
    val accent = when {
        overdue -> CalendarDanger
        today -> CalendarMoss
        else -> CalendarOak
    }
    val background = when {
        overdue -> Color(0xFFFFECE8)
        today -> Color(0xFFEAF4EC)
        else -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = .5.sp
                )
                Text(
                    "$count job${if (count == 1) "" else "s"}",
                    color = CalendarInk.copy(alpha = .55f),
                    fontSize = 11.sp
                )
            }
            Text(
                if (expanded) "▾" else "▸",
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}
