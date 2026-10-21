package app.yadavar.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.yadavar.data.NoteEntity
import app.yadavar.data.Repeat
import app.yadavar.time.Fmt
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun SectionTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp),
    )
}

fun repeatLabel(r: Int) = when (r) {
    Repeat.DAILY -> "روزانه"
    Repeat.WEEKLY -> "هفتگی"
    else -> "بدون تکرار"
}

@Composable
fun NoteCard(
    note: NoteEntity,
    use24h: Boolean,
    onClick: () -> Unit,
    onToggleDone: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val whenText = remember(note.remindAt, use24h, note.repeat) {
        note.remindAt?.let {
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            Fmt.dateTime(dt, use24h) + if (note.repeat != Repeat.NONE) " · " + repeatLabel(note.repeat) else ""
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onToggleDone != null) {
                Checkbox(checked = note.done, onCheckedChange = { onToggleDone() })
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    note.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (note.done) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (whenText != null) {
                    Text(
                        "یادآوری: $whenText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                val end = note.remindAt
                if (end != null && !note.done) ReminderProgress(note, end)
            }
            if (onDelete != null) {
                IconButton(onClick = onClick) { Icon(Icons.Default.Edit, contentDescription = "ویرایش") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
            }
        }
    }
}

@Composable
fun StepperRow(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedButton(onClick = onMinus) { Text("−") }
            androidx.compose.material3.OutlinedButton(onClick = onPlus) { Text("+") }
        }
    }
}

/** نوار پیشرفت یک یادآوری: از زمان ساخت (یا نوبت قبلی در تکرارشونده‌ها) تا لحظه‌ی یادآوری. */
@Composable
fun ReminderProgress(note: NoteEntity, end: Long) {
    val start = when (note.repeat) {
        Repeat.DAILY -> maxOf(note.createdAt, end - 86_400_000L)
        Repeat.WEEKLY -> maxOf(note.createdAt, end - 7 * 86_400_000L)
        else -> note.createdAt
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= end) break
            delay(if (end - now <= 3_600_000L) 1_000L else 15_000L)
        }
    }
    val span = (end - start).coerceAtLeast(1L)
    val target = ((now - start).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    val shown by animateFloatAsState(target, tween(500), label = "reminderProgress")
    val left = end - now
    val label = when {
        left <= 0L -> "زمان یادآوری رسید"
        left < 60_000L -> Fmt.digits("${left / 1000 + 1} ثانیه دیگر")
        else -> Fmt.remaining(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()),
        )
    }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { shown },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
