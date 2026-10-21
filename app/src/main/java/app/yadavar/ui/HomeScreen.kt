package app.yadavar.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.yadavar.time.Fmt

@Composable
private fun MicGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.07f
        val bodyW = w * 0.34f
        drawRoundRect(
            color = color, topLeft = Offset((w - bodyW) / 2, h * 0.08f),
            size = Size(bodyW, h * 0.5f), cornerRadius = CornerRadius(bodyW / 2),
        )
        drawArc(
            color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.28f), size = Size(w * 0.52f, h * 0.42f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(color, Offset(w / 2, h * 0.70f), Offset(w / 2, h * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.36f, h * 0.88f), Offset(w * 0.64f, h * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun PulseRing() {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale",
    )
    Box(
        Modifier.size(190.dp).scale(s).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    )
}

@Composable
fun HomeScreen(vm: MainViewModel) {
    val voice by vm.voice.collectAsState()
    val notes by vm.notes.collectAsState()
    val settings by vm.settings.collectAsState()
    val ctx = LocalContext.current

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startListening() else vm.onMicPermissionDenied()
    }

    fun onMicClick() {
        when (voice.phase) {
            VoicePhase.LISTENING -> vm.stopListening()
            VoicePhase.PROCESSING -> Unit
            else -> {
                val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (ok) vm.startListening() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val now = System.currentTimeMillis()
    val upcoming = notes.filter { val r = it.remindAt; r != null && !it.done && r > now }
        .sortedBy { it.remindAt }.take(3)
    val recent = notes.take(5)
    val listening = voice.phase == VoicePhase.LISTENING
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "یادآور من", Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = { vm.go(Screen.SETTINGS) }) {
                    Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                    if (listening) PulseRing()
                    Box(
                        Modifier.size(130.dp).clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(primary, MaterialTheme.colorScheme.secondary)))
                            .clickable { onMicClick() }
                            .semantics { contentDescription = if (listening) "توقف شنیدن" else "شروع ضبط صدا" },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (listening) {
                            Box(Modifier.size(34.dp).background(onPrimary, RoundedCornerShape(8.dp)))
                        } else {
                            MicGlyph(onPrimary, Modifier.size(60.dp))
                        }
                    }
                }
                val label = when (voice.phase) {
                    VoicePhase.READY -> "آماده شنیدن؛ دکمه را لمس کنید"
                    VoicePhase.LISTENING -> "در حال گوش دادن… " + Fmt.timer(voice.seconds)
                    VoicePhase.PROCESSING -> "در حال پردازش…"
                    VoicePhase.SAVED -> "ثبت شد ✓"
                    VoicePhase.ERROR -> voice.error ?: "خطا"
                }
                Text(
                    label, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (voice.phase == VoicePhase.ERROR) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (listening && voice.partial.isNotBlank()) {
                    Text(
                        voice.partial, Modifier.padding(top = 8.dp), textAlign = TextAlign.Center,
                        maxLines = 3, color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (listening) TextButton(onClick = { vm.cancelListening() }) { Text("لغو") }
                if (voice.phase == VoicePhase.ERROR && voice.needsAppSettings) {
                    TextButton(onClick = { PermissionHelper.openAppSettings(ctx) }) { Text("باز کردن تنظیمات برنامه") }
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            item { SectionTitle("یادآوری‌های نزدیک") }
            items(upcoming, key = { it.id }) { n ->
                NoteCard(n, settings.use24h, onClick = { vm.editNote(n) })
            }
        }
        item { SectionTitle("یادداشت‌های اخیر") }
        if (recent.isEmpty()) {
            item {
                Text(
                    "هنوز یادداشتی ندارید. میکروفون را لمس کنید و مثلاً بگویید «فردا ساعت ۹ صبح یادآوری کن جلسه دارم».",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(recent, key = { "n${it.id}" }) { n ->
                NoteCard(n, settings.use24h, onClick = { vm.editNote(n) })
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { vm.go(Screen.NOTES) }, modifier = Modifier.weight(1f)) { Text("همه یادداشت‌ها") }
                OutlinedButton(onClick = { vm.newTextNote() }, modifier = Modifier.weight(1f)) { Text("یادداشت متنی") }
            }
        }
    }
}
