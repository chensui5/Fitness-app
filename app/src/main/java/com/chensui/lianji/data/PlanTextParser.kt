package com.chensui.lianji.data

import java.util.UUID

/**
 * 粘贴文本导入计划 —— 解析器。
 *
 * 设计要点：**本解析器不认识任何健身动作。** 它只切结构：
 * `## 周一：胸+三头` → 星期；`1. 杠铃卧推 4×8` → 名称 + 组数 + 次数。
 * 名称是「组次标记之前的全部文本」，原样保留、不做校验，
 * 因此冷门动作、自造名、英文名、错别字都能进来。
 * 决定成功率的是**排版规整度**，不是动作的知名度。
 */
object PlanTextParser {

    /* ---------------- 词表 ---------------- */

    private val DOW = mapOf(
        '一' to 1, '二' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '日' to 7, '天' to 7
    )

    /** 其他训练的运动词（可枚举，所以用词表；动作名不可枚举，所以不用词表） */
    private val SPORTS = listOf(
        "快走", "慢跑", "跑步机", "跑步", "骑行", "动感单车", "单车", "游泳", "跳绳",
        "爬楼", "椭圆机", "划船机", "有氧", "徒步", "爬山", "瑜伽", "拉伸", "开合跳"
    )

    private val EVERYDAY_KEYS = listOf("每次训练后", "每次练后", "训练结束后", "训练后", "每日", "每天")
    private val REST_KEYS = listOf("休息安排", "休息日", "休息计划", "休息")

    /* ---------------- 正则 ---------------- */

    // 注意：井号必须用捕获组包住，否则标题会占据组 1、组 2 不存在
    private val HEAD = Regex("""^\s*(#{1,6})\s*(.*)$""")
    private val DAY_RANGE = Regex(
        """(?:周|星期|礼拜)([一二三四五六日天])\s*[-~—－至到]\s*(?:周|星期|礼拜)?([一二三四五六日天])"""
    )
    private val DAY_TOKEN = Regex("""(?:周|星期|礼拜)([一二三四五六日天])""")
    private val SETS_REPS = Regex(
        """(\d+)\s*[×xX*✕]\s*(\d+)\s*(?:[-~—－至到]\s*(\d+))?\s*(次|秒|分钟|min|MIN)?"""
    )
    private val LIST_MARK = Regex("""^\s*(?:\d+\s*[.、)）]|[-*•·])\s*""")
    private val PAREN = Regex("""[（(]([^）)]*)[）)]""")
    private val SPORT_AMOUNT = Regex(
        """(\d+(?:\.\d+)?)\s*(?:[-~—－至到]\s*(\d+(?:\.\d+)?))?\s*(公里|千米|km|KM|米|个|分钟|分|小时|min|MIN)"""
    )

    /* ---------------- 结果模型 ---------------- */

    /** 导入预览中的一天 */
    data class ParsedDay(
        val dayOfWeek: Int,                                        // 1=周一 … 7=周日
        var title: String = "",
        var isRest: Boolean = false,
        val exercises: MutableList<Exercise> = mutableListOf(),
        val others: MutableList<ParsedOther> = mutableListOf()
    ) {
        fun isEmpty(): Boolean = !isRest && exercises.isEmpty() && others.isEmpty()
    }

    /** 导入预览中的一条其他训练 */
    data class ParsedOther(
        val typeName: String,
        val amount: Double,
        val unit: String,
        val fromRange: Boolean = false        // 原文是区间，预览里标黄
    )

    /** 未能识别的行 */
    data class SkippedLine(val text: String, val reason: String)

    data class ParseResult(
        val days: List<ParsedDay>,
        val skipped: List<SkippedLine>,
        val hints: List<String>
    )

    /* ---------------- 主流程 ---------------- */

    private enum class Section { None, Day, EveryDay, Rest, Unknown }

