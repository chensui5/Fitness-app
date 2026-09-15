package com.chensui.lianji.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chensui.lianji.data.Dates
import com.chensui.lianji.data.DayPlan
import com.chensui.lianji.data.Exercise
import com.chensui.lianji.data.OtherPlan
import com.chensui.lianji.data.OtherType
import com.chensui.lianji.data.Store
import com.chensui.lianji.ui.components.ChipSelector
import com.chensui.lianji.ui.components.CoinBadge
import com.chensui.lianji.ui.components.EmptyHint
import com.chensui.lianji.ui.components.SectionCard
import com.chensui.lianji.ui.components.SectionTitle
import com.chensui.lianji.ui.components.StatusChip
import com.chensui.lianji.ui.components.ThinDivider
import com.chensui.lianji.ui.components.sanitizePositiveIntInput
import com.chensui.lianji.ui.theme.CoinGold
import com.chensui.lianji.ui.theme.RestGray
import com.chensui.lianji.ui.theme.WarnOrange
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun WeekPlanScreen(modifier: Modifier = Modifier) {
    val data by Store.data.collectAsStateWithLifecycle()

    val thisWeek = Store.currentWeekKey()
    var weekKey by remember { mutableStateOf(thisWeek) }
    var editingDay by remember { mutableStateOf<Int?>(null) }
    var editingOther by remember { mutableStateOf<Any?>(null) }
    var editingWorkoutCoin by remember { mutableStateOf(false) }
    var otherTypeDialog by remember { mutableStateOf<OtherType?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmCarry by remember { mutableStateOf(false) }

    val stored = data.plans[weekKey]
    val todayMonday = Dates.mondayOf(Dates.now())
    val monday = Dates.mondayOf(Dates.parse(weekKey) ?: Dates.now())
    val sunday = monday.plusDays(6)

    val relative = when (weekKey) {
        thisWeek -> "本周"
        Dates.key(todayMonday.plusWeeks(1)) -> "下周"
        Dates.key(todayMonday.minusWeeks(1)) -> "上周"
        else -> null
    }

    val canPrev = monday.isAfter(todayMonday.minusWeeks(8))
    val canNext = monday.isBefore(todayMonday.plusWeeks(8))
    val prevPlanKey = Store.latestPlanBefore(weekKey)

    // 早于昨天的日期已成历史，不可再制定或修改计划（防止回填过去刷奖励）
    val editableDays = Store.editableDaysIn(weekKey)
    val canEditThisWeek = editableDays.isNotEmpty()
    val weekFullyPast = !canEditThisWeek

    // 其他训练同样按周存储，这里只取当前查看周的安排
    val weekOthers = data.otherPlans
        .filter { it.weekKey == weekKey }
        .sortedBy { it.dayOfWeek }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        /* ---------- 标题 ---------- */
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "每周计划",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "每周独立制定，不会自动循环到下一周",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CoinBadge(coins = data.coins, compact = true)
            }
        }

        /* ---------- 周切换 ---------- */
        item {
            WeekSwitcher(
                monday = monday,
                sunday = sunday,
                relative = relative,
                canPrev = canPrev,
                canNext = canNext,
                onPrev = { weekKey = Dates.key(monday.minusWeeks(1)) },
                onNext = { weekKey = Dates.key(monday.plusWeeks(1)) },
                onBackToThisWeek = { weekKey = thisWeek }
            )
        }

        /* ---------- 历史周提示 ---------- */
        if (weekFullyPast) {
            item {
                InfoBanner(
                    "这是历史计划，不可再新增或修改。只有昨天及以后的日期才能制定计划，已产生的打卡记录不受影响。",
                    RestGray
                )
            }
        }

        /* ---------- 该周尚未制定计划 ---------- */
        if (stored == null && canEditThisWeek) {
            item {
                EmptyWeekCard(
                    canCarry = prevPlanKey != null,
                    onCarry = { confirmCarry = true }
                )
            }
        }

        /* ---------- 七天 ---------- */
        items(7) { idx ->
            val dow = idx + 1
            val day = stored?.dayOf(dow) ?: DayPlan(dow)
            val date = monday.plusDays((dow - 1).toLong())
            DayPlanCard(
                date = date,
                plan = day,
                scheduled = stored != null,
                isToday = date == Dates.now(),
                editable = dow in editableDays,
                onEdit = { editingDay = dow }
            )
        }

        /* ---------- 周级操作：延续 / 清空（同一排、同一套样式） ---------- */
        if (stored != null && canEditThisWeek) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        enabled = prevPlanKey != null,
                        onClick = { confirmCarry = true }
                    ) {
                        Text(
                            text = "延续上周计划",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (prevPlanKey != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    TextButton(onClick = { confirmClear = true }) {
                        Text(
                            text = "清空这一周的计划",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = WarnOrange
                        )
                    }
                }
            }
        }

        /* ---------- 其他训练计划 ---------- */
        item {
            SectionCard {
                SectionTitle(
                    text = "其他训练计划",
                    trailing = if (weekOthers.isEmpty()) null else "${weekOthers.size} 项"
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "与其他训练奖励独立，同样按周制定",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                if (weekOthers.isEmpty()) {
                    EmptyHint("这一周还没有安排跑步、跳绳等训练")
                } else {
                    weekOthers.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (canEditThisWeek) Modifier.clickable { editingOther = p }
                                    else Modifier
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusChip(
                                text = Dates.weekLabel(p.dayOfWeek),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    p.typeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "目标 ${fmtNum(p.targetAmount)} ${p.unit} · 每 ${p.unit} ${p.rewardPerUnit} 金币",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canEditThisWeek) {
                                TextButton(onClick = { Store.deleteOtherPlan(p.id) }) {
                                    Text("删除", color = WarnOrange, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        ThinDivider()
                    }
                }

                Spacer(Modifier.height(8.dp))
                if (canEditThisWeek) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingOther = "new" }) {
                            Text("+ 添加训练计划", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Text(
                        "历史计划，不可新增或修改",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        /* ---------- 其他训练金币单价 ---------- */
        item {
            SectionCard {
                SectionTitle(text = "其他训练金币单价")
                Spacer(Modifier.height(4.dp))
                Text(
                    "改动后需等 7 天才能再次修改",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                data.otherTypes.forEach { t ->
                    val lock = Store.daysLeft(t.lockedUntil)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (lock > 0) "每 ${t.unit} ${t.coinPerUnit} 金币 · ${lock} 天后可改"
                                else "每 ${t.unit} ${t.coinPerUnit} 金币 · 可修改",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (lock > 0) MaterialTheme.colorScheme.onSurfaceVariant else CoinGold
                            )
                        }
                        TextButton(
                            enabled = lock == 0,
                            onClick = { otherTypeDialog = t }
                        ) {
                            Text(
                                "调整",
                                color = if (lock == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ThinDivider()
                }

                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        otherTypeDialog = OtherType(name = "", unit = "公里", coinPerUnit = 1)
                    }
                ) {
                    Text("+ 新增训练类型", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        /* ---------- 健身打卡奖励 ---------- */
        item {
            val lock = Store.daysLeft(data.rewardConfig.workoutLockedUntil)
            SectionCard {
                SectionTitle(text = "健身打卡奖励")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${data.rewardConfig.workoutCoin} 金币 / 次",
                            style = MaterialTheme.typography.titleMedium,
                            color = CoinGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lock > 0) "改动后需等 7 天，还剩 $lock 天" else "当前可修改",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(enabled = lock == 0, onClick = { editingWorkoutCoin = true }) {
                        Text(
                            "修改",
                            color = if (lock == 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    /* ---------- 弹窗 ---------- */
    editingDay?.let { dow ->
        DayPlanEditorDialog(
            dayOfWeek = dow,
            initial = stored?.dayOf(dow) ?: DayPlan(dow),
            onDismiss = { editingDay = null },
            onSave = { rest, list ->
                Store.saveDayPlan(weekKey, dow, rest, list)
                editingDay = null
            }
        )
    }

    editingOther?.let { target ->
        OtherPlanEditorDialog(
            types = data.otherTypes,
            initial = target as? OtherPlan,
            weekKey = weekKey,
            allowedDays = editableDays,
            onDismiss = { editingOther = null },
            onSave = { plan ->
                Store.saveOtherPlan(plan)
                editingOther = null
            }
        )
    }

    otherTypeDialog?.let { t ->
        OtherTypeEditorDialog(
            initial = t,
            isNew = t.name.isBlank(),
            onDismiss = { otherTypeDialog = null },
            onSave = { updated, coin ->
                if (updated.name.isBlank()) {
                    otherTypeDialog = null
                    return@OtherTypeEditorDialog
                }
                if (coin != null) {
                    Store.saveOtherType(updated)
                    Store.setOtherCoinPerUnit(updated.id, coin)
                } else {
                    Store.saveOtherType(updated)
                }
                otherTypeDialog = null
            }
        )
    }

    if (editingWorkoutCoin) {
        NumberInputDialog(
            title = "健身打卡奖励",
            label = "每次打卡奖励金币",
            hint = "${data.rewardConfig.workoutCoin}",
            helper = "设定后需等 7 天才能再次修改",
            onDismiss = { editingWorkoutCoin = false },
            onConfirm = { v ->
                Store.setWorkoutCoin(v.toInt())
                editingWorkoutCoin = false
            }
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "清空这一周的计划",
            message = "将删除这一周的全部安排。该周已完成的打卡会一并撤销，已发放的金币将被扣回（可能把余额扣成负数）。",
            confirmText = "清空",
            danger = true,
            onDismiss = { confirmClear = false },
            onConfirm = {
                Store.clearWeek(weekKey)
                confirmClear = false
            }
        )
    }

    if (confirmCarry) {
        ConfirmDialog(
            title = "延续上周计划",
            message = if (stored == null) {
                "将把最近一次制定过计划的那一周的安排，整套复制到这一周（动作会重新生成）。"
            } else {
                "将用最近一次制定过计划的那一周的安排，覆盖这一周现有的安排。该周已完成的打卡会一并撤销，已发放的金币将被扣回（可能把余额扣成负数）。"
            },
            confirmText = "延续",
            onDismiss = { confirmCarry = false },
            onConfirm = {
                prevPlanKey?.let { Store.carryOverWeek(it, weekKey) }
                confirmCarry = false
            }
        )
    }
}

private fun fmtNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

/* ==================== 周切换器 ==================== */

@Composable
private fun WeekSwitcher(
    monday: LocalDate,
    sunday: LocalDate,
    relative: String?,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBackToThisWeek: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(enabled = canPrev, onClick = onPrev) {
                Text(
                    "‹",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (canPrev) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${monday.monthValue} 月 ${monday.dayOfMonth} 日",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "  –  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${sunday.monthValue} 月 ${sunday.dayOfMonth} 日",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(2.dp))
                if (relative != null) {
                    Text(
                        relative,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TextButton(
                        onClick = onBackToThisWeek,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "回到本周",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            TextButton(enabled = canNext, onClick = onNext) {
                Text(
                    "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (canNext) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ==================== 空周提示 ==================== */

@Composable
private fun EmptyWeekCard(canCarry: Boolean, onCarry: () -> Unit) {
    SectionCard {
        Text(
            "这一周还没有安排",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "每周的计划是独立的。可以直接延续上一周，也可以点下面任意一天从头制定。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.then(
                if (canCarry) Modifier.clickable { onCarry() } else Modifier
            ),
            shape = RoundedCornerShape(10.dp),
            color = if (canCarry) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = if (canCarry) "延续上一周的计划" else "没有可延续的历史计划",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (canCarry) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/* ==================== 单日卡片 ==================== */

@Composable
private fun DayPlanCard(
    date: LocalDate,
    plan: DayPlan,
    scheduled: Boolean,
    isToday: Boolean,
    editable: Boolean,
    onEdit: () -> Unit
) {
    SectionCard(onClick = if (editable) onEdit else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Dates.weekLabel(plan.dayOfWeek),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${date.monthValue}/${date.dayOfMonth}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isToday) {
                Spacer(Modifier.width(8.dp))
                StatusChip("今天", MaterialTheme.colorScheme.primary)
            }
            if (!editable) {
                Spacer(Modifier.width(8.dp))
                StatusChip("已过", RestGray)
            }
            Spacer(Modifier.weight(1f))
            when {
                !scheduled -> StatusChip("未安排", RestGray)
                plan.isRestDay -> StatusChip("休息日", RestGray)
                else -> StatusChip("${plan.exercises.size} 个动作", MaterialTheme.colorScheme.primary)
            }
        }

        if (scheduled && !plan.isRestDay && plan.exercises.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            plan.exercises.forEach { ex ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ex.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${ex.sets} 组 × ${ex.reps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (editable && (!scheduled || (!plan.isRestDay && plan.exercises.isEmpty()))) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "点击添加动作",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ==================== 单日计划编辑 ==================== */

/** 拖动排序手柄：三条横线 */
@Composable
private fun DragHandle(active: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(15.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun DayPlanEditorDialog(
    dayOfWeek: Int,
    initial: DayPlan,
    onDismiss: () -> Unit,
    onSave: (Boolean, List<Exercise>) -> Unit
) {
    var rest by remember { mutableStateOf(initial.isRestDay) }
    val rows = remember {
        mutableStateListOf<Exercise>().also { it.addAll(initial.exercises) }
    }
    var adding by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    /* ---- 拖动排序：长按手柄后接管手势，避免和弹窗滚动打架 ---- */
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val rowHeight = 56.dp
    val rowHeightPx = with(density) { rowHeight.toPx() }
    var dragFrom by remember { mutableStateOf(-1) }
    var dragDelta by remember { mutableStateOf(0f) }
    // 手指每拖过一格，目标位置就换一格
    val dragTo = if (dragFrom < 0 || rows.isEmpty()) -1
    else (dragFrom + (dragDelta / rowHeightPx).roundToInt()).coerceIn(0, rows.lastIndex)

    fun finishDrag(commit: Boolean) {
        if (commit && dragFrom >= 0 && dragTo >= 0 && dragFrom != dragTo) {
            val moved = rows.removeAt(dragFrom)
            rows.add(dragTo, moved)
        }
        dragFrom = -1
        dragDelta = 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "${Dates.weekLabel(dayOfWeek)} 的计划",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("设为休息日", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "休息日不记录动作，不计入打卡目标",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = rest, onCheckedChange = { rest = it })
                }

                if (rest) {
                    Spacer(Modifier.height(10.dp))
                    InfoBanner("这天将不显示训练内容，也无法打卡", RestGray)
                } else {
                    Spacer(Modifier.height(14.dp))
                    ThinDivider()
                    Spacer(Modifier.height(10.dp))

                    if (rows.isEmpty()) {
                        Text(
                            "还没有动作",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "点一下动作即可修改；长按左侧三横线可上下拖动排序",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        rows.forEachIndexed { index, ex ->
                            val dragging = dragFrom == index
                            // 被拖的那一行跟手，其余行让位
                            val shift = when {
                                dragFrom < 0 -> 0f
                                dragging -> dragDelta
                                dragFrom < dragTo && index in (dragFrom + 1)..dragTo -> -rowHeightPx
                                dragFrom > dragTo && index in dragTo until dragFrom -> rowHeightPx
                                else -> 0f
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                                    .zIndex(if (dragging) 1f else 0f)
                                    .graphicsLayer { translationY = shift }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { editingIndex = index },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .fillMaxHeight()
                                            .pointerInput(index, rows.size) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        dragFrom = index
                                                        dragDelta = 0f
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                    onDrag = { change, amount ->
                                                        change.consume()
                                                        dragDelta += amount.y
                                                    },
                                                    onDragEnd = { finishDrag(true) },
                                                    onDragCancel = { finishDrag(false) }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DragHandle(active = dragging)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(ex.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${ex.sets} 组 × ${ex.reps}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "修改 ›",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    TextButton(onClick = { rows.removeAt(index) }) {
                                        Text("移除", color = WarnOrange, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                ThinDivider(modifier = Modifier.align(Alignment.BottomStart))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { adding = true }) {
                        Text("+ 添加动作", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onSave(rest, rows.toList()) }) {
                        Text("保存", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (adding) {
        ExerciseEditorDialog(
            onDismiss = { adding = false },
            onSave = { ex ->
                rows.add(ex)
                adding = false
            }
        )
    }

    editingIndex?.let { idx ->
        ExerciseEditorDialog(
            initial = rows.getOrNull(idx),
            onDismiss = { editingIndex = null },
            onSave = { ex ->
                if (idx in rows.indices) rows[idx] = ex
                editingIndex = null
            }
        )
    }
}

/** 动作「每组数量」的可选单位 —— 只能从这里选，不允许自由输入或删除 */
private val REP_UNITS = listOf("次", "秒", "分钟")

/** 解析已存的动作描述（形如 "10 次"）；无法解析时回落到 12 次 */
private fun parseReps(raw: String): Pair<Int, String> {
    val m = Regex("""^\s*(\d+)\s*(\S*)\s*$""").find(raw) ?: return 12 to "次"
    val value = m.groupValues[1].toIntOrNull() ?: 12
    val unit = m.groupValues[2].takeIf { it in REP_UNITS } ?: "次"
    return value to unit
}

@Composable
private fun ExerciseEditorDialog(
    initial: Exercise? = null,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sets by remember { mutableStateOf((initial?.sets ?: 4).toString()) }
    val parsed = remember { parseReps(initial?.reps ?: "") }
    var unit by remember { mutableStateOf(parsed.second) }
    var qty by remember { mutableStateOf(parsed.first.toString()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    val setsValue = sets.toIntOrNull() ?: 0
    val qtyValue = qty.toIntOrNull() ?: 0
    val setsOk = setsValue > 0
    val qtyOk = qtyValue > 0
    val valid = name.isNotBlank() && setsOk && qtyOk

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    if (initial == null) "添加动作" else "修改动作",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("动作名称，如：卧推") }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = sanitizePositiveIntInput(it) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("组数（只填数字）") },
                    isError = !setsOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (!setsOk) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "组数需为大于 0 的整数",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarnOrange
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "每组数量",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                ChipSelector(
                    options = REP_UNITS,
                    selected = unit,
                    onSelect = { unit = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = sanitizePositiveIntInput(it) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("数量（只填数字）") },
                    isError = !qtyOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (!qtyOk) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "数量需为大于 0 的整数",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarnOrange
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("备注（可选），如：注意沉肩") }
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "单位只能从上面选择；习惯说「力竭」的，请写在备注里。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        enabled = valid,
                        onClick = {
                            onSave(
                                Exercise(
                                    id = initial?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    sets = setsValue,
                                    reps = "$qtyValue $unit",
                                    note = note.trim()
                                )
                            )
                        }
                    ) {
                        Text(
                            if (initial == null) "添加" else "保存",
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

/* ==================== 其他训练计划编辑 ==================== */

@Composable
private fun OtherPlanEditorDialog(
    types: List<OtherType>,
    initial: OtherPlan?,
    weekKey: String,
    allowedDays: List<Int>,
    onDismiss: () -> Unit,
    onSave: (OtherPlan) -> Unit
) {
    var typeId by remember { mutableStateOf(initial?.typeId ?: types.firstOrNull()?.id ?: "") }
    var dow by remember { mutableStateOf(initial?.dayOfWeek ?: Dates.dow(Dates.now())) }
    var target by remember { mutableStateOf(initial?.targetAmount?.let { fmtNum(it) } ?: "5") }

    val type = types.firstOrNull { it.id == typeId }
    val targetValue = target.toDoubleOrNull() ?: 0.0
    val valid = type != null && targetValue > 0

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp).heightIn(max = 540.dp).verticalScroll(rememberScrollState())) {
                Text("其他训练计划", style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(14.dp))
                Text(
                    "训练类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                if (types.isEmpty()) {
                    Text(
                        "请先在下方「其他训练金币单价」里新增训练类型",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarnOrange
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        types.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                pair.forEach { t ->
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable { typeId = t.id },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (t.id == typeId) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = t.name,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (t.id == typeId) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "安排在哪天",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (1..7).forEach { d ->
                        val allowed = d in allowedDays
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .then(if (allowed) Modifier.clickable { dow = d } else Modifier),
                            shape = RoundedCornerShape(8.dp),
                            color = if (d == dow && allowed) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = Dates.weekLabel(d).removePrefix("周"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        !allowed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        d == dow -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "浅色的日期已成历史，不可再安排计划",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("目标量（${type?.unit ?: "单位"}）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (type != null && targetValue > 0) {
                    Spacer(Modifier.height(10.dp))
                    InfoBanner(
                        "完成后可获得约 ${(targetValue * type.coinPerUnit).toInt()} 金币（按实际完成量计算）",
                        CoinGold
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        enabled = valid,
                        onClick = {
                            val t = type ?: return@TextButton
                            onSave(
                                OtherPlan(
                                    id = initial?.id ?: UUID.randomUUID().toString(),
                                    weekKey = weekKey,
                                    typeId = t.id,
                                    typeName = t.name,
                                    unit = t.unit,
                                    dayOfWeek = dow,
                                    targetAmount = targetValue,
                                    rewardPerUnit = t.coinPerUnit
                                )
                            )
                        }
                    ) {
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

/* ==================== 训练类型编辑 ==================== */

@Composable
private fun OtherTypeEditorDialog(
    initial: OtherType,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (OtherType, Int?) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var unit by remember { mutableStateOf(initial.unit) }
    var coin by remember { mutableStateOf(initial.coinPerUnit.toString()) }

    val lockDays = Store.daysLeft(initial.lockedUntil)
    val valid = name.isNotBlank() && unit.isNotBlank() && (coin.toIntOrNull() ?: -1) >= 0

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    if (isNew) "新增训练类型" else "调整金币单价",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    enabled = isNew,
                    label = { Text("名称，如 跑步") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = unit, onValueChange = { unit = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    enabled = isNew,
                    label = { Text("计量单位，如 公里 / 个 / 分钟") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = coin,
                    onValueChange = { coin = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("每单位奖励金币") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isNew) "新增后即可在计划中选用" else "修改后需等 7 天才能再次调整",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isNew && lockDays > 0) {
                    Spacer(Modifier.height(8.dp))
                    InfoBanner("该单价还有 $lockDays 天锁定期", WarnOrange)
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!isNew) {
                        TextButton(onClick = { Store.deleteOtherType(initial.id); onDismiss() }) {
                            Text("删除类型", color = WarnOrange)
                        }
                    }
                    TextButton(
                        enabled = valid,
                        onClick = {
                            val c = coin.toIntOrNull() ?: 0
                            onSave(
                                initial.copy(name = name.trim(), unit = unit.trim(), coinPerUnit = c),
                                c
                            )
                        }
                    ) {
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
