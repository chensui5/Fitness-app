package com.chensui.lianji.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chensui.lianji.data.ShopItem
import com.chensui.lianji.data.Store
import com.chensui.lianji.ui.components.CoinBadge
import com.chensui.lianji.ui.components.EmptyHint
import com.chensui.lianji.ui.components.LocalImage
import com.chensui.lianji.ui.components.SectionCard
import com.chensui.lianji.ui.components.SectionTitle
import com.chensui.lianji.ui.components.StatusChip
import com.chensui.lianji.ui.components.ThinDivider
import com.chensui.lianji.ui.theme.CoinGold
import com.chensui.lianji.ui.theme.DoneGreen
import com.chensui.lianji.ui.theme.RestGray
import com.chensui.lianji.ui.theme.WarnOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShopScreen(modifier: Modifier = Modifier) {
    val data by Store.data.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ShopItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var redeemTarget by remember { mutableStateOf<ShopItem?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    val listed = data.shopItems.filter { it.isListed }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "商店",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "用金币兑换给自己准备的奖励",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CoinBadge(coins = data.coins)
            }
        }

        /* ---------- 可兑换商品 ---------- */
        item {
            SectionTitle(text = "可兑换", trailing = if (listed.isEmpty()) null else "${listed.size} 件")
        }

        if (listed.isEmpty()) {
            item {
                SectionCard {
                    EmptyHint("商店还空着，去下面上架一件奖励吧")
                }
            }
        } else {
            items(listed.size) { i ->
                val item = listed[i]
                ShopItemCard(
                    item = item,
                    coins = data.coins,
                    onRedeem = { redeemTarget = item }
                )
            }
        }

        /* ---------- 商品管理 ---------- */
        item {
            SectionCard {
                SectionTitle(text = "商品管理")
                Spacer(Modifier.height(10.dp))

                if (data.shopItems.isEmpty()) {
                    EmptyHint("还没有创建任何商品")
                } else {
                    data.shopItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    StatusChip(
                                        text = if (item.isListed) "已上架" else "已下架",
                                        color = if (item.isListed) DoneGreen else RestGray
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = buildString {
                                        append("${item.price} 金币")
                                        if (item.limitTotal > 0) append(" · 已兑 ${item.purchased}/${item.limitTotal}")
                                        else append(" · 已兑 ${item.purchased} 次")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { Store.toggleListing(item.id) }) {
                                Text(
                                    if (item.isListed) "下架" else "上架",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            TextButton(onClick = { editing = item }) {
                                Text("编辑", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        ThinDivider()
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { creating = true }) {
                    Text("+ 上架新商品", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        /* ---------- 兑换记录 ---------- */
        if (data.purchases.isNotEmpty()) {
            item {
                SectionCard {
                    SectionTitle(text = "兑换记录")
                    Spacer(Modifier.height(10.dp))
                    data.purchases.take(20).forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.itemName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(p.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "-${p.price}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarnOrange,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        ThinDivider()
                    }
                }
            }
        }
    }

    /* ---------- 弹窗 ---------- */
    if (creating || editing != null) {
        ShopItemEditorDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { item ->
                Store.saveShopItem(item)
                creating = false
                editing = null
            }
        )
    }

    redeemTarget?.let { item ->
        val err = when {
            data.coins < item.price -> "金币不足，还差 ${item.price - data.coins} 枚"
            item.limitTotal > 0 && item.purchased >= item.limitTotal -> "已达可兑换次数上限"
            else -> null
        }
        ConfirmDialog(
            title = "兑换「${item.name}」",
            message = buildString {
                append("将消耗 ${item.price} 金币。")
                if (item.description.isNotBlank()) append("\n\n${item.description}")
                if (err != null) append("\n\n$err")
            },
            confirmText = "确认兑换",
            danger = err != null,
            onDismiss = { redeemTarget = null },
            onConfirm = {
                val result = Store.redeem(item.id)
                toast = if (result == null) "兑换成功，去享受奖励吧" else result
                redeemTarget = null
            }
        )
    }

    toast?.let { msg ->
        ConfirmDialog(
            title = "提示",
            message = msg,
            confirmText = "知道了",
            onDismiss = { toast = null },
            onConfirm = { toast = null }
        )
    }
}

/* ==================== 商品卡片 ==================== */

@Composable
private fun ShopItemCard(
    item: ShopItem,
    coins: Int,
    onRedeem: () -> Unit
) {
    val affordable = coins >= item.price
    val soldOut = item.limitTotal > 0 && item.purchased >= item.limitTotal

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                LocalImage(
                    uri = item.imageUri,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                    fallback = {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(item.emoji, fontSize = 24.sp)
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${item.price} 金币",
                        style = MaterialTheme.typography.labelLarge,
                        color = CoinGold,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.limitTotal > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "剩 ${(item.limitTotal - item.purchased).coerceAtLeast(0)} 次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            TextButton(
                enabled = affordable && !soldOut,
                onClick = onRedeem
            ) {
                Text(
                    text = when {
                        soldOut -> "已兑完"
                        !affordable -> "金币不足"
                        else -> "兑换"
                    },
                    color = if (affordable && !soldOut) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/* ==================== 商品编辑 ==================== */

@Composable
private fun ShopItemEditorDialog(
    initial: ShopItem?,
    onDismiss: () -> Unit,
    onSave: (ShopItem) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "\uD83C\uDF81") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "20") }
    var limit by remember { mutableStateOf(initial?.limitTotal?.toString() ?: "0") }
    var imageUri by remember { mutableStateOf(initial?.imageUri) }
    var listed by remember { mutableStateOf(initial?.isListed ?: true) }

    val lockDays = initial?.let { Store.priceLockDays(it) } ?: 0
    val priceLocked = lockDays > 0
    val valid = name.isNotBlank() && (price.toIntOrNull() ?: -1) >= 0

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageUri = uri.toString()
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
                Text(
                    if (initial == null) "上架新商品" else "编辑商品",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(14.dp))

                /* 奖励图片 */
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).clickable {
                            pickImage.launch(arrayOf("image/*"))
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        LocalImage(
                            uri = imageUri,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                            fallback = {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("＋", style = MaterialTheme.typography.headlineSmall, color = RestGray)
                                    }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("奖励图片", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "点击左侧从相册选择，可留空",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (imageUri != null) {
                            TextButton(onClick = { imageUri = null }) {
                                Text("移除图片", color = WarnOrange, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = emoji, onValueChange = { emoji = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("无图片时显示的图标符号") }
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("奖励名称，如 一顿火锅") }
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 4,
                    label = { Text("奖励描述") }
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (!priceLocked) price = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !priceLocked,
                    label = { Text("消耗金币数量") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (priceLocked) {
                    Spacer(Modifier.height(6.dp))
                    InfoBanner("上架后 30 天内不可修改价格，还剩 $lockDays 天", WarnOrange)
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("可购买次数，0 表示不限") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("立即上架", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "下架后不会出现在可兑换列表",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = listed, onCheckedChange = { listed = it })
                }

                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (initial != null) {
                        TextButton(onClick = { Store.deleteShopItem(initial.id); onDismiss() }) {
                            Text("删除", color = WarnOrange)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        enabled = valid,
                        onClick = {
                            val now = System.currentTimeMillis()
                            val item = (initial ?: ShopItem(name = "", price = 0)).copy(
                                name = name.trim(),
                                description = desc.trim(),
                                emoji = emoji.ifBlank { "\uD83C\uDF81" },
                                price = if (priceLocked) initial!!.price else (price.toIntOrNull() ?: 0),
                                limitTotal = limit.toIntOrNull() ?: 0,
                                imageUri = imageUri,
                                isListed = listed,
                                listedAt = if (initial == null || (!initial.isListed && listed)) now else initial.listedAt
                            )
                            onSave(item)
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