    fun parse(text: String): ParseResult {
        val days = linkedMapOf<Int, ParsedDay>()
        val skipped = mutableListOf<SkippedLine>()
        val hints = mutableListOf<String>()
        val everyDay = mutableListOf<Exercise>()

        fun day(dow: Int): ParsedDay = days.getOrPut(dow) { ParsedDay(dow) }

        var section = Section.None
        var dow = 0

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (isSeparator(line)) continue

            /* ---------- 1. 标题行 ---------- */
            val headMatch = HEAD.find(line)
            if (headMatch != null || isDayHeading(line) || isSpecialHeading(line)) {
                val title = (headMatch?.groupValues?.get(2) ?: line).trim()
                val level = headMatch?.groupValues?.get(1)?.length ?: 0
                val d = lastDayToken(title)
                when {
                    // 带星期的标题优先判定，这样「## 周日：休息」能正确落到周日
                    d != 0 -> {
                        section = Section.Day; dow = d
                        day(d).title = titleAfterColon(title)
                        if (REST_KEYS.any { title.contains(it) }) day(d).isRest = true
                    }
                    EVERYDAY_KEYS.any { title.contains(it) } -> {
                        section = Section.EveryDay; dow = 0
                    }
                    REST_KEYS.any { title.contains(it) } -> {
                        section = Section.Rest; dow = 0
                    }
                    else -> {
                        section = Section.Unknown; dow = 0
                        // 一级标题当作整篇文档的标题（如「# 新手三分化训练计划」），静默忽略
                        if (level != 1 && title.isNotEmpty()) {
                            skipped += SkippedLine(line, "判断不出属于哪一天")
                        }
                    }
                }
                continue
            }

            /* ---------- 2. 内容行 ---------- */
            when (section) {
                Section.Day -> {
                    val parsed = parseExercises(line)
                    if (parsed.isEmpty()) {
                        if (looksLikeOther(line)) {
                            day(dow).others += parseOther(line)!!
                        } else {
                            skipped += SkippedLine(line, "没看出动作名称 + 组次")
                        }
                    } else {
                        day(dow).exercises += parsed
                    }
                }

                Section.EveryDay -> {
                    val parsed = parseExercises(line)
                    if (parsed.isEmpty()) {
                        skipped += SkippedLine(line, "没看出动作名称 + 组次")
                    } else {
                        everyDay += parsed
                    }
                }

                Section.Rest -> {
                    val target = daysInLine(line)
                    if (target.isEmpty()) {
                        skipped += SkippedLine(line, "休息安排里没写清是哪几天")
                    } else {
                        target.forEach { day(it).isRest = true }
                        if (looksLikeOther(line)) {
                            val other = parseOther(line)
                            if (other != null) target.forEach { day(it).others += other }
                        }
                    }
                }

                else -> skipped += SkippedLine(line, "这行没有所属的星期标题")
            }
        }

        /* ---------- 3. 「每次训练后」追加到每个训练日 ---------- */
        if (everyDay.isNotEmpty()) {
            val trainingDays = days.values.filter { !it.isRest && (it.exercises.isNotEmpty() || it.others.isNotEmpty()) }
            trainingDays.forEach { d ->
                d.exercises += everyDay.map { it.copy(id = UUID.randomUUID().toString()) }
            }
            hints += "「每次训练后」的 ${everyDay.size} 个动作已追加到 ${trainingDays.size} 个训练日"
        }

