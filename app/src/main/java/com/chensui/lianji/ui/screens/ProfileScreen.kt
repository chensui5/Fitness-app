package com.chensui.lianji.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chensui.lianji.BuildConfig
import com.chensui.lianji.data.BodyRecord
import com.chensui.lianji.data.Dates
import com.chensui.lianji.data.Store
import com.chensui.lianji.data.UserProfile
import com.chensui.lianji.ui.components.CoinBadge
import com.chensui.lianji.ui.components.EmptyHint
import com.chensui.lianji.ui.components.LocalImage
import com.chensui.lianji.ui.components.SectionCard
import com.chensui.lianji.ui.components.SectionTitle
import com.chensui.lianji.ui.components.SimpleProgress
import com.chensui.lianji.ui.components.StatCell
import com.chensui.lianji.ui.components.StatusChip
import com.chensui.lianji.ui.components.ThinDivider
import com.chensui.lianji.ui.theme.CoinGold
import com.chensui.lianji.ui.theme.DoneGreen
import com.chensui.lianji.ui.theme.RestGray
import com.chensui.lianji.ui.theme.WarnOrange
import com.chensui.lianji.ui.theme.doneColor
import java.time.YearMonth

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val data by Store.data.collectAsStateWithLifecycle()

    var profileEditor by remember { mutableStateOf(false) }
    var bodyEditor by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var calendarOpen by remember { mutableStateOf(false) }
    var coinLogOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val now = Dates.now()
    val thisMonth = YearMonth.from(now)
    val (doneCount, targetCount) = Store.monthStats(thisMonth)
    val streak = Store.streakDays()
    val total = Store.totalCheckIns()
    val latestBody = Store.latestBodyRecord()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(Store.exportJson().toByteArray(Charsets.UTF_8))
                }
                message = "数据已导出"
            }.onFailure { message = "导出失败：${it.message}" }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        /* ---------- 个人资料头（只展示，修改入口在设置里） ---------- */
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LocalImage(
                        uri = data.profile.avatarUri,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        fallback = {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = data.profile.nickname.take(1).ifBlank { "练" },
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = data.profile.nickname,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = data.profile.motto.ifBlank { "还没写激励文案" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    CoinBadge(coins = data.coins)
                }
            }
        }

        /* ---------- 本月打卡 ---------- */
        item {
            val percent = if (targetCount > 0) doneCount.toFloat() / targetCount else 0f
            SectionCard(onClick = { calendarOpen = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "本月打卡",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${thisMonth.monthValue} 月 · 查看日历 ›",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (targetCount == 0) {
                    Text(
                        text = "本月还没有制定过计划，去「计划」页安排一下。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$doneCount",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / $targetCount 天",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${(percent * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    SimpleProgress(progress = percent, height = 8.dp)

                    Spacer(Modifier.height(14.dp))
                    ThinDivider()
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniStat("连续打卡", "$streak 天", Modifier.weight(1f))
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )
                        MiniStat("累计打卡", "$total 次", Modifier.weight(1f))
                    }
                }
            }
        }

        /* ---------- 身体数据 ---------- */
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("身体数据", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    val editable = Store.canEditMonth(thisMonth.toString())
                    TextButton(onClick = { bodyEditor = true }) {
                        Text(
                            text = if (editable) "修改" else "未到修改期",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (editable) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (Store.isInReminderWindow()) {
                    Spacer(Modifier.height(6.dp))
                    InfoBanner("月底啦，记得更新这个月的身体数据", WarnOrange)
                }

                Spacer(Modifier.height(10.dp))

                if (latestBody == null) {
                    EmptyHint("还没有记录身高体重")
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCell(
                            value = fmt1(latestBody.heightCm),
                            label = "身高 cm",
                            modifier = Modifier.weight(1f)
                        )
                        StatCell(
                            value = fmt1(latestBody.weightKg),
                            label = "体重 kg",
                            modifier = Modifier.weight(1f)
                        )
                        StatCell(
                            value = fmt1(bmi(latestBody.heightCm, latestBody.weightKg)),
                            label = "BMI",
                            modifier = Modifier.weight(1f),
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "锻炼建议",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                advice(latestBody.heightCm, latestBody.weightKg),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "最近记录：${latestBody.month}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { historyOpen = true }) {
                    Text("查看历史记录", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        /* ---------- 显示模式 ---------- */
        item {
            SectionCard {
                SectionTitle(text = "显示模式")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to "跟随系统",
                        1 to "浅色",
                        2 to "深色"
                    ).forEach { (mode, label) ->
                        val selected = data.themeMode == mode
                        Surface(
                            modifier = Modifier.weight(1f).clickable { Store.setThemeMode(mode) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        /* ---------- 反馈 ---------- */
        item {
            SectionCard {
                SectionTitle(text = "反馈")
                Spacer(Modifier.height(10.dp))
                SwitchRow(
                    label = "操作音效",
                    hint = "勾选动作、完成打卡时播放轻提示音",
                    checked = data.soundEnabled,
                    onCheckedChange = { Store.setSoundEnabled(it) }
                )
                ThinDivider()
                SwitchRow(
                    label = "触感反馈",
                    hint = "操作时给予轻微振动",
                    checked = data.hapticEnabled,
                    onCheckedChange = { Store.setHapticEnabled(it) }
                )
            }
        }

        /* ---------- 设置 ---------- */
        item {
            SectionCard {
                SectionTitle(text = "设置")
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { profileEditor = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("编辑个人资料", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "头像、昵称、激励文案",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "修改",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                ThinDivider()
                SettingRow("版本号", "v${BuildConfig.VERSION_NAME}")

                ThinDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        exportLauncher.launch("lianji-backup-${Dates.todayKey()}.json")
                    }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("导出数据", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "把全部记录导出为 JSON 文件备份",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("导出", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }

                ThinDivider()
                SettingRow("作者签名", "尘遂")

                ThinDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { coinLogOpen = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("金币流水", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "每一笔获得、兑换与回收记录",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${data.coins}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (data.coins < 0) WarnOrange else CoinGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "查看",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        item {
            Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "练迹 · 记录每一次训练",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    /* ---------- 弹窗 ---------- */
    if (profileEditor) {
        ProfileEditorDialog(
            initial = data.profile,
            onDismiss = { profileEditor = false },
            onSave = { updated ->
                Store.updateProfile(updated)
                profileEditor = false
            }
        )
    }

    if (bodyEditor) {
        BodyEditorDialog(
            month = thisMonth.toString(),
            initial = Store.bodyRecordOf(thisMonth.toString()) ?: latestBody,
            onDismiss = { bodyEditor = false },
            onSave = { h, w ->
                Store.saveBodyRecord(
                    BodyRecord(month = thisMonth.toString(), heightCm = h, weightKg = w)
                )
                bodyEditor = false
            }
        )
    }

    if (historyOpen) {
        HistoryDialog(records = data.bodyRecords.sortedByDescending { it.month }, onDismiss = { historyOpen = false })
    }

    if (calendarOpen) {
        CalendarDialog(onDismiss = { calendarOpen = false })
    }

    if (coinLogOpen) {
        CoinLogDialog(onDismiss = { coinLogOpen = false })
    }

    message?.let { msg ->
        ConfirmDialog(
            title = "提示",
            message = msg,
            confirmText = "好",
            onDismiss = { message = null },
            onConfirm = { message = null }
        )
    }
}

/* ==================== 小工具 ==================== */

private fun fmt1(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

private fun bmi(h: Double, w: Double): Double =
    if (h <= 0) 0.0 else w / ((h / 100.0) * (h / 100.0))

private fun advice(h: Double, w: Double): String {
    val b = bmi(h, w)
    if (b <= 0) return "填写身高体重后自动生成建议"
    return when {
        b < 18.5 -> "体重偏轻（BMI ${fmt1(b)}）。建议增加优质蛋白与热量摄入，训练以深蹲、硬拉、卧推等复合动作为主，循序渐进增重。"
        b < 24.0 -> "体重处于健康区间（BMI ${fmt1(b)}）。保持规律训练与均衡饮食，注意渐进超负荷，每 2-4 周小幅提升重量或次数。"
        b < 28.0 -> "体重略高（BMI ${fmt1(b)}）。力量训练后加 20-30 分钟有氧，减少精制碳水与含糖饮料，优先保证蛋白质摄入。"
        else -> "体重偏高（BMI ${fmt1(b)}）。以饮食控制为主、训练为辅，优先低冲击有氧（快走、游泳、骑行），配合基础力量训练保护关节。"
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ==================== 身体数据编辑 ==================== */

@Composable
private fun BodyEditorDialog(
    month: String,
    initial: BodyRecord?,
    onDismiss: () -> Unit,
    onSave: (Double, Double) -> Unit
) {
    var h by remember { mutableStateOf(initial?.heightCm?.let { fmt1(it) } ?: "") }
    var w by remember { mutableStateOf(initial?.weightKg?.let { fmt1(it) } ?: "") }

    val hv = h.toDoubleOrNull()
    val wv = w.toDoubleOrNull()
    val valid = hv != null && hv > 50 && wv != null && wv > 10

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("$month 身体数据", style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(10.dp))
                InfoBanner("仅在每月最后三天与次月可修改；历史记录只能查看，不能改动", WarnOrange)

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = h,
                        onValueChange = { h = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("身高 cm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = w,
                        onValueChange = { w = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("体重 kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                if (valid) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "BMI ${fmt1(bmi(hv!!, wv!!))}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                advice(hv, wv),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(enabled = valid, onClick = { if (valid) onSave(hv!!, wv!!) }) {
                        Text(
                            "保存",
                            color = if (valid) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/* ==================== 历史记录（只读） ==================== */

@Composable
private fun HistoryDialog(records: List<BodyRecord>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("身体数据记录", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "历史记录仅供查看，无法修改",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (records.isEmpty()) {
                    EmptyHint("还没有任何记录")
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        records.forEachIndexed { i, r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    r.month,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    "${fmt1(r.heightCm)} cm",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.7f)
                                )
                                Text(
                                    "${fmt1(r.weightKg)} kg",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.7f)
                                )
                                Text(
                                    "BMI ${fmt1(bmi(r.heightCm, r.weightKg))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(0.9f),
                                    textAlign = TextAlign.End
                                )
                            }
                            if (i < records.size - 1) ThinDivider()
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/* ==================== 日历 ==================== */

@Composable
private fun CalendarDialog(onDismiss: () -> Unit) {
    val data by Store.data.collectAsStateWithLifecycle()
    var cursor by remember { mutableStateOf(YearMonth.from(Dates.now())) }

    val (done, target) = Store.monthStats(cursor)
    val today = Dates.now()
    val isCurrentMonth = cursor == YearMonth.from(today)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("打卡日历", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                /* 月份切换 */
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { cursor = cursor.minusMonths(1) }) {
                        Text("‹ 上月", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${cursor.year} 年 ${cursor.monthValue} 月",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        enabled = cursor < YearMonth.from(today),
                        onClick = { cursor = cursor.plusMonths(1) }
                    ) {
                        Text(
                            "下月 ›",
                            color = if (cursor < YearMonth.from(today)) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                /* 表头 */
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                        Text(
                            text = d,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                /* 日期网格 */
                val firstDay = cursor.atDay(1)
                val lead = firstDay.dayOfWeek.value - 1
                val daysInMonth = cursor.lengthOfMonth()
                val totalCells = lead + daysInMonth
                val rows = (totalCells + 6) / 7

                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val dayNum = r * 7 + c - lead + 1
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNum in 1..daysInMonth) {
                                    val date = cursor.atDay(dayNum)
                                    val key = Dates.key(date)
                                    val rec = data.records[key]
                                    // 按「该日期所属周」查计划：没有制定过计划的周不会显示打卡目标
                                    val dayPlan = data.plans[Dates.weekKeyOf(date)]
                                        ?.dayOf(Dates.dow(date))
                                    val hasTarget = dayPlan != null &&
                                            !dayPlan.isRestDay && dayPlan.exercises.isNotEmpty()
                                    val isRest = dayPlan?.isRestDay == true
                                    val checked = rec?.workoutChecked == true
                                    val accent = doneColor()

                                    val border = when {
                                        checked -> accent
                                        hasTarget -> MaterialTheme.colorScheme.outline
                                        else -> Color.Transparent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (checked) accent else Color.Transparent)
                                            .then(
                                                if (border != Color.Transparent)
                                                    Modifier.androidBorder(border)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when {
                                                checked -> Color.White
                                                date == today -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (checked || date == today) FontWeight.Bold
                                            else FontWeight.Normal
                                        )
                                    }

                                    if (isRest) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 2.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(RestGray)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ThinDivider()
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatCell(
                        value = "$done/$target",
                        label = if (isCurrentMonth) "本月打卡" else "该月打卡",
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        LegendRow(DoneGreen, "已完成打卡")
                        Spacer(Modifier.height(5.dp))
                        LegendRow(RestGray, "休息日（无目标）")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 圆形描边 */
private fun Modifier.androidBorder(color: Color) =
    this.border(width = 1.2.dp, color = color, shape = CircleShape)

/* ==================== 个人资料编辑（头像 / 昵称 / 激励文案） ==================== */

@Composable
private fun ProfileEditorDialog(
    initial: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    var nickname by remember { mutableStateOf(initial.nickname) }
    var motto by remember { mutableStateOf(initial.motto) }
    var avatarUri by remember { mutableStateOf(initial.avatarUri) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            avatarUri = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier
                    .padding(18.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("编辑个人资料", style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(16.dp))

                /* 头像 */
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { pickImage.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        LocalImage(
                            uri = avatarUri,
                            modifier = Modifier.size(72.dp).clip(CircleShape),
                            fallback = {
                                Surface(
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "＋",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = RestGray
                                        )
                                    }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("头像", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "点击左侧图片从相册选择",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (avatarUri != null) {
                            TextButton(onClick = { avatarUri = null }) {
                                Text(
                                    "移除头像",
                                    color = WarnOrange,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* 昵称：单行走 singleLine，不能同时传 minLines/maxLines */
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { if (it.length <= 20) nickname = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("昵称") }
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${nickname.length}/20",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (nickname.length >= 20) WarnOrange
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                /* 激励文案：多行，minLines 必须不大于 maxLines */
                OutlinedTextField(
                    value = motto,
                    onValueChange = { if (it.length <= 25) motto = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 2,
                    label = { Text("激励文案") }
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "最多 25 字",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${motto.length}/25",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (motto.length >= 25) WarnOrange
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = {
                            onSave(
                                initial.copy(
                                    nickname = nickname.trim().ifBlank { "健身者" },
                                    motto = motto.trim(),
                                    avatarUri = avatarUri
                                )
                            )
                        }
                    ) {
                        Text(
                            "保存",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
