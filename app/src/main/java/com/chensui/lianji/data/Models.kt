package com.chensui.lianji.data

import kotlinx.serialization.Serializable
import java.util.UUID

/* ============ 饮食 ============ */

/** 食物营养条目：营养值均以「每 100 克」为基准 */
@Serializable
data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Double,   // 千卡 / 100g
    val protein: Double,    // 蛋白质 g / 100g
    val carbs: Double,      // 碳水 g / 100g
    val fat: Double,        // 脂肪 g / 100g
    val category: String = "通用",
    val isCustom: Boolean = false
)

/** 一条实际的饮食记录 */
@Serializable
data class DietEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val grams: Double = 100.0,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val meal: String = "早餐"
) {
    val safeGrams: Double get() = if (grams <= 0.0) 1.0 else grams
}

object MealSlots {
    val ALL = listOf("早餐", "午餐", "晚餐", "加餐")
}

/* ============ 健身计划 ============ */

/** 一个训练动作 */
@Serializable
data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sets: Int = 3,
    val reps: String = "10 次",
    val note: String = ""
)

/** 一周中某一天的计划 */
@Serializable
data class DayPlan(
    val dayOfWeek: Int,                            // 1=周一 … 7=周日
    val isRestDay: Boolean = false,
    val exercises: List<Exercise> = emptyList()
)

/**
 * 某一周的计划实例。
 *
 * [weekKey] 是该周周一的日期（yyyy-MM-dd），作为这一周的唯一标识。
 * 计划按周存储而非循环模板——否则"周一练胸"会让人误以为历史上每个周一都安排了训练。
 */
@Serializable
data class WeekPlan(
    val weekKey: String = "",
    val days: List<DayPlan> = (1..7).map { DayPlan(it) },
    val createdAt: Long = System.currentTimeMillis()
) {
    fun dayOf(dayOfWeek: Int): DayPlan =
        days.firstOrNull { it.dayOfWeek == dayOfWeek } ?: DayPlan(dayOfWeek)
}

/* ============ 其他训练 ============ */

/** 其他训练类型（跑步、跳绳……），金币单价可单独设定并有 7 天冷却 */
@Serializable
data class OtherType(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unit: String,              // 公里 / 个 / 分钟
    val coinPerUnit: Int = 2,
    val lockedUntil: Long = 0L
)

/** 安排在某一周某一天的其他训练计划 */
@Serializable
data class OtherPlan(
    val id: String = UUID.randomUUID().toString(),
    val weekKey: String = "",          // 所属周（该周周一日期）；空值代表历史数据，迁移时归入本周
    val typeId: String,
    val typeName: String,
    val unit: String,
    val dayOfWeek: Int,
    val targetAmount: Double,
    val rewardPerUnit: Int
)

/* ============ 每日完成记录 ============ */

@Serializable
data class OtherDone(
    val planId: String,
    val typeName: String,
    val amount: Double,
    val unit: String,
    val coin: Int
)

@Serializable
data class DayRecord(
    val date: String,                                   // yyyy-MM-dd
    val doneExerciseIds: List<String> = emptyList(),
    val workoutChecked: Boolean = false,                // 健身打卡完成
    val workoutCoins: Int = 0,                          // 该次打卡实际发放的金币，用于精确回收
    val otherDone: List<OtherDone> = emptyList(),
    val otherChecked: Boolean = false,                  // 其他训练打卡完成
    val feeling: String = "",
    val coinsEarned: Int = 0                            // 该天累计净得（健身 + 其他训练）
)

/* ============ 商店 ============ */

@Serializable
data class ShopItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val imageUri: String? = null,
    val emoji: String = "\uD83C\uDF81",
    val price: Int = 10,
    val limitTotal: Int = 0,                 // 0 = 不限购
    val purchased: Int = 0,
    val isListed: Boolean = true,
    val listedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class PurchaseRecord(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val itemName: String,
    val price: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/* ============ 金币 ============ */

@Serializable
data class CoinLog(
    val id: String = UUID.randomUUID().toString(),
    val amount: Int,                 // 正数收入 / 负数支出
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

/* ============ 身体数据 ============ */

@Serializable
data class BodyRecord(
    val month: String,               // yyyy-MM
    val heightCm: Double,
    val weightKg: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

/* ============ 用户资料 ============ */

@Serializable
data class UserProfile(
    val nickname: String = "健身者",
    val motto: String = "每天进步一点点",
    val avatarUri: String? = null
)

/** 奖励配置，改动后有 7 天冷却 */
@Serializable
data class RewardConfig(
    val workoutCoin: Int = 10,
    val workoutLockedUntil: Long = 0L
)

/* ============ 应用程序总数据 ============ */

@Serializable
data class AppData(
    val profile: UserProfile = UserProfile(),
    val coins: Int = 0,
    val plans: Map<String, WeekPlan> = emptyMap(),   // key = 该周周一日期
    val otherTypes: List<OtherType> = Defaults.otherTypes(),
    val otherPlans: List<OtherPlan> = emptyList(),
    val records: Map<String, DayRecord> = emptyMap(),
    val diets: Map<String, List<DietEntry>> = emptyMap(),
    val shopItems: List<ShopItem> = emptyList(),
    val purchases: List<PurchaseRecord> = emptyList(),
    val coinLogs: List<CoinLog> = emptyList(),
    val bodyRecords: List<BodyRecord> = emptyList(),
    val rewardConfig: RewardConfig = RewardConfig(),
    val customFoods: List<FoodItem> = emptyList(),
    val themeMode: Int = 0,       // 0 跟随系统 / 1 浅色 / 2 深色
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true
)

object Defaults {
    fun otherTypes(): List<OtherType> = listOf(
        OtherType(name = "跑步", unit = "公里", coinPerUnit = 3),
        OtherType(name = "跳绳", unit = "个", coinPerUnit = 1),
        OtherType(name = "骑行", unit = "公里", coinPerUnit = 2),
        OtherType(name = "游泳", unit = "分钟", coinPerUnit = 1),
        OtherType(name = "快走", unit = "公里", coinPerUnit = 1)
    )
}