        return ParseResult(
            days = days.values.filter { !it.isEmpty() }.toList(),
            skipped = skipped,
            hints = hints
        )
    }

    /* ---------------- 练习行 ---------------- */

    /** 一行可能含多个动作（用「或」「、」并列），返回全部解析结果 */
    private fun parseExercises(line: String): List<Exercise> {
        val body = line.replace(LIST_MARK, "").trim()
        if (body.isEmpty()) return emptyList()

        val parts = splitParallel(body)
        val out = mutableListOf<Exercise>()
        for (p in parts) {
            parseOneExercise(p)?.let { out += it }
        }
        return out
    }

    private fun parseOneExercise(part: String): Exercise? {
        val m = SETS_REPS.find(part) ?: return null
        val before = part.substring(0, m.range.first).trim()
        var tail = part.substring(m.range.last + 1).trim()

        // 「核心训练：死虫式」这类分类前缀 → 归入备注，名称只留动作本身
        var prefix = ""
        val colon = before.lastIndexOfAny(charArrayOf('：', ':'))
        val name = if (colon >= 0) {
            prefix = before.substring(0, colon).trim()
            before.substring(colon + 1).trim()
        } else before

        if (name.isBlank()) return null

        val sets = m.groupValues[1].toIntOrNull() ?: return null
        val low = m.groupValues[2].toIntOrNull() ?: return null
        val high = m.groupValues[3].toIntOrNull()
        val unitRaw = m.groupValues[4]
        val unit = when {
            unitRaw.contains("秒") -> "秒"
            unitRaw.contains("分") || unitRaw.equals("min", ignoreCase = true) -> "分钟"
            else -> "次"
        }

        val notes = mutableListOf<String>()
        if (prefix.isNotEmpty()) notes += prefix

        // 括注（如「倾角≤20°」）→ 备注
        PAREN.findAll(tail).forEach { g -> notes += g.groupValues[1].trim() }
        tail = PAREN.replace(tail, " ").trim().trimStart('/', '／', '，', ',', '、', '-', ' ').trim()
        if (tail.isNotEmpty()) notes += normalizeTail(tail)

        val isRange = high != null && high != low
        val reps: String
        if (isRange) {
            // 区间不猜：数量留空，原文写进备注，由用户在预览里补
            reps = ""
            notes += "原文 $low-$high $unit"
        } else {
            reps = "$low $unit"
        }

        return Exercise(
            id = UUID.randomUUID().toString(),
            name = name,
            sets = sets,
            reps = reps,
            note = notes.filter { it.isNotBlank() }.joinToString("；")
        )
    }

    /** 把「/侧」「每侧」这类归一成好读的说法 */
    private fun normalizeTail(tail: String): String = when {
        tail.contains("侧") -> "每侧"
        tail.contains("力竭") -> "力竭"
        tail.contains("递减") -> "递减组"
        else -> tail
    }

    /** 并列的动作拆分：`死虫式 3×15 或 平板支撑 3×30秒` → 两条 */
    private fun splitParallel(body: String): List<String> {
        val seps = listOf(" 或者 ", " 或 ", "或者", "或", "、", " / ")
        for (s in seps) {
            if (!body.contains(s)) continue
            val parts = body.split(s).map { it.trim() }.filter { it.isNotEmpty() }
            // 只有当每一段都能独立解析出组次时才拆，避免把动作名里的「或」误切
            if (parts.size >= 2 && parts.all { SETS_REPS.containsMatchIn(it) }) return parts
        }
        return listOf(body)
    }

    /* ---------------- 其他训练 ---------------- */

    private fun looksLikeOther(line: String): Boolean {
        val hasSport = SPORTS.any { line.contains(it) }
        val hasAmount = SPORT_AMOUNT.containsMatchIn(line)
        return hasSport && hasAmount
    }

    private fun parseOther(line: String): ParsedOther? {
        val sport = SPORTS.firstOrNull { line.contains(it) } ?: return null
        val m = SPORT_AMOUNT.find(line) ?: return null
        val low = m.groupValues[1].toDoubleOrNull() ?: return null
        val high = m.groupValues[2].toDoubleOrNull()
        val rawUnit = m.groupValues[3]
        val unit = when (rawUnit) {
            "千米", "km", "KM" -> "公里"
            "分" -> "分钟"
            "min", "MIN" -> "分钟"
            else -> rawUnit
        }
        val isRange = high != null && high > low
        // 其他训练的目标量不允许为空，区间取下限（预览里标黄提示）
        return ParsedOther(
            typeName = sport,
            amount = low,
            unit = unit,
            fromRange = isRange
        )
    }

    /* ---------------- 行分类工具 ---------------- */

    private fun isSeparator(line: String): Boolean =
        line.all { it == '-' || it == '=' || it == '*' || it == '_' || it == ' ' } && line.isNotBlank() && line.length >= 3

    private fun isSpecialHeading(line: String): Boolean {
        val t = line.replace(LIST_MARK, "").trim()
        if (t.isEmpty()) return false
        val isMarkdown = HEAD.containsMatchIn(line)
        // 只有 markdown 标题、或「很短且没有冒号/组次」的独立行才算章节标题。
        // 否则「周四-周日：休息/低强度有氧（快走20-30分钟）」这种带内容的一行
        // 会因为含「休息」二字被误判成标题，把整行内容吞掉。
        val looksLikeTitle = isMarkdown ||
            (t.length <= 12 && !t.contains('：') && !t.contains(':') && !SETS_REPS.containsMatchIn(t))
        if (!looksLikeTitle) return false
        return EVERYDAY_KEYS.any { t.contains(it) } || REST_KEYS.any { t.contains(it) }
    }

    /** 以「周X」开头、且不像动作行 → 当作标题 */
    private fun isDayHeading(line: String): Boolean {
        // 含日期区间的行是内容而不是标题，例如「周四-周日：休息/低强度有氧（快走20-30分钟）」
        if (DAY_RANGE.containsMatchIn(line)) return false
        if (!DAY_TOKEN.containsMatchIn(line.take(8))) return false
        if (!line.trimStart().startsWithAny("周", "星期", "礼拜")) return false
        return line.contains('：') || line.contains(':') || !SETS_REPS.containsMatchIn(line)
    }

    private fun String.startsWithAny(vararg keys: String): Boolean = keys.any { startsWith(it) }

    private fun lastDayToken(text: String): Int {
        val all = DAY_TOKEN.findAll(text).toList()
        if (all.isEmpty()) return 0
        return DOW[all.last().groupValues[1].first()] ?: 0
    }

    /** 休息安排里出现的所有天（含区间） */
    private fun daysInLine(line: String): List<Int> {
        val out = linkedSetOf<Int>()
        DAY_RANGE.findAll(line).forEach { m ->
            val a = DOW[m.groupValues[1].first()] ?: return@forEach
            val b = DOW[m.groupValues[2].first()] ?: return@forEach
            if (a <= b) (a..b).forEach { out += it } else (a..7).forEach { out += it }
        }
        if (out.isEmpty()) {
            DAY_TOKEN.findAll(line).forEach { m ->
                DOW[m.groupValues[1].first()]?.let { out += it }
            }
        }
        return out.toList()
    }

    private fun titleAfterColon(title: String): String {
        val i = title.lastIndexOfAny(charArrayOf('：', ':'))
        return if (i >= 0) title.substring(i + 1).trim() else ""
    }
}
