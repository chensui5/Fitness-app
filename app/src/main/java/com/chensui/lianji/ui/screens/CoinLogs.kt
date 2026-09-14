package com.chensui.lianji.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chensui.lianji.data.CoinLog
import com.chensui.lianji.data.Dates
import com.chensui.lianji.data.Store
import com.chensui.lianji.ui.components.EmptyHint
import com.chensui.lianji.ui.components.ThinDivider
import com.chensui.lianji.ui.theme.CoinGold
import com.chensui.lianji.ui.theme.WarnOrange
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * 金币流水。
 *
 * 展示全部收支明细：打卡发放、其他训练奖励、商店兑换，以及计划变更导致的金币回收。
 * 回收会以负数出现，因此「累计消耗」与「已回收」分开统计，账目才对得上。
 */
@Composable
fun CoinLogDialog(onDismiss: () -> Unit) {
    val data by Store.data.collectAsStateWithLifecycle()
    val logs = data.coinLogs   // Store 里已保证最新的在前

    val earned = logs.filter { it.amount > 0 }.sumOf { it.amount }
    val spent = logs.filter { it.amount < 0 }.sumOf { it.amount }   // 负数

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 22.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "金币流水",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                /* ---------- 概览 ---------- */
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "当前余额",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "${data.coins}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (data.coins < 0) WarnOrange else CoinGold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            SumCell("累计获得", "+$earned", CoinGold, Modifier.weight(1f))
                            SumCell("累计支出", "$spent", WarnOrange, Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                /* ---------- 明细 ---------- */
                if (logs.isEmpty()) {
                    EmptyHint("还没有任何金币记录")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        var lastDate: LocalDate? = null
                        logs.forEach { log ->
                            val date = dateOf(log.timestamp)
                            if (date != lastDate) {
                                DateHeader(date)
                                lastDate = date
                            }
                            LogRow(log)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/* ==================== 子组件 ==================== */

private fun dateOf(ts: Long): LocalDate =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

private fun timeOf(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

@Composable
private fun SumCell(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    val today = Dates.now()
    val label = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> "${date.monthValue} 月 ${date.dayOfMonth} 日"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun LogRow(log: CoinLog) {
    val positive = log.amount >= 0
    val accent = if (positive) CoinGold else WarnOrange

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 收支方向用一个小圆点区分，比纯文字更快扫读
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = log.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeOf(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = if (positive) "+${log.amount}" else "${log.amount}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
    ThinDivider()
}
