package com.chensui.lianji.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chensui.lianji.data.Dates
import com.chensui.lianji.data.OtherPlan
import com.chensui.lianji.data.OtherType
import com.chensui.lianji.data.PlanTextParser
import com.chensui.lianji.data.Store
import com.chensui.lianji.data.setsRepsLabel
import com.chensui.lianji.ui.components.SectionCard
import com.chensui.lianji.ui.components.ThinDivider
import com.chensui.lianji.ui.components.sanitizePositiveIntInput
import com.chensui.lianji.ui.theme.WarnOrange
import java.time.LocalDate
import java.util.UUID

/* ==================== 第一步：粘贴文本 ==================== */

@Composable
internal fun PlanPasteDialog(
    onDismiss: () -> Unit,
    onParsed: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("从文本导入计划", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "把训练计划整段粘进来，自动识别出每天的动作与组次。识别结果会先给你预览确认。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 260.dp),
                    label = { Text("在这里粘贴计划文本") },
                    placeholder = { Text("## 周一：胸+三头\n1. 杠铃卧推 4×8\n2. 上斜器械推胸 4×10") }
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    "支持「## 周一」「周一到周日」「休息安排」等写法；认不出的行会列出来，不会瞎猜。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        enabled = text.isNotBlank(),
                        onClick = { onParsed(text) }
                    ) {
                        Text(
                            "识别并预览",
                            color = if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/* ==================== 第二步：预览与勾选 ==================== */

/** 最多往后预览 8 周（≈56 天，控制在 60 天内） */
private const val MAX_WEEK_OFFSET = 8

@Composable
internal fun PlanImportPreviewDialog(
    sourceText: String,
    onDismiss: () -> Unit
) {
    val data by Store.data.collectAsStateWithLifecycle()

    val result = remember(sourceText) { PlanTextParser.parse(sourceText) }
    val days = remember(sourceText) {
        mutableStateListOf<PlanTextParser.ParsedDay>().also { it.addAll(result.days) }
    }
    val checked = remember(sourceText) {
        mutableStateListOf<Boolean>().also { list -> repeat(result.days.size) { list.add(true) } }
    }

    var weekOffset by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf<Int?>(null) }
    var editingExercise by remember { mutableStateOf<Pair<Int, Int>?>(null) }   // (dayIdx, exIdx)
    var addingTo by remember { mutableStateOf<Int?>(null) }
    var editingOther by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<String?>(null) }

    val monday = Dates.mondayOf(Dates.now()).plusWeeks(weekOffset.toLong())
    val targetWeek = Dates.key(monday)

    // 该周已被锁定（早于昨天）的日期不能写入
    fun dateOf(dow: Int): LocalDate = monday.plusDays((dow - 1).toLong())
    fun locked(dow: Int): Boolean = !Store.canEditPlanOn(dateOf(dow))

    val selected = days.filterIndexed { i, d -> checked[i] && !d.isEmpty() && !locked(d.dayOfWeek) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).padding(vertical = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp).heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
                Text("导入预览", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "识别出 ${days.size} 天。点动作可修改，取消勾选即可跳过某天。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                /* ---------- 目标周 ---------- */
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        enabled = weekOffset > 0,
                        onClick = { weekOffset-- }
                    ) {
                        Text(
                            "‹",
                            color = if (weekOffset > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${monday.monthValue}月${monday.dayOfMonth}日 – " +
                                "${monday.plusDays(6).monthValue}月${monday.plusDays(6).dayOfMonth}日",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            when (weekOffset) {
                                0 -> "本周"
                                1 -> "下周"
                                else -> "第 ${weekOffset + 1} 周"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        enabled = weekOffset < MAX_WEEK_OFFSET,
                        onClick = { weekOffset++ }
                    ) {
                        Text(
                            "›",
                            color = if (weekOffset < MAX_WEEK_OFFSET) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (result.hints.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    result.hints.forEach {
                        InfoBanner(it, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                    }
                }

                /* ---------- 每天 ---------- */
                Spacer(Modifier.height(6.dp))
                days.forEachIndexed { i, day ->
                    val isLocked = locked(day.dayOfWeek)
                    val isOpen = expanded == i
                    SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                        // 整行都可点。原来 clickable 只挂在左侧文字区，用户去点右侧的
                        //「查看」会毫无反应 —— 看起来就像按钮坏了。
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = if (isOpen) null else i },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked[i] && !isLocked,
                                enabled = !isLocked,
                                onCheckedChange = { checked[i] = it }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        Dates.weekLabel(day.dayOfWeek),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isLocked) "已过 · 不可写入"
                                        else if (day.isRest) "休息"
                                        else "${day.exercises.size} 个动作" +
                                            if (day.others.isNotEmpty()) " + ${day.others.size} 项训练" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLocked) WarnOrange
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (day.title.isNotBlank()) {
                                    Text(
                                        day.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // 用 TextButton 而不是纯 Text，让它看起来就是个能按的按钮
                            TextButton(onClick = { expanded = if (isOpen) null else i }) {
                                Text(
                                    if (isOpen) "收起" else "查看",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isOpen) {
                            Spacer(Modifier.height(8.dp))
                            ThinDivider()
                            Spacer(Modifier.height(6.dp))

                            day.exercises.forEachIndexed { j, ex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingExercise = i to j }
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(ex.name, style = MaterialTheme.typography.bodyMedium)
                                            if (ex.reps.isBlank()) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "⚠ 数量待填",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = WarnOrange
                                                )
                                            }
                                        }
                                        if (ex.note.isNotBlank()) {
                                            Text(
                                                ex.note,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        ex.setsRepsLabel(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "修改 ›",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    TextButton(onClick = {
                                        // 按 id 删；在遍历里按下标删会让后面的元素错位
                                        day.exercises.removeAll { it.id == ex.id }
                                    }) {
                                        Text("移除", color = WarnOrange, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            day.others.forEachIndexed { j, other ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingOther = i to j }
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            other.typeName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            if (other.fromRange) "⚠ 原文是区间，已取下限"
                                            else "识别为其他训练",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (other.fromRange) WarnOrange
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        fmtAmount(other.amount) + " " + other.unit,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "修改 ›",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    TextButton(onClick = { day.others.removeAt(j) }) {
                                        Text("移除", color = WarnOrange, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            if (day.exercises.isEmpty() && day.others.isEmpty() && !day.isRest) {
                                Text(
                                    "这天没有识别到内容",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            Row {
                                TextButton(onClick = { addingTo = i }) {
                                    Text("+ 添加动作", color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { day.isRest = !day.isRest }) {
                                    Text(
                                        if (day.isRest) "取消休息日" else "设为休息日",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                /* ---------- 跳过的行 ---------- */
                if (result.skipped.isNotEmpty()) {
                    SectionCard {
                        Text(
                            "已跳过 ${result.skipped.size} 行（不猜，交给你判断）",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarnOrange
                        )
                        Spacer(Modifier.height(6.dp))
                        result.skipped.take(12).forEach { s ->
                            Text(
                                "· ${s.text.take(40)}　（${s.reason}）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(3.dp))
                        }
                        if (result.skipped.size > 12) {
                            Text(
                                "… 还有 ${result.skipped.size - 12} 行",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                /* ---------- 底部操作 ---------- */
                Spacer(Modifier.height(12.dp))
                Text(
                    "将导入 ${selected.size} 天到 " +
                        "${monday.monthValue}/${monday.dayOfMonth} 那一周" +
                        if (selected.size < days.count { !it.isEmpty() })
                            "（有 ${days.count { !it.isEmpty() } - selected.size} 天被取消或已过期）" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        enabled = selected.isNotEmpty(),
                        onClick = {
                            if (Store.data.value.plans[targetWeek] != null) confirmReplace = true
                            else {
                                doImport(targetWeek, selected, data.otherTypes)
                                done = "已导入 ${selected.size} 天"
                            }
                        }
                    ) {
                        Text(
                            "确认导入",
                            color = if (selected.isNotEmpty()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    /* ---------- 编辑动作 ---------- */
    editingExercise?.let { (i, j) ->
        val ex = days.getOrNull(i)?.exercises?.getOrNull(j)
        if (ex != null) {
            ExerciseEditorDialog(
                initial = ex,
                onDismiss = { editingExercise = null },
                onSave = { updated ->
                    days[i].exercises[j] = updated
                    editingExercise = null
                }
            )
        }
    }

    addingTo?.let { i ->
        ExerciseEditorDialog(
            initial = null,
            onDismiss = { addingTo = null },
            onSave = { ex ->
                days[i].exercises.add(ex)
                addingTo = null
            }
        )
    }

    /* ---------- 编辑其他训练 ---------- */
    editingOther?.let { (i, j) ->
        val other = days.getOrNull(i)?.others?.getOrNull(j)
        if (other != null) {
            ParsedOtherEditorDialog(
                initial = other,
                onDismiss = { editingOther = null },
                onSave = { updated ->
                    days[i].others[j] = updated
                    editingOther = null
                }
            )
        }
    }

    /* ---------- 替换确认 ---------- */
    if (confirmReplace) {
        ConfirmDialog(
            title = "目标周已有安排",
            message = "这一周已经制定过计划。导入会替换被勾选那几天的安排，" +
                "这些天已完成的打卡会被撤销，已发放的金币将一并扣回（可能把余额扣成负数）。",
            confirmText = "替换",
            danger = true,
            onDismiss = { confirmReplace = false },
            onConfirm = {
                doImport(targetWeek, selected, data.otherTypes)
                confirmReplace = false
                done = "已导入 ${selected.size} 天"
            }
        )
    }

    done?.let { msg ->
        ConfirmDialog(
            title = "导入完成",
            message = msg,
            confirmText = "好",
            onDismiss = onDismiss,
            onConfirm = onDismiss
        )
    }
}

/* ==================== 其他训练：编辑 ==================== */

@Composable
private fun ParsedOtherEditorDialog(
    initial: PlanTextParser.ParsedOther,
    onDismiss: () -> Unit,
    onSave: (PlanTextParser.ParsedOther) -> Unit
) {
    var name by remember { mutableStateOf(initial.typeName) }
    var amount by remember { mutableStateOf(fmtAmount(initial.amount)) }
    var unit by remember { mutableStateOf(initial.unit) }

    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val valid = name.isNotBlank() && amountValue > 0

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("其他训练", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("训练类型") }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.weight(1f), singleLine = true,
                        label = { Text("目标量") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = unit, onValueChange = { unit = it },
                        modifier = Modifier.weight(1f), singleLine = true,
                        label = { Text("单位") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(enabled = valid, onClick = { onSave(initial.copy(typeName = name.trim(), amount = amountValue, unit = unit.trim())) }) {
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

/* ==================== 落库 ==================== */

/**
 * 把预览结果写入 [targetWeek]。
 * - 运动类型若不存在会自动新建（金币单价默认 2，之后可在计划页调整）
 * - 目标周已有内容时由调用方先做替换确认
 */
private fun doImport(
    targetWeek: String,
    days: List<PlanTextParser.ParsedDay>,
    types: List<OtherType>
) {
    val known = types.associateBy { it.name }.toMutableMap()

    days.forEach { day ->
        val dow = day.dayOfWeek
        if (day.isRest && day.exercises.isEmpty()) {
            Store.saveDayPlan(targetWeek, dow, true, emptyList())
        } else {
            Store.saveDayPlan(targetWeek, dow, false, day.exercises)
        }

        day.others.forEach { other ->
            val type = known[other.typeName] ?: OtherType(
                id = UUID.randomUUID().toString(),
                name = other.typeName,
                unit = other.unit,
                coinPerUnit = 2
            ).also {
                Store.saveOtherType(it)
                known[other.typeName] = it
            }
            Store.saveOtherPlan(
                OtherPlan(
                    id = UUID.randomUUID().toString(),
                    weekKey = targetWeek,
                    typeId = type.id,
                    typeName = type.name,
                    unit = type.unit,
                    dayOfWeek = dow,
                    targetAmount = other.amount,
                    rewardPerUnit = type.coinPerUnit
                )
            )
        }
    }
}

private fun fmtAmount(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
