package com.chensui.lianji.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chensui.lianji.data.DietEntry
import com.chensui.lianji.data.FoodItem
import com.chensui.lianji.data.FoodLibrary
import com.chensui.lianji.ui.components.SectionTitle
import com.chensui.lianji.ui.components.ThinDivider
import com.chensui.lianji.ui.theme.DoneGreen
import com.chensui.lianji.ui.theme.WarnOrange
import java.util.UUID

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

/* ==================== 通用外壳 ==================== */

@Composable
private fun SheetShell(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    confirmText: String = "确定",
    dismissText: String = "取消",
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (onConfirm != null) {
                        TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                            Text(
                                text = confirmText,
                                color = if (confirmEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ==================== 食物选择 ==================== */

@Composable
fun FoodPickerDialog(
    meal: String,
    customFoods: List<FoodItem>,
    onDismiss: () -> Unit,
    onConfirm: (DietEntry) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FoodItem?>(null) }
    var gramsText by remember { mutableStateOf("100") }
    var manual by remember { mutableStateOf(false) }

    val allFoods = remember(customFoods) { customFoods + FoodLibrary.items }

    val results: List<FoodItem> = remember(query, allFoods) {
        if (query.isBlank()) {
            emptyList()
        } else {
            allFoods.filter { it.name.contains(query.trim(), ignoreCase = true) }
                .sortedBy { it.name.length }
                .take(40)
        }
    }

    val quick = remember(allFoods) {
        FoodLibrary.quickPicks.mapNotNull { name -> allFoods.firstOrNull { it.name == name } }
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
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "添加到$meal",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { manual = !manual }) {
                        Text(
                            text = if (manual) "从食物库选" else "手动填写",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (manual) {
                    ManualFoodForm(
                        onCancel = onDismiss,
                        onConfirm = { entry -> onConfirm(entry) }
                    )
                } else if (selected == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("搜索食物，如：鸡胸肉") }
                    )
                    Spacer(Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (query.isBlank()) {
                            Text(
                                "常用食物",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            QuickGrid(quick) { selected = it }
                        } else {
                            if (results.isEmpty()) {
                                Text(
                                    "没有匹配的食物，可切到「手动填写」自行录入",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                results.forEach { food ->
                                    FoodRow(food) { selected = food }
                                    ThinDivider()
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text("关闭", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val food = selected!!
                    var grams = gramsText.toDoubleOrNull() ?: 100.0

                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "每 100 克：${fmt(food.calories)} 千卡 · 蛋白 ${fmt(food.protein)}g · 碳水 ${fmt(food.carbs)}g · 脂肪 ${fmt(food.fat)}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("食用量（克）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(50, 100, 150, 200).forEach { g ->
                            QuickAmountChip(g) {
                                gramsText = g.toString()
                                grams = g.toDouble()
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "按 ${fmt(grams)} 克计算",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SmallStat("热量", "${(food.calories * grams / 100).toInt()}", "千卡", Modifier.weight(1f))
                                SmallStat("蛋白质", fmt(food.protein * grams / 100), "g", Modifier.weight(1f))
                                SmallStat("碳水", fmt(food.carbs * grams / 100), "g", Modifier.weight(1f))
                                SmallStat("脂肪", fmt(food.fat * grams / 100), "g", Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selected = null; gramsText = "100" }) {
                            Text("重选", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(
                            onClick = {
                                onConfirm(
                                    DietEntry(
                                        id = UUID.randomUUID().toString(),
                                        name = food.name,
                                        grams = grams,
                                        calories = food.calories * grams / 100,
                                        protein = food.protein * grams / 100,
                                        carbs = food.carbs * grams / 100,
                                        fat = food.fat * grams / 100
                                    )
                                )
                            }
                        ) {
                            Text("添加", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickGrid(foods: List<FoodItem>, onPick: (FoodItem) -> Unit) {
    Column {
        foods.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { food ->
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onPick(food) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                text = food.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${fmt(food.calories)} 千卡/100g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FoodRow(food: FoodItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "蛋白 ${fmt(food.protein)}g · 碳水 ${fmt(food.carbs)}g · 脂肪 ${fmt(food.fat)}g / 100g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${fmt(food.calories)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickAmountChip(grams: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "${grams}g",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SmallStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "$label($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ManualFoodForm(
    onCancel: () -> Unit,
    onConfirm: (DietEntry) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var cal by remember { mutableStateOf("") }
    var pro by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val canSave = name.isNotBlank() && (cal.toDoubleOrNull() ?: -1.0) >= 0

    val g = grams.toDoubleOrNull() ?: 100.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("食物名称") }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = grams, onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' } },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("食用量（克，默认 100）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "以下按每 100 克填写，留空视为 0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        NumField("热量（千卡）", cal) { cal = it }
        Spacer(Modifier.height(6.dp))
        NumField("蛋白质（克）", pro) { pro = it }
        Spacer(Modifier.height(6.dp))
        NumField("碳水（克）", carb) { carb = it }
        Spacer(Modifier.height(6.dp))
        NumField("脂肪（克）", fat) { fat = it }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                enabled = canSave,
                onClick = {
                    val k = g / 100.0
                    onConfirm(
                        DietEntry(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            grams = g,
                            calories = (cal.toDoubleOrNull() ?: 0.0) * k,
                            protein = (pro.toDoubleOrNull() ?: 0.0) * k,
                            carbs = (carb.toDoubleOrNull() ?: 0.0) * k,
                            fat = (fat.toDoubleOrNull() ?: 0.0) * k
                        )
                    )
                }
            ) {
                Text(
                    "添加",
                    color = if (canSave) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

/* ==================== 数字输入 ==================== */

@Composable
fun NumberInputDialog(
    title: String,
    label: String,
    hint: String,
    helper: String = "",
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val value = text.toDoubleOrNull()
    val valid = value != null && value > 0

    SheetShell(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { if (valid) onConfirm(value!!) },
        confirmText = "确认",
        confirmEnabled = valid
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(hint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        if (helper.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = helper,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ==================== 文本输入 ==================== */

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    maxLines: Int = 4,
    maxLength: Int = 0,
    helper: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }

    SheetShell(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(text.trim()) },
        confirmText = "保存"
    ) {
        val onValue: (String) -> Unit = {
            if (maxLength <= 0 || it.length <= maxLength) text = it
        }
        if (maxLines <= 1) {
            // 单行输入：不能传 minLines/maxLines，否则 Compose 会因 maxLines < minLines 抛异常
            OutlinedTextField(
                value = text,
                onValueChange = onValue,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(label) }
            )
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = onValue,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = maxLines,
                label = { Text(label) }
            )
        }
        if (helper.isNotBlank() || maxLength > 0) {
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = helper,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (maxLength > 0) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${text.length}/$maxLength",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (text.length >= maxLength) WarnOrange
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/* ==================== 确认框 ==================== */

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    danger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    // 左侧按钮文案。默认「取消」；更新提示里用「忽略此次更新」。
    // 放在末尾是为了不影响已有的命名参数调用。
    dismissText: String = "取消"
) {
    SheetShell(
        title = title,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = confirmText,
        dismissText = dismissText
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (danger) WarnOrange else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ==================== 提示条 ==================== */

@Composable
fun InfoBanner(text: String, color: Color = DoneGreen) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
