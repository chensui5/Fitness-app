package com.chensui.lianji.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/* ============================ 日期工具 ============================ */

object Dates {
    private val dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFmt = DateTimeFormatter.ofPattern("yyyy-MM")

    fun now(): LocalDate = LocalDate.now()
    fun key(date: LocalDate): String = date.format(dayFmt)
    fun todayKey(): String = key(now())
    fun monthKey(ym: YearMonth): String = ym.format(monthFmt)
    fun parse(s: String): LocalDate? = runCatching { LocalDate.parse(s, dayFmt) }.getOrNull()
    fun parseMonth(s: String): YearMonth? = runCatching { YearMonth.parse(s, monthFmt) }.getOrNull()

    /** 1 = 周一 … 7 = 周日 */
    fun dow(date: LocalDate): Int = date.dayOfWeek.value

    fun weekLabel(dow: Int): String =
        listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").getOrElse(dow - 1) { "" }

    fun pretty(date: LocalDate): String = "${date.monthValue} 月 ${date.dayOfMonth} 日"

    /** 该日期所在周的周一 */
    fun mondayOf(date: LocalDate): LocalDate =
        date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

    /** 周标识：所在周周一的日期，形如 2026-09-14 */
    fun weekKeyOf(date: LocalDate): String = key(mondayOf(date))
}

/* ============================ 全局数据仓库 ============================ */

object Store {

    private const val PREF_NAME = "lianji_store_v1"
    private const val KEY_DATA = "app_data"
    private const val DAY_MS = 24L * 60 * 60 * 1000

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private var prefs: android.content.SharedPreferences? = null

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> get() = _data.asStateFlow()

