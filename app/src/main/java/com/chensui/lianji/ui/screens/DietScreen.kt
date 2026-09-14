package com.chensui.lianji.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chensui.lianji.data.Dates
import com.chensui.lianji.data.DietEntry
import com.chensui.lianji.data.MealSlots
import com.chensui.lianji.data.Store
import com.chensui.lianji.ui.components.SectionCard
import com.chensui.lianji.ui.theme.WarnOrange
import com.chensui.lianji.ui.theme.mealColor
import com.chensui.lianji.ui.theme.nutrientColor

@Composable
fun DietScreen(modifier: Modifier = Modifier) {
    val data by Store.data.collectAsStateWithLifecycle()
    val today = Dates.now()
    val todayKey = Dates.key(today)
    val diets = data.diets[todayKey] ?: emptyList()

    var pickerMeal by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<DietEntry?>(null) }

    val totalCal = diets.sumOf { it.calories }
    val totalPro = diets.sumOf { it.protein }
    val totalCarb = diets.sumOf { it.carbs }
    val totalFat = diets.sumOf { it.fat }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        /* ---------- 顶部 ---------- */
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
                Text(
                    text = "${today.monthValue} 月 ${today.dayOfMonth} 日 · ${Dates.weekLabel(Dates.dow(today))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "今日饮食",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        /* ---------- 总览 ---------- */
        item {
            IntakeSummary(
                calories = totalCal,
                protein = totalPro,
                carbs = totalCarb,
                fat = totalFat,
                count = diets.size
            )
        }

        /* ---------- 各餐次 ---------- */
        MealSlots.ALL.forEach { meal ->
            val entries = diets.filter { it.meal == meal }
            item(key = "meal_$meal") {
                MealSection(
                    meal = meal,
                    entries = entries,
                    onAdd = { pickerMeal = meal },
                    onRemove = { pendingDelete = it }
                )
            }
        }

        item {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "营养数据为参考估算值，实际会因烹饪方式有差异",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    pickerMeal?.let { meal ->
        FoodPickerDialog(
            meal = meal,
            customFoods = data.customFoods,
            onDismiss = { pickerMeal = null },
            onConfirm = { entry ->
                Store.addDiet(todayKey, entry.copy(meal = meal))
                pickerMeal = null
            }
        )
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = "删除记录",
            message = "确定删除「${entry.name} ${fmtGram(entry.grams)} g」吗？",
            confirmText = "删除",
            danger = true,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                Store.removeDiet(todayKey, entry.id)
                pendingDelete = null
            }
        )
    }
}

private fun fmtGram(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

/* ==================== 总览卡 ==================== */

@Composable
private fun IntakeSummary(
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    count: Int
) {
    val calColor = nutrientColor(3)

    SectionCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "总摄入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${calories.toInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        color = calColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "千卡",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Text(
                text = "共 $count 条",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        /* 三张营养素小卡片 */
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientTile("蛋白质", protein, nutrientColor(0), Modifier.weight(1f))
            NutrientTile("碳水", carbs, nutrientColor(1), Modifier.weight(1f))
            NutrientTile("脂肪", fat, nutrientColor(2), Modifier.weight(1f))
        }

        if (calories > 0) {
            Spacer(Modifier.height(14.dp))
            EnergySplitBar(protein, carbs, fat)
        }
    }
}

@Composable
private fun NutrientTile(
    label: String,
    grams: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.13f)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", grams),
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "g",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/** 三大营养素供能占比条：蛋白 4 kcal/g、碳水 4 kcal/g、脂肪 9 kcal/g */
@Composable
private fun EnergySplitBar(protein: Double, carbs: Double, fat: Double) {
    val pc = protein * 4.0
    val cc = carbs * 4.0
    val fc = fat * 9.0
    val sum = pc + cc + fc
    if (sum <= 0.0) return

    val pRatio = (pc / sum).toFloat()
    val cRatio = (cc / sum).toFloat()
    val fRatio = (fc / sum).toFloat()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (pRatio > 0f) Box(Modifier.weight(pRatio).fillMaxHeight().background(nutrientColor(0)))
            if (cRatio > 0f) Box(Modifier.weight(cRatio).fillMaxHeight().background(nutrientColor(1)))
            if (fRatio > 0f) Box(Modifier.weight(fRatio).fillMaxHeight().background(nutrientColor(2)))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SplitLegend("蛋白供能", pRatio, nutrientColor(0))
            SplitLegend("碳水供能", cRatio, nutrientColor(1))
            SplitLegend("脂肪供能", fRatio, nutrientColor(2))
        }
    }
}

@Composable
private fun SplitLegend(label: String, ratio: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(7.dp).height(7.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(
            text = "$label ${(ratio * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ==================== 餐次分区 ==================== */

@Composable
private fun MealSection(
    meal: String,
    entries: List<DietEntry>,
    onAdd: () -> Unit,
    onRemove: (DietEntry) -> Unit
) {
    val accent = mealColor(meal)
    val kcal = entries.sumOf { it.calories }

    SectionCard {
        /* 标题行：色块 + 餐次名 + 热量 + 添加 */
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = meal,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (entries.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = accent.copy(alpha = 0.14f)) {
                    Text(
                        text = "${kcal.toInt()} 千卡",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAdd) {
                Text("+ 添加", color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(6.dp))

        if (entries.isEmpty()) {
            Text(
                text = "还没有记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { entry ->
                    FoodEntryCard(entry = entry, accent = accent, onRemove = { onRemove(entry) })
                }
            }
        }
    }
}

/** 单条食物记录：左侧餐次色条 + 白底小卡片，营养明细用同色系小标签区分 */
@Composable
private fun FoodEntryCard(
    entry: DietEntry,
    accent: Color,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 6.dp, top = 9.dp, bottom = 9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${fmtGram(entry.grams)} g",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRemove) {
                        Text("删除", color = WarnOrange, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MacroTag("${entry.calories.toInt()}", "千卡", nutrientColor(3))
                    MacroTag(String.format("%.1f", entry.protein), "蛋白", nutrientColor(0))
                    MacroTag(String.format("%.1f", entry.carbs), "碳水", nutrientColor(1))
                    MacroTag(String.format("%.1f", entry.fat), "脂肪", nutrientColor(2))
                }
            }
        }
    }
}

@Composable
private fun MacroTag(value: String, label: String, color: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}
