package com.chensui.lianji.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chensui.lianji.data.Dates
import com.chensui.lianji.data.DayPlan
import com.chensui.lianji.data.DayRecord
import com.chensui.lianji.data.Exercise
import com.chensui.lianji.data.OtherDone
import com.chensui.lianji.data.OtherPlan
import com.chensui.lianji.data.Store
import com.chensui.lianji.data.setsRepsLabel
import com.chensui.lianji.ui.SoundFx
import com.chensui.lianji.ui.components.CoinBadge
import com.chensui.lianji.ui.components.EmptyHint
import com.chensui.lianji.ui.components.SectionCard
import com.chensui.lianji.ui.components.SectionTitle
import com.chensui.lianji.ui.components.SimpleProgress
import com.chensui.lianji.ui.theme.CoinGold
import com.chensui.lianji.ui.theme.RestGray
import com.chensui.lianji.ui.theme.WarnOrange
import com.chensui.lianji.ui.theme.doneColor

@Composable
fun DailyScreen(modifier: Modifier = Modifier) {
    val data by Store.data.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val today = Dates.now()
    val todayKey = Dates.key(today)
    val dow = Dates.dow(today)
    val weekKey = Dates.weekKeyOf(today)
    val storedWeek = data.plans[weekKey]
    val plan = storedWeek?.dayOf(dow) ?: DayPlan(dow)
    val record = data.records[todayKey] ?: DayRecord(todayKey)
    val otherPlans = data.otherPlans.filter { it.weekKey == weekKey && it.dayOfWeek == dow }

    val sound = data.soundEnabled
    val haptics = data.hapticEnabled

    var otherTarget by remember { mutableStateOf<OtherPlan?>(null) }
    var feelingOpen by remember { mutableStateOf(false) }

    /** 根据点击结果给出不同的听觉与触觉反馈 */
    val onToggleExercise: (String) -> Unit = { id ->
        val wasDone = id in record.doneExerciseIds
        val validIds = plan.exercises.map { it.id }
        val doneNow = record.doneExerciseIds.count { it in validIds }
        val afterCount = if (wasDone) doneNow - 1 else doneNow + 1
        val willFinishAll = !wasDone && validIds.isNotEmpty() &&
                afterCount >= validIds.size && !record.workoutChecked

        if (willFinishAll) {
            SoundFx.celebrate(sound)
            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (!wasDone) {
            SoundFx.tick(sound)
            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            SoundFx.undo(sound)
            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        Store.toggleExercise(todayKey, id)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        /* ---------- 顶部 ---------- */
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${today.monthValue} 月 ${today.dayOfMonth} 日 · ${Dates.weekLabel(dow)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when {
                            storedWeek == null -> "本周还没有计划"
                            plan.isRestDay -> "今天是休息日"
                            plan.exercises.isEmpty() -> "今天没有安排动作"
                            record.workoutChecked -> "今天已经练完了"
                            else -> "今天有 ${plan.exercises.size} 个动作"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                CoinBadge(coins = data.coins)
            }
        }

        /* ---------- 今日训练 ---------- */
        item {
            when {
                storedWeek == null -> NoPlanCard()
                plan.isRestDay -> RestDayCard()
                else -> WorkoutCard(
                    exercises = plan.exercises,
                    record = record,
                    rewardCoin = data.rewardConfig.workoutCoin,
                    onToggle = onToggleExercise
                )
            }
        }

        /* ---------- 其他训练 ---------- */
        if (otherPlans.isNotEmpty()) {
            item {
                OtherTrainingCard(
                    plans = otherPlans,
                    done = record.otherDone,
                    coinsEarned = record.otherDone.sumOf { it.coin },
                    onComplete = { p ->
                        SoundFx.tick(sound)
                        otherTarget = p
                    },
                    onUndo = { id ->
                        val coin = record.otherDone.firstOrNull { it.planId == id }?.coin ?: 0
                        if (coin > 0) SoundFx.undo(sound) else SoundFx.tick(sound)
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        Store.undoOther(todayKey, id)
                    }
                )
            }
        }

        /* ---------- 训练感受 ---------- */
        if (record.workoutChecked || record.otherChecked) {
            item {
                FeelingCard(text = record.feeling, onClick = { feelingOpen = true })
            }
        }
    }

    otherTarget?.let { p ->
        NumberInputDialog(
            title = "完成${p.typeName}",
            label = "实际完成量（${p.unit}）",
            hint = fmtNum(p.targetAmount),
            helper = "每 ${p.unit} 奖励 ${p.rewardPerUnit} 金币",
            onDismiss = { otherTarget = null },
            onConfirm = { value ->
                SoundFx.reward(sound)
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Store.completeOther(todayKey, p.id, value)
                otherTarget = null
            }
        )
    }

    if (feelingOpen) {
        TextInputDialog(
            title = "今日训练感受",
            label = "记录今天的感受与下次要优化的地方",
            initial = record.feeling,
            maxLines = 6,
            onDismiss = { feelingOpen = false },
            onConfirm = { text ->
                Store.saveFeeling(todayKey, text)
                feelingOpen = false
            }
        )
    }
}

private fun fmtNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

/* ==================== 本周未制定计划 ==================== */

@Composable
private fun NoPlanCard() {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text("计", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("本周还没有计划", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "去「计划」页安排这一周，或者一键延续上周的安排。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ==================== 休息日 ==================== */

@Composable
private fun RestDayCard() {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(RestGray.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Text("休", color = RestGray, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("休息日", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "今天不计入打卡目标，也不记录训练。好好恢复，肌肉是在休息时长出来的。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ==================== 今日训练 ==================== */

@Composable
private fun WorkoutCard(
    exercises: List<Exercise>,
    record: DayRecord,
    rewardCoin: Int,
    onToggle: (String) -> Unit
) {
    val validIds = exercises.map { it.id }
    val doneCount = record.doneExerciseIds.count { it in validIds }
    val total = exercises.size
    val accent = doneColor()

    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else doneCount.toFloat() / total,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "progress"
    )

    SectionCard {
        SectionTitle(
            text = "今日训练",
            trailing = if (total == 0) null else "$doneCount / $total"
        )
        Spacer(Modifier.height(10.dp))

        if (total == 0) {
            EmptyHint("周计划里还没给今天安排动作，去「计划」页添加吧")
        } else {
            SimpleProgress(
                progress = progress,
                color = if (record.workoutChecked) accent else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            exercises.forEach { ex ->
                ExerciseRow(
                    exercise = ex,
                    done = ex.id in record.doneExerciseIds,
                    onClick = { onToggle(ex.id) }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(2.dp))

            AnimatedVisibility(
                visible = record.workoutChecked,
                enter = fadeIn(tween(280)) + expandVertically(tween(320))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.13f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✓", color = accent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "已完成打卡，获得 $rewardCoin 金币",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!record.workoutChecked) {
                Text(
                    text = "全部完成后自动打卡，奖励 $rewardCoin 金币",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 单个动作行。
 *
 * 反馈由三层叠加：
 * 1. 按下时整行轻微缩回，松手弹回；
 * 2. 勾选圆用带过冲的 spring 弹出，同时底色由灰转绿；
 * 3. 音效与触感在点击回调里按结果区分（见 DailyScreen.onToggleExercise）。
 */
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    done: Boolean,
    onClick: () -> Unit
) {
    val accent = doneColor()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessHigh),
        label = "pressScale"
    )

    val bg by animateColorAsState(
        targetValue = if (done) accent.copy(alpha = 0.13f)
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(260),
        label = "rowBg"
    )

    // 0 → 1，spring 会自然过冲，形成"弹一下"的手感
    val check by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium),
        label = "check"
    )
    val checkAlpha = check.coerceIn(0f, 1f)
    val checkScale = 0.75f + 0.25f * check.coerceIn(0f, 1.35f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                    }
                    .clip(CircleShape)
                    .background(accent.copy(alpha = checkAlpha))
                    .border(
                        width = if (checkAlpha > 0.99f) 0.dp else 1.5.dp,
                        color = RestGray.copy(alpha = 1f - checkAlpha),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color.White.copy(alpha = checkAlpha),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                if (exercise.note.isNotBlank()) {
                    Text(
                        text = exercise.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = exercise.setsRepsLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = if (done) accent.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ==================== 其他训练 ==================== */

@Composable
private fun OtherTrainingCard(
    plans: List<OtherPlan>,
    done: List<OtherDone>,
    coinsEarned: Int,
    onComplete: (OtherPlan) -> Unit,
    onUndo: (String) -> Unit
) {
    SectionCard {
        SectionTitle(
            text = "其他训练",
            trailing = if (coinsEarned > 0) "已得 $coinsEarned 金币" else null
        )
        Spacer(Modifier.height(10.dp))

        plans.forEach { p ->
            val d = done.firstOrNull { it.planId == p.id }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = p.typeName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (d != null) "已完成 ${fmtNum(d.amount)} ${d.unit}，+${d.coin} 金币"
                        else "目标 ${fmtNum(p.targetAmount)} ${p.unit} · 每 ${p.unit} ${p.rewardPerUnit} 金币",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (d != null) doneColor() else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (d != null) {
                    TextButton(onClick = { onUndo(p.id) }) {
                        Text("撤销", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    TextButton(onClick = { onComplete(p) }) {
                        Text("完成", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "其他训练奖励与健身打卡相互独立",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ==================== 训练感受 ==================== */

@Composable
private fun FeelingCard(text: String, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("训练感受", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (text.isBlank()) "去写" else "编辑",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = text.ifBlank { "记录今天的状态、发力感受，提醒下次动作怎么优化。" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (text.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
