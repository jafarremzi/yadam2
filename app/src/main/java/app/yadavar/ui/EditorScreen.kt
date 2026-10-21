package app.yadavar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import app.yadavar.data.Repeat
import app.yadavar.time.Fmt
import app.yadavar.time.ParseStatus
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: MainViewModel) {
    val s by vm.editor.collectAsState()
    val settings by vm.settings.collectAsState()
    val ctx = LocalContext.current
    var askedNotif by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> vm.save() }

    fun trySave() {
        val needAsk = s.reminderEnabled && Build.VERSION.SDK_INT >= 33 && !askedNotif &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needAsk) {
            askedNotif = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.save()
        }
    }

    val dt = s.dateTime
    // ساعت زنده: اگر ویرایشگر باز بماند و زمان از دست برود، همان لحظه «گذشته» نشان داده می‌شود
    var nowTick by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            nowTick = LocalDateTime.now()
        }
    }
    // زمان نسبی («یک دقیقه دیگر») بدون دست‌کاری: هنگام تأیید دوباره از همان لحظه حساب می‌شود
    val relativeStale = s.relativeMinutes != null && !s.timeTouched && dt != null && !dt.isAfter(nowTick)
    val pastNow = dt != null && !dt.isAfter(nowTick) && !relativeStale
    val blocked = s.text.isBlank() || (s.reminderEnabled && (dt == null || pastNow ||
        (s.status == ParseStatus.IN_PAST && !relativeStale) || s.status == ParseStatus.INVALID))

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                if (s.existing != null) "ویرایش" else if (s.fromVoice) "تأیید گفتار" else "یادداشت جدید",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        OutlinedTextField(
            value = s.text, onValueChange = vm::onTextChange, modifier = Modifier.fillMaxWidth(),
            label = { Text(if (s.fromVoice) "متن تشخیص‌داده‌شده (قابل ویرایش)" else "متن") }, minLines = 3,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("یادآوری زمان‌دار", Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium)
            Switch(checked = s.reminderEnabled, onCheckedChange = vm::setReminderEnabled)
        }

        if (s.reminderEnabled && dt != null) {
            OutlinedTextField(
                value = s.title, onValueChange = vm::onTitleChange, modifier = Modifier.fillMaxWidth(),
                label = { Text("عنوان کار") }, singleLine = true,
            )

            s.message?.let { msg ->
                val bad = s.status == ParseStatus.IN_PAST || s.status == ParseStatus.INVALID
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (bad) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Text(msg, Modifier.padding(12.dp),
                        color = if (bad) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            if (relativeStale) {
                Text("با تأیید، مدت گفته‌شده دوباره از همین لحظه حساب می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if ((s.status == ParseStatus.IN_PAST || pastNow) && s.message == null) {
                Text("این زمان گذشته است؛ زمان را تغییر دهید.", color = MaterialTheme.colorScheme.error)
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Fmt.date(dt.toLocalDate()), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(Fmt.time(dt, settings.use24h), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(Fmt.remaining(nowTick, dt), color = MaterialTheme.colorScheme.onSurfaceVariant)

                    s.alternative?.let { alt ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = true, onClick = {}, label = { Text(Fmt.time(dt, settings.use24h)) })
                            FilterChip(selected = false, onClick = { vm.useAlternative() }, label = { Text(Fmt.time(alt, settings.use24h)) })
                        }
                    }
                    StepperRow("روز", { vm.adjust(ChronoUnit.DAYS, -1) }, { vm.adjust(ChronoUnit.DAYS, 1) })
                    StepperRow("ساعت", { vm.adjust(ChronoUnit.HOURS, -1) }, { vm.adjust(ChronoUnit.HOURS, 1) })
                    StepperRow("دقیقه (۵ تا ۵ تا)", { vm.adjust(ChronoUnit.MINUTES, -5) }, { vm.adjust(ChronoUnit.MINUTES, 5) })
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AssistChip(onClick = { vm.setQuick(3) }, label = { Text("۱ دقیقه دیگر", maxLines = 1) })
                        AssistChip(onClick = { vm.setQuick(0) }, label = { Text("۱۰ دقیقه دیگر", maxLines = 1) })
                        AssistChip(onClick = { vm.setQuick(1) }, label = { Text("۱ ساعت دیگر", maxLines = 1) })
                        AssistChip(onClick = { vm.setQuick(2) }, label = { Text("فردا ۹ صبح", maxLines = 1) })
                    }
                }
            }

            Text("تکرار", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in listOf(Repeat.NONE, Repeat.DAILY, Repeat.WEEKLY)) {
                    FilterChip(selected = s.repeat == r, onClick = { vm.setRepeat(r) }, label = { Text(repeatLabel(r)) })
                }
            }
        }

        Button(onClick = { trySave() }, enabled = !blocked, modifier = Modifier.fillMaxWidth()) {
            Text(if (s.reminderEnabled) "تأیید و برنامه‌ریزی" else "ذخیره یادداشت", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(onClick = { vm.back() }, modifier = Modifier.fillMaxWidth()) { Text("انصراف", maxLines = 1) }
    }
}