    val current: AppData get() = _data.value

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
        val raw = prefs?.getString(KEY_DATA, null)
        _data.value = if (raw.isNullOrBlank()) {
            AppData()
        } else {
            runCatching { json.decodeFromString<AppData>(raw) }.getOrElse { AppData() }
        }
        autoRecordMonthIfNeeded()
        migrateLegacyTemplate(raw)
        migrateOrphanOtherPlans()
    }

    /** 旧版其他训练没有 weekKey（为空串），统一归入本周，避免升级后凭空消失 */
    private fun migrateOrphanOtherPlans() {
        val d = current
        if (d.otherPlans.none { it.weekKey.isBlank() }) return
        val wk = Dates.weekKeyOf(Dates.now())
        mutate { dd ->
            dd.copy(otherPlans = dd.otherPlans.map {
                if (it.weekKey.isBlank()) it.copy(weekKey = wk) else it
            })
        }
    }

    /**
     * 旧版本把周计划存成一张循环模板（`AppData.weekPlan`）。
     * 改为按周存储后，若还没有任何周计划，就把这套模板落到「本周」，
     * 避免用户升级后要重新录一遍。动作会重新分配 id。
     */
    private fun migrateLegacyTemplate(raw: String?) {
        if (raw.isNullOrBlank() || current.plans.isNotEmpty()) return
        runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val legacy = root["weekPlan"]?.jsonObject ?: return@runCatching
            val daysJson = legacy["days"]?.jsonArray ?: return@runCatching

            val parsed = daysJson.mapNotNull { el ->
                runCatching {
                    val obj = el.jsonObject
                    val dow = obj["dayOfWeek"]?.jsonPrimitive?.content?.toIntOrNull()
                        ?: return@mapNotNull null
                    val rest = obj["isRestDay"]?.jsonPrimitive?.content == "true"
                    val exs = obj["exercises"]?.jsonArray?.mapNotNull { exEl ->
                        runCatching {
                            val ex = exEl.jsonObject
                            Exercise(
                                id = java.util.UUID.randomUUID().toString(),
                                name = ex["name"]?.jsonPrimitive?.content.orEmpty(),
                                sets = ex["sets"]?.jsonPrimitive?.content?.toIntOrNull() ?: 3,
                                reps = ex["reps"]?.jsonPrimitive?.content ?: "10 次",
                                note = ex["note"]?.jsonPrimitive?.content.orEmpty()
                            )
                        }.getOrNull()
                    }.orEmpty()
                    DayPlan(dow, rest, exs)
                }.getOrNull()
            }

            // 模板里一个动作都没有就没什么可迁移的
            if (parsed.none { it.exercises.isNotEmpty() }) return@runCatching

            val weekKey = Dates.weekKeyOf(Dates.now())
            mutate { d ->
                val base = (1..7).map { dow -> parsed.firstOrNull { it.dayOfWeek == dow } ?: DayPlan(dow) }
                d.copy(plans = d.plans + (weekKey to WeekPlan(weekKey, base)))
            }
        }
    }

    private fun save() {
        runCatching {
            prefs?.edit()?.putString(KEY_DATA, json.encodeToString(_data.value))?.apply()
        }
    }

    private fun mutate(block: (AppData) -> AppData) {
        _data.value = block(_data.value)
        save()
    }

    fun exportJson(): String = runCatching { json.encodeToString(_data.value) }
        .getOrElse { "{}" }

    /* ---------------- 用户资料 ---------------- */

    fun updateProfile(profile: UserProfile) = mutate { it.copy(profile = profile) }

    fun setThemeMode(mode: Int) = mutate { it.copy(themeMode = mode) }

    fun setSoundEnabled(on: Boolean) = mutate { it.copy(soundEnabled = on) }

    fun setHapticEnabled(on: Boolean) = mutate { it.copy(hapticEnabled = on) }

    /** 记住用户点过「忽略此次更新」的版本，同一版本不再弹提示 */
    fun ignoreUpdateVersion(version: String) = mutate { it.copy(ignoredUpdateVersion = version) }

    /* ---------------- 金币 ---------------- */

    fun addCoins(amount: Int, reason: String) = mutate { d ->
        d.copy(
            coins = d.coins + amount,   // 不加 0 下限：余额可能处于负数状态
            coinLogs = (listOf(CoinLog(amount = amount, reason = reason)) + d.coinLogs).take(300)
        )
    }

    /* ---------------- 每周健身计划（按周存储） ---------------- */

    fun currentWeekKey(): String = Dates.weekKeyOf(Dates.now())

    fun weekPlanOf(weekKey: String): WeekPlan? = current.plans[weekKey]

    fun hasPlan(weekKey: String): Boolean = current.plans.containsKey(weekKey)

    /** 该日期所属周是否已制定计划 */
    fun hasPlanForDate(date: LocalDate): Boolean =
        current.plans.containsKey(Dates.weekKeyOf(date))

    /**
     * 保存某一周中某一天的计划。
     * 若该周尚无计划，会顺带把这一周创建出来。
     */
    fun saveDayPlan(
        weekKey: String,
        dayOfWeek: Int,
        restDay: Boolean,
        exercises: List<Exercise>
    ) = mutate { d ->
        // 早于昨天的日期已成历史，不可再制定或修改
        dateOf(weekKey, dayOfWeek)?.let { if (!canEditPlanOn(it)) return@mutate d }
        val base = d.plans[weekKey]?.days ?: (1..7).map { DayPlan(it) }
        val days = base.map { day ->
            if (day.dayOfWeek == dayOfWeek) {
                DayPlan(dayOfWeek, restDay, if (restDay) emptyList() else exercises)
            } else day
        }
        val updated = WeekPlan(
            weekKey = weekKey,
            days = days.sortedBy { it.dayOfWeek },
            createdAt = d.plans[weekKey]?.createdAt ?: System.currentTimeMillis()
        )
        var nd = d.copy(plans = d.plans + (weekKey to updated))
        // 计划改动后这天可能不再满足打卡条件（改成休息日 / 新增了没做的动作）→ 撤销并退款
        Dates.parse(weekKey)?.let { monday ->
            nd = reconcileWorkout(nd, Dates.key(monday.plusDays((dayOfWeek - 1).toLong())))
        }
        nd
    }

    /**
     * 整周删除。
     * 该周所有已打卡的日期都会重新核算并回收金币（可能把余额扣成负数）。
     */
    fun clearWeek(weekKey: String) = mutate { d ->
        if (!canEditWeek(weekKey)) return@mutate d      // 整周已成历史，不可清空
        var nd = d.copy(plans = d.plans - weekKey)
        Dates.parse(weekKey)?.let { monday ->
            for (i in 0 until 7) {
                nd = reconcileWorkout(nd, Dates.key(monday.plusDays(i.toLong())))
            }
        }
        reclaimOrphanOtherRewards(nd)
    }

    /* ---------------- 计划编辑边界（禁止回填历史） ---------------- */

    /**
     * 计划可编辑的最早日期：昨天（含）。
     * 更早的日期视为既成历史，不可再新增或修改计划，避免通过回填过去刷奖励。
     */
    fun planEditFloor(): LocalDate = Dates.now().minusDays(1)

    /** 该日期是否还能制定 / 修改计划 */
    fun canEditPlanOn(date: LocalDate): Boolean = !date.isBefore(planEditFloor())

    /** 该周是否还有可编辑的日期（周级操作：延续、清空、新增其他训练） */
    fun canEditWeek(weekKey: String): Boolean {
        val monday = Dates.mondayOf(Dates.parse(weekKey) ?: return false)
        return canEditPlanOn(monday.plusDays(6))
    }

    /** 该周内尚可编辑的星期几（1..7），用于限制日期选择范围 */
    fun editableDaysIn(weekKey: String): List<Int> {
        val monday = Dates.mondayOf(Dates.parse(weekKey) ?: return emptyList())
        return (1..7).filter { canEditPlanOn(monday.plusDays((it - 1).toLong())) }
    }

    /** 某周某天对应的日期 */
    fun dateOf(weekKey: String, dayOfWeek: Int): LocalDate? =
        Dates.parse(weekKey)?.let { Dates.mondayOf(it).plusDays((dayOfWeek - 1).toLong()) }

    /** 复制同一周内某天的计划到其它几天 */
    fun copyDayPlan(weekKey: String, fromDay: Int, toDays: List<Int>) = mutate { d ->
        val src = d.plans[weekKey]?.dayOf(fromDay) ?: return@mutate d
        val base = d.plans[weekKey]?.days ?: (1..7).map { DayPlan(it) }
        val days = base.map { day ->
            if (day.dayOfWeek in toDays) {
                DayPlan(
                    day.dayOfWeek,
                    src.isRestDay,
                    src.exercises.map { it.copy(id = java.util.UUID.randomUUID().toString()) }
                )
            } else day
        }
        d.copy(plans = d.plans + (weekKey to WeekPlan(weekKey, days.sortedBy { it.dayOfWeek })))
    }

    /**
     * 延续计划：把 [fromWeekKey] 那一周的安排整体复制到 [toWeekKey]。
     * 动作会重新分配 id，避免与源周的完成记录互相干扰。
     */
    fun carryOverWeek(fromWeekKey: String, toWeekKey: String): Boolean {
        if (!canEditWeek(toWeekKey)) return false       // 目标周已成历史，不允许回填
        val src = current.plans[fromWeekKey] ?: return false
        mutate { d ->
            val days = src.days.map { day ->
                DayPlan(
                    dayOfWeek = day.dayOfWeek,
                    isRestDay = day.isRestDay,
                    exercises = day.exercises.map { it.copy(id = java.util.UUID.randomUUID().toString()) }
                )
            }
            var nd = d.copy(plans = d.plans + (toWeekKey to WeekPlan(weekKey = toWeekKey, days = days)))
            // 目标周原本若已有安排，被覆盖后原打卡可能失效，需要重算
            Dates.parse(toWeekKey)?.let { monday ->
                for (i in 0 until 7) {
                    nd = reconcileWorkout(nd, Dates.key(monday.plusDays(i.toLong())))
                }
            }
            nd
        }
        return true
    }

    /** 找到 [weekKey] 之前最近一个已制定计划的周，用于「延续上一周」 */
    fun latestPlanBefore(weekKey: String): String? {
        val start = Dates.parse(weekKey) ?: return null
        var cursor = start.minusWeeks(1)
        var guard = 0
        while (guard < 260) {
            val k = Dates.key(cursor)
            if (current.plans.containsKey(k)) return k
            cursor = cursor.minusWeeks(1)
            guard++
        }
        return null
    }

    fun planOf(date: LocalDate): DayPlan {
        val stored = current.plans[Dates.weekKeyOf(date)] ?: return DayPlan(Dates.dow(date))
        return stored.dayOf(Dates.dow(date))
    }

    fun isRestDay(date: LocalDate): Boolean = planOf(date).isRestDay

    /** 该日期是否有健身打卡目标（所属周必须先有计划） */
    fun hasWorkoutTarget(date: LocalDate): Boolean {
        val stored = current.plans[Dates.weekKeyOf(date)] ?: return false
        val p = stored.dayOf(Dates.dow(date))
        return !p.isRestDay && p.exercises.isNotEmpty()
    }

    /* ---------------- 其他训练 ---------------- */

    fun saveOtherType(type: OtherType) = mutate { d ->
        val exists = d.otherTypes.any { it.id == type.id }
        val list = if (exists) d.otherTypes.map { if (it.id == type.id) type else it }
        else d.otherTypes + type
        d.copy(otherTypes = list)
    }

    fun deleteOtherType(id: String) = mutate { d ->
        reclaimOrphanOtherRewards(
            d.copy(
                otherTypes = d.otherTypes.filterNot { it.id == id },
                otherPlans = d.otherPlans.filterNot { it.typeId == id }
            )
        )
    }

    fun saveOtherPlan(plan: OtherPlan) = mutate { d ->
        val exists = d.otherPlans.any { it.id == plan.id }
        val list = if (exists) d.otherPlans.map { if (it.id == plan.id) plan else it }
        else d.otherPlans + plan
        d.copy(otherPlans = list)
    }

    fun deleteOtherPlan(id: String) = mutate { d ->
        // 删掉计划的同时，回收这条计划已经发出去的金币
        reclaimOrphanOtherRewards(d.copy(otherPlans = d.otherPlans.filterNot { it.id == id }))
    }

    /** 某一周安排的全部其他训练 */
    fun otherPlansInWeek(weekKey: String): List<OtherPlan> =
        current.otherPlans.filter { it.weekKey == weekKey }.sortedBy { it.dayOfWeek }

    /** 某一天安排的其他训练（同样按周存储，不会跨周自动循环） */
    fun otherPlansOf(date: LocalDate): List<OtherPlan> =
        current.otherPlans.filter {
            it.weekKey == Dates.weekKeyOf(date) && it.dayOfWeek == Dates.dow(date)
        }

    /* ---------------- 每日记录 ---------------- */

    fun recordOf(dateKey: String): DayRecord = current.records[dateKey] ?: DayRecord(dateKey)

    fun todayRecord(): DayRecord = recordOf(Dates.todayKey())

    fun saveFeeling(dateKey: String, text: String) = mutate { d ->
        val rec = d.records[dateKey] ?: DayRecord(dateKey)
        d.copy(records = d.records + (dateKey to rec.copy(feeling = text)))
    }

    /* ---------------- 奖励回收（计划变动时联动） ---------------- */

    /**
     * 统一的金币增减入口。
     * 注意：这里**不做 0 下限保护**——奖励被回收时如果金币已经花掉，就如实变成负数。
     */
    private fun applyCoinDelta(d: AppData, delta: Int, reason: String): AppData =
        d.copy(
            coins = d.coins + delta,
            coinLogs = (listOf(CoinLog(amount = delta, reason = reason)) + d.coinLogs).take(300)
        )

    /**
     * 撤销某天的打卡并扣回金币。
     * 优先按当初实际发放的 [DayRecord.workoutCoins] 扣；老数据没记这个字段时退回当前配置值。
     */
    private fun revokeWorkout(d: AppData, dateKey: String, reason: String): AppData {
        val rec = d.records[dateKey] ?: return d
        if (!rec.workoutChecked) return d

        val back = if (rec.workoutCoins != 0) rec.workoutCoins else d.rewardConfig.workoutCoin
        val nd = applyCoinDelta(d, -back, "$reason · $dateKey")
        return nd.copy(
            records = nd.records + (dateKey to rec.copy(
                workoutChecked = false,
                workoutCoins = 0,
                coinsEarned = rec.coinsEarned - back
            ))
        )
    }

    /**
     * 重新核算某天是否仍满足打卡条件：所属周有计划、不是休息日、动作全部完成。
     * 只要有一条不成立就撤销打卡并退款。删除计划 / 清空整周 / 改成休息日 / 新增未完成的动作
     * 都会走到这里。
     */
    private fun reconcileWorkout(d: AppData, dateKey: String): AppData {
        val rec = d.records[dateKey] ?: return d
        if (!rec.workoutChecked) return d

        val date = Dates.parse(dateKey) ?: return d
        val plan = d.plans[Dates.weekKeyOf(date)]?.dayOf(Dates.dow(date))
        val qualified = plan != null &&
                !plan.isRestDay &&
                plan.exercises.isNotEmpty() &&
                plan.exercises.all { it.id in rec.doneExerciseIds }

        return if (qualified) d else revokeWorkout(d, dateKey, "计划变动，回收打卡奖励")
    }

    /** 回收某天某条其他训练记录的金币 */
    private fun revokeOtherDone(d: AppData, dateKey: String, planId: String, reason: String): AppData {
        val rec = d.records[dateKey] ?: return d
        val item = rec.otherDone.firstOrNull { it.planId == planId } ?: return d

        val remaining = rec.otherDone.filterNot { it.planId == planId }
        val date = Dates.parse(dateKey)
        val dayPlans = if (date != null) {
            val wk = Dates.weekKeyOf(date)
            d.otherPlans.filter { it.weekKey == wk && it.dayOfWeek == Dates.dow(date) }
        } else emptyList()
        val allDone = dayPlans.isNotEmpty() && dayPlans.all { p -> remaining.any { it.planId == p.id } }

        val nd = applyCoinDelta(d, -item.coin, "$reason · ${item.typeName}")
        return nd.copy(
            records = nd.records + (dateKey to rec.copy(
                otherDone = remaining,
                otherChecked = allDone,
                coinsEarned = rec.coinsEarned - item.coin
            ))
        )
    }

    /**
     * 扫描全部记录，回收「对应计划已不存在」的其他训练奖励。
     * 删除单项计划、清空整周、删除训练类型都走这里。
     */
    private fun reclaimOrphanOtherRewards(d: AppData): AppData {
        val aliveIds = d.otherPlans.map { it.id }.toSet()
        val orphans = d.records.entries.flatMap { (dateKey, rec) ->
            rec.otherDone.filter { it.planId !in aliveIds }.map { dateKey to it.planId }
        }
        if (orphans.isEmpty()) return d

        var nd = d
        orphans.forEach { (dateKey, planId) ->
            nd = revokeOtherDone(nd, dateKey, planId, "计划已删除，回收奖励")
        }
        return nd
    }

    /**
     * 勾选 / 取消勾选一个训练动作。
     * 当天动作全部完成时自动打卡并发放金币；取消后打卡状态与金币一并回退。
     */
    fun toggleExercise(dateKey: String, exerciseId: String) = mutate { d ->
        val date = Dates.parse(dateKey) ?: return@mutate d
        val stored = d.plans[Dates.weekKeyOf(date)] ?: return@mutate d   // 该周尚未制定计划
        val plan = stored.dayOf(Dates.dow(date))
        if (plan.isRestDay) return@mutate d          // 休息日不可记录动作

        val rec = d.records[dateKey] ?: DayRecord(dateKey)
        val done = rec.doneExerciseIds.toMutableList()
        if (done.contains(exerciseId)) done.remove(exerciseId) else done.add(exerciseId)

        val qualified = plan.exercises.isNotEmpty() && plan.exercises.all { it.id in done }

        // 先把勾选结果落盘，再做奖励结算
        var nd = d.copy(records = d.records + (dateKey to rec.copy(doneExerciseIds = done)))

        if (qualified && !rec.workoutChecked) {
            val reward = nd.rewardConfig.workoutCoin
            val cur = nd.records[dateKey] ?: rec
            nd = applyCoinDelta(nd, reward, "训练打卡 · $dateKey")
            nd = nd.copy(
                records = nd.records + (dateKey to cur.copy(
                    workoutChecked = true,
                    workoutCoins = reward,
                    coinsEarned = cur.coinsEarned + reward
                ))
            )
        } else if (!qualified && rec.workoutChecked) {
            nd = revokeWorkout(nd, dateKey, "撤销打卡")
        }

        nd
    }

    /** 记录一项其他训练的完成量，按量即时发放金币 */
    fun completeOther(dateKey: String, planId: String, amount: Double) = mutate { d ->
        val plan = d.otherPlans.firstOrNull { it.id == planId } ?: return@mutate d
        val date = Dates.parse(dateKey) ?: return@mutate d
        val rec = d.records[dateKey] ?: DayRecord(dateKey)

        val old = rec.otherDone.firstOrNull { it.planId == planId }
        val newCoin = (amount * plan.rewardPerUnit).toInt().coerceAtLeast(0)
        val delta = newCoin - (old?.coin ?: 0)

        val done = rec.otherDone.filterNot { it.planId == planId } +
                OtherDone(planId = planId, typeName = plan.typeName, amount = amount, unit = plan.unit, coin = newCoin)

        val weekKey = Dates.weekKeyOf(date)
        val dayPlans = d.otherPlans.filter {
            it.weekKey == weekKey && it.dayOfWeek == Dates.dow(date)
        }
        val allDone = dayPlans.isNotEmpty() && dayPlans.all { p -> done.any { it.planId == p.id } }

        d.copy(
            coins = d.coins + delta,
            records = d.records + (dateKey to rec.copy(
                otherDone = done,
                otherChecked = allDone,
                coinsEarned = rec.coinsEarned + delta
            )),
            coinLogs = if (delta != 0) {
                (listOf(CoinLog(amount = delta, reason = "其他训练 · ${plan.typeName}")) + d.coinLogs).take(300)
            } else d.coinLogs
        )
    }

    /** 撤销某项其他训练的完成记录，并扣回已发放的金币（可扣成负数） */
    fun undoOther(dateKey: String, planId: String) = mutate { d ->
        revokeOtherDone(d, dateKey, planId, "撤销其他训练")
    }

    /* ---------------- 饮食 ---------------- */

    fun dietOf(dateKey: String): List<DietEntry> = current.diets[dateKey] ?: emptyList()

    fun addDiet(dateKey: String, entry: DietEntry) = mutate { d ->
        val list = (d.diets[dateKey] ?: emptyList()) + entry
        d.copy(diets = d.diets + (dateKey to list))
    }

    fun removeDiet(dateKey: String, entryId: String) = mutate { d ->
        val list = (d.diets[dateKey] ?: emptyList()).filterNot { it.id == entryId }
        d.copy(diets = d.diets + (dateKey to list))
    }

    fun addCustomFood(food: FoodItem) = mutate { d ->
        d.copy(customFoods = d.customFoods + food.copy(isCustom = true))
    }

    fun removeCustomFood(id: String) = mutate { d ->
        d.copy(customFoods = d.customFoods.filterNot { it.id == id })
    }

    /* ---------------- 商店 ---------------- */

    fun saveShopItem(item: ShopItem) = mutate { d ->
        val exists = d.shopItems.any { it.id == item.id }
        val list = if (exists) d.shopItems.map { if (it.id == item.id) item else it }
        else d.shopItems + item
        d.copy(shopItems = list)
    }

    fun deleteShopItem(id: String) = mutate { d ->
        d.copy(shopItems = d.shopItems.filterNot { it.id == id })
    }

    fun toggleListing(id: String) = mutate { d ->
        d.copy(shopItems = d.shopItems.map {
            if (it.id == id) it.copy(isListed = !it.isListed, listedAt = System.currentTimeMillis()) else it
        })
    }

    /** 兑换商品，返回 null 表示成功，否则返回失败原因 */
    fun redeem(itemId: String): String? {
        val d = current
        val item = d.shopItems.firstOrNull { it.id == itemId } ?: return "商品不存在"
        if (!item.isListed) return "商品已下架"
        if (item.limitTotal > 0 && item.purchased >= item.limitTotal) return "已达可兑换次数上限"
        if (d.coins < item.price) return "金币不足，还差 ${item.price - d.coins} 枚"

        mutate { dd ->
            val target = dd.shopItems.first { it.id == itemId }
            dd.copy(
                coins = dd.coins - target.price,
                shopItems = dd.shopItems.map {
                    if (it.id == itemId) it.copy(purchased = it.purchased + 1) else it
                },
                purchases = (listOf(
                    PurchaseRecord(itemId = target.id, itemName = target.name, price = target.price)
                ) + dd.purchases).take(300),
                coinLogs = (listOf(
                    CoinLog(amount = -target.price, reason = "兑换「${target.name}」")
                ) + dd.coinLogs).take(300)
            )
        }
        return null
    }

    /** 价格锁剩余天数（上架后 30 天内不可改价），0 表示可改 */
    fun priceLockDays(item: ShopItem): Int {
        if (!item.isListed) return 0
        val unlockAt = item.listedAt + 30L * DAY_MS
        val remain = unlockAt - System.currentTimeMillis()
        return if (remain <= 0) 0 else ((remain + DAY_MS - 1) / DAY_MS).toInt()
    }

    /* ---------------- 奖励设定 ---------------- */

    fun setWorkoutCoin(coin: Int) = mutate { d ->
        d.copy(rewardConfig = d.rewardConfig.copy(
            workoutCoin = coin,
            workoutLockedUntil = System.currentTimeMillis() + 7L * DAY_MS
        ))
    }

    fun setOtherCoinPerUnit(typeId: String, coin: Int) = mutate { d ->
        d.copy(otherTypes = d.otherTypes.map {
            if (it.id == typeId) it.copy(
                coinPerUnit = coin,
                lockedUntil = System.currentTimeMillis() + 7L * DAY_MS
            ) else it
        })
    }

    fun daysLeft(until: Long): Int {
        val remain = until - System.currentTimeMillis()
        if (remain <= 0) return 0
        return ((remain + DAY_MS - 1) / DAY_MS).toInt()
    }

    /* ---------------- 身体数据 ---------------- */

    /** 可编辑的月份：上个月始终可补录；本月仅最后三天可改 */
    fun editableMonths(): Set<String> {
        val today = Dates.now()
        val cm = YearMonth.from(today)
        val set = mutableSetOf(cm.minusMonths(1).toString())
        if (today.dayOfMonth > today.lengthOfMonth() - 3) set.add(cm.toString())
        return set
    }

    /**
     * 该月份的身体数据是否可编辑。
     * 规则：每月最后三天 + 次月（即上个月）可改当月数据。
     * 例外：**从未记录过任何数据时，当月随时可建立第一条** ——
     * 否则新用户得干等到月底才能填身高体重。
     */
    fun canEditMonth(month: String): Boolean {
        if (isFirstBodyRecord(month)) return true
        return month in editableMonths()
    }

    /** 是否属于「首次填写」（用于按钮文案区分「填写 / 修改」） */
    fun isFirstBodyRecord(month: String): Boolean =
        current.bodyRecords.isEmpty() && month == YearMonth.from(Dates.now()).toString()

    fun isInReminderWindow(): Boolean {
        val today = Dates.now()
        return today.dayOfMonth > today.lengthOfMonth() - 3
    }

    fun bodyRecordOf(month: String): BodyRecord? =
        current.bodyRecords.firstOrNull { it.month == month }

    fun latestBodyRecord(): BodyRecord? =
        current.bodyRecords.maxByOrNull { it.month }

    /**
     * 进入月底三天窗口时，若本月尚无记录，则自动沿用最近一次身体数据落档，
     * 保证历史记录连续；用户可在同一窗口内再作修改。
     */
    fun autoRecordMonthIfNeeded() {
        if (!isInReminderWindow()) return
        val ym = YearMonth.from(Dates.now()).toString()
        if (bodyRecordOf(ym) != null) return
        val last = latestBodyRecord() ?: return
        saveBodyRecord(
            BodyRecord(month = ym, heightCm = last.heightCm, weightKg = last.weightKg)
        )
    }

    fun saveBodyRecord(record: BodyRecord) = mutate { d ->
        // 兜底：不在可编辑窗口内直接拒绝写入（界面已拦，这里防止其它调用路径绕过）
        if (!canEditMonth(record.month)) return@mutate d
        val exists = d.bodyRecords.any { it.month == record.month }
        val list = if (exists) d.bodyRecords.map { if (it.month == record.month) record else it }
        else (d.bodyRecords + record).sortedBy { it.month }
        d.copy(bodyRecords = list)
    }

    /* ---------------- 统计 ---------------- */

    /** 某月打卡情况：已完成天数 to 目标天数 */
    fun monthStats(ym: YearMonth): Pair<Int, Int> {
        val d = current
        var target = 0
        var done = 0
        for (day in 1..ym.lengthOfMonth()) {
            val date = ym.atDay(day)
            val stored = d.plans[Dates.weekKeyOf(date)] ?: continue   // 该周没制定计划，不计入
            val plan = stored.dayOf(Dates.dow(date))
            val hasTarget = !plan.isRestDay && plan.exercises.isNotEmpty()
            if (hasTarget) {
                target++
                if (d.records[Dates.key(date)]?.workoutChecked == true) done++
            }
        }
        return done to target
    }

    fun totalCheckIns(): Int = current.records.values.count { it.workoutChecked }

    fun streakDays(): Int {
        val d = current
        var streak = 0
        var cursor = Dates.now()
        var guard = 0
        while (guard < 400) {
            val plan = d.plans[Dates.weekKeyOf(cursor)]?.dayOf(Dates.dow(cursor))
            val hasTarget = plan != null && !plan.isRestDay && plan.exercises.isNotEmpty()
            val checked = d.records[Dates.key(cursor)]?.workoutChecked == true
            if (hasTarget && checked) {
                streak++
            } else if (hasTarget && !checked) {
                // 今天还没打卡不算断签
                if (cursor != Dates.now()) break
            }
            cursor = cursor.minusDays(1)
            guard++
        }
        return streak
    }
}
