package com.chensui.lianji

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chensui.lianji.data.Store
import com.chensui.lianji.data.UpdateChecker
import com.chensui.lianji.ui.screens.ConfirmDialog
import com.chensui.lianji.ui.screens.DailyScreen
import com.chensui.lianji.ui.screens.DietScreen
import com.chensui.lianji.ui.screens.ProfileScreen
import com.chensui.lianji.ui.screens.ShopScreen
import com.chensui.lianji.ui.screens.WeekPlanScreen
import com.chensui.lianji.ui.theme.LianJiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(applicationContext)
        setContent {
            val data by Store.data.collectAsStateWithLifecycle()
            LianJiTheme(mode = data.themeMode) {
                AppRoot()
            }
        }
    }
}

/** tab 顺序：今日 → 饮食 → 计划 → 商店 → 我的 */
private val TAB_LABELS = listOf("今日", "饮食", "计划", "商店", "我的")

@Composable
private fun AppRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val data by Store.data.collectAsStateWithLifecycle()

    // 进入软件时静默检查一次更新。
    // 只有「确实有新版本」才提示；已是最新、没联网、请求失败一律保持安静，绝不打扰。
    var pendingUpdate by remember { mutableStateOf<UpdateChecker.UpdateResult.Newer?>(null) }

    LaunchedEffect(Unit) {
        if (!UpdateChecker.isNetworkAvailable(context)) return@LaunchedEffect
        val r = UpdateChecker.check(BuildConfig.VERSION_NAME)
        if (r is UpdateChecker.UpdateResult.Newer && r.version != data.ignoredUpdateVersion) {
            pendingUpdate = r
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                TAB_LABELS.forEachIndexed { index, label ->
                    TabItem(index, label, tab) { tab = it }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                0 -> DailyScreen()
                1 -> DietScreen()
                2 -> WeekPlanScreen()
                3 -> ShopScreen()
                else -> ProfileScreen()
            }
        }
    }

    pendingUpdate?.let { r ->
        ConfirmDialog(
            title = "发现新版本 ${r.version}",
            message = buildString {
                append("当前版本 v${BuildConfig.VERSION_NAME}")
                if (r.notes.isNotBlank()) append("\n\n${r.notes}")
            },
            confirmText = "更新",
            dismissText = "忽略此次更新",
            onDismiss = {
                // 记住这一版，之后不再重复打扰；出了更新的版本仍会提示
                Store.ignoreUpdateVersion(r.version)
                pendingUpdate = null
            },
            onConfirm = {
                pendingUpdate = null
                openUrl(context, r.url)
            }
        )
    }
}

/** 用系统浏览器打开链接；没有可用浏览器时静默忽略，不闪退 */
private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    index: Int,
    label: String,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val selected = index == selectedIndex
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(index) },
        icon = { TabGlyph(index = index, tint = tint) },
        label = { Text(text = label, fontSize = 11.sp, color = tint) },
        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
    )
}

/** 用最简单的几何形体表达五个页面，避免堆砌装饰 */
@Composable
private fun TabGlyph(index: Int, tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.9.dp.toPx()
        when (index) {
            0 -> {
                // 今日：圆圈 + 中心实点
                drawCircle(
                    color = tint,
                    radius = w / 2f - stroke,
                    style = Stroke(width = stroke)
                )
                drawCircle(color = tint, radius = w * 0.16f)
            }

            1 -> {
                // 饮食：碗身（下半圆）+ 碗口横线
                val bowlLeft = w * 0.16f
                val bowlTop = h * 0.24f
                val bowlW = w * 0.68f
                val bowlH = h * 0.36f
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(bowlLeft, bowlTop),
                    size = Size(bowlW, bowlH),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val rimY = bowlTop + bowlH / 2f
                drawLine(
                    color = tint,
                    start = Offset(w * 0.12f, rimY),
                    end = Offset(w * 0.88f, rimY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            2 -> {
                // 计划：三条等长横线
                val xs = w * 0.16f
                val xe = w * 0.84f
                listOf(0.26f, 0.5f, 0.74f).forEach { fy ->
                    drawLine(
                        color = tint,
                        start = Offset(xs, h * fy),
                        end = Offset(xe, h * fy),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            3 -> {
                // 商店：袋身 + 提手
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.34f),
                    size = Size(w * 0.68f, h * 0.50f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.33f, h * 0.12f),
                    size = Size(w * 0.34f, h * 0.34f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            else -> {
                // 我的：头 + 肩
                drawCircle(
                    color = tint,
                    radius = w * 0.19f,
                    center = Offset(w / 2f, h * 0.32f),
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.52f),
                    size = Size(w * 0.64f, h * 0.58f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}
