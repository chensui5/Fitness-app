package com.chensui.lianji.data

/**
 * 内置食物营养库
 *
 * 所有数值均为「每 100 克」的参考值，来源为常见食物成分表的近似值，
 * 用于快速估算，实际烹饪方式会带来差异，用户可手动微调。
 */
object FoodLibrary {

    val items: List<FoodItem> = listOf(
        // ---------- 肉蛋类 ----------
        FoodItem(name = "鸡胸肉", calories = 133.0, protein = 24.6, carbs = 0.6, fat = 3.4, category = "肉蛋"),
        FoodItem(name = "鸡腿肉", calories = 181.0, protein = 16.0, carbs = 0.0, fat = 13.0, category = "肉蛋"),
        FoodItem(name = "鸡蛋", calories = 143.0, protein = 13.3, carbs = 2.8, fat = 8.8, category = "肉蛋"),
        FoodItem(name = "蛋清", calories = 48.0, protein = 11.6, carbs = 1.0, fat = 0.1, category = "肉蛋"),
        FoodItem(name = "蛋黄", calories = 328.0, protein = 15.2, carbs = 3.4, fat = 28.2, category = "肉蛋"),
        FoodItem(name = "瘦牛肉", calories = 125.0, protein = 19.9, carbs = 2.0, fat = 4.2, category = "肉蛋"),
        FoodItem(name = "牛腱子", calories = 106.0, protein = 20.3, carbs = 1.2, fat = 2.3, category = "肉蛋"),
        FoodItem(name = "猪里脊", calories = 155.0, protein = 20.2, carbs = 0.7, fat = 7.9, category = "肉蛋"),
        FoodItem(name = "三文鱼", calories = 208.0, protein = 17.2, carbs = 0.0, fat = 15.0, category = "肉蛋"),
        FoodItem(name = "鳕鱼", calories = 88.0, protein = 20.4, carbs = 0.5, fat = 0.5, category = "肉蛋"),
        FoodItem(name = "虾仁", calories = 93.0, protein = 18.6, carbs = 2.8, fat = 0.8, category = "肉蛋"),
        FoodItem(name = "金枪鱼罐头", calories = 116.0, protein = 24.0, carbs = 0.0, fat = 1.5, category = "肉蛋"),
        FoodItem(name = "瘦羊肉", calories = 118.0, protein = 20.5, carbs = 0.2, fat = 3.9, category = "肉蛋"),
        FoodItem(name = "鸭胸肉", calories = 138.0, protein = 15.0, carbs = 0.0, fat = 8.5, category = "肉蛋"),

        // ---------- 主食 ----------
        FoodItem(name = "白米饭", calories = 116.0, protein = 2.6, carbs = 25.9, fat = 0.3, category = "主食"),
        FoodItem(name = "糙米饭", calories = 112.0, protein = 2.7, carbs = 23.0, fat = 0.9, category = "主食"),
        FoodItem(name = "燕麦片", calories = 367.0, protein = 12.4, carbs = 61.0, fat = 6.7, category = "主食"),
        FoodItem(name = "全麦面包", calories = 246.0, protein = 9.0, carbs = 45.0, fat = 3.4, category = "主食"),
        FoodItem(name = "白吐司", calories = 282.0, protein = 8.3, carbs = 52.0, fat = 4.4, category = "主食"),
        FoodItem(name = "红薯", calories = 86.0, protein = 1.6, carbs = 20.1, fat = 0.1, category = "主食"),
        FoodItem(name = "紫薯", calories = 82.0, protein = 2.2, carbs = 18.0, fat = 0.2, category = "主食"),
        FoodItem(name = "土豆", calories = 77.0, protein = 2.0, carbs = 17.2, fat = 0.1, category = "主食"),
        FoodItem(name = "玉米", calories = 106.0, protein = 3.3, carbs = 22.8, fat = 1.2, category = "主食"),
        FoodItem(name = "意大利面", calories = 158.0, protein = 5.8, carbs = 30.9, fat = 0.9, category = "主食"),
        FoodItem(name = "荞麦面", calories = 99.0, protein = 3.6, carbs = 21.0, fat = 0.3, category = "主食"),
        FoodItem(name = "米粉", calories = 109.0, protein = 1.8, carbs = 25.0, fat = 0.2, category = "主食"),
        FoodItem(name = "馒头", calories = 221.0, protein = 7.0, carbs = 47.0, fat = 1.1, category = "主食"),
        FoodItem(name = "饺子(猪肉)", calories = 240.0, protein = 8.0, carbs = 29.0, fat = 9.5, category = "主食"),
        FoodItem(name = "小米粥", calories = 46.0, protein = 1.4, carbs = 8.4, fat = 0.7, category = "主食"),

        // ---------- 蔬菜 ----------
        FoodItem(name = "西兰花", calories = 34.0, protein = 2.8, carbs = 6.6, fat = 0.4, category = "蔬菜"),
        FoodItem(name = "菠菜", calories = 23.0, protein = 2.9, carbs = 3.6, fat = 0.4, category = "蔬菜"),
        FoodItem(name = "生菜", calories = 15.0, protein = 1.4, carbs = 2.9, fat = 0.2, category = "蔬菜"),
        FoodItem(name = "黄瓜", calories = 15.0, protein = 0.7, carbs = 3.6, fat = 0.1, category = "蔬菜"),
        FoodItem(name = "番茄", calories = 18.0, protein = 0.9, carbs = 3.9, fat = 0.2, category = "蔬菜"),
        FoodItem(name = "胡萝卜", calories = 41.0, protein = 0.9, carbs = 9.6, fat = 0.2, category = "蔬菜"),
        FoodItem(name = "白菜", calories = 17.0, protein = 1.5, carbs = 3.2, fat = 0.1, category = "蔬菜"),
        FoodItem(name = "青椒", calories = 20.0, protein = 1.0, carbs = 4.6, fat = 0.2, category = "蔬菜"),
        FoodItem(name = "芦笋", calories = 20.0, protein = 2.2, carbs = 3.9, fat = 0.1, category = "蔬菜"),
        FoodItem(name = "秋葵", calories = 33.0, protein = 2.0, carbs = 7.1, fat = 0.2, category = "蔬菜"),
        FoodItem(name = "蘑菇", calories = 22.0, protein = 3.1, carbs = 3.3, fat = 0.3, category = "蔬菜"),
        FoodItem(name = "洋葱", calories = 40.0, protein = 1.1, carbs = 9.3, fat = 0.1, category = "蔬菜"),
        FoodItem(name = "南瓜", calories = 26.0, protein = 1.0, carbs = 6.5, fat = 0.1, category = "蔬菜"),

        // ---------- 豆制品 ----------
        FoodItem(name = "北豆腐", calories = 98.0, protein = 12.2, carbs = 3.0, fat = 4.8, category = "豆制品"),
        FoodItem(name = "南豆腐", calories = 57.0, protein = 6.2, carbs = 2.4, fat = 2.5, category = "豆制品"),
        FoodItem(name = "豆浆", calories = 31.0, protein = 3.0, carbs = 1.2, fat = 1.6, category = "豆制品"),
        FoodItem(name = "毛豆", calories = 131.0, protein = 13.1, carbs = 10.5, fat = 5.0, category = "豆制品"),
        FoodItem(name = "鹰嘴豆", calories = 164.0, protein = 8.9, carbs = 27.4, fat = 2.6, category = "豆制品"),

        // ---------- 乳制品 ----------
        FoodItem(name = "纯牛奶", calories = 64.0, protein = 3.3, carbs = 4.8, fat = 3.6, category = "乳制品"),
        FoodItem(name = "低脂牛奶", calories = 45.0, protein = 3.4, carbs = 4.9, fat = 1.3, category = "乳制品"),
        FoodItem(name = "脱脂牛奶", calories = 34.0, protein = 3.4, carbs = 5.0, fat = 0.2, category = "乳制品"),
        FoodItem(name = "无糖希腊酸奶", calories = 59.0, protein = 10.0, carbs = 3.6, fat = 0.4, category = "乳制品"),
        FoodItem(name = "原味酸奶", calories = 72.0, protein = 2.5, carbs = 9.3, fat = 2.7, category = "乳制品"),
        FoodItem(name = "奶酪", calories = 328.0, protein = 25.7, carbs = 3.5, fat = 23.5, category = "乳制品"),

        // ---------- 水果 ----------
        FoodItem(name = "香蕉", calories = 93.0, protein = 1.4, carbs = 22.0, fat = 0.2, category = "水果"),
        FoodItem(name = "苹果", calories = 53.0, protein = 0.4, carbs = 13.5, fat = 0.2, category = "水果"),
        FoodItem(name = "蓝莓", calories = 57.0, protein = 0.7, carbs = 14.5, fat = 0.3, category = "水果"),
        FoodItem(name = "草莓", calories = 32.0, protein = 1.0, carbs = 7.1, fat = 0.2, category = "水果"),
        FoodItem(name = "橙子", calories = 48.0, protein = 0.8, carbs = 11.1, fat = 0.2, category = "水果"),
        FoodItem(name = "西瓜", calories = 31.0, protein = 0.5, carbs = 7.9, fat = 0.1, category = "水果"),
        FoodItem(name = "牛油果", calories = 171.0, protein = 2.0, carbs = 7.4, fat = 15.3, category = "水果"),
        FoodItem(name = "猕猴桃", calories = 61.0, protein = 0.8, carbs = 14.5, fat = 0.6, category = "水果"),
        FoodItem(name = "葡萄", calories = 45.0, protein = 0.5, carbs = 10.3, fat = 0.2, category = "水果"),

        // ---------- 坚果与油脂 ----------
        FoodItem(name = "杏仁", calories = 578.0, protein = 21.2, carbs = 21.7, fat = 49.4, category = "坚果"),
        FoodItem(name = "核桃", calories = 646.0, protein = 14.9, carbs = 13.7, fat = 65.2, category = "坚果"),
        FoodItem(name = "腰果", calories = 553.0, protein = 18.2, carbs = 30.2, fat = 43.9, category = "坚果"),
        FoodItem(name = "花生", calories = 574.0, protein = 25.8, carbs = 16.1, fat = 49.2, category = "坚果"),
        FoodItem(name = "花生酱", calories = 588.0, protein = 25.0, carbs = 20.0, fat = 50.0, category = "坚果"),
        FoodItem(name = "橄榄油", calories = 899.0, protein = 0.0, carbs = 0.0, fat = 99.9, category = "油脂"),

        // ---------- 补剂与饮品 ----------
        FoodItem(name = "乳清蛋白粉", calories = 380.0, protein = 80.0, carbs = 8.0, fat = 5.0, category = "补剂"),
        FoodItem(name = "黑咖啡", calories = 2.0, protein = 0.3, carbs = 0.0, fat = 0.0, category = "饮品"),
        FoodItem(name = "无糖可乐", calories = 0.4, protein = 0.0, carbs = 0.0, fat = 0.0, category = "饮品"),
        FoodItem(name = "运动饮料", calories = 26.0, protein = 0.0, carbs = 6.5, fat = 0.0, category = "饮品"),

        // ---------- 常见成品 ----------
        FoodItem(name = "煎鸡胸(少油)", calories = 165.0, protein = 24.0, carbs = 1.0, fat = 6.5, category = "常见成品"),
        FoodItem(name = "牛肉汉堡", calories = 254.0, protein = 13.0, carbs = 24.0, fat = 12.0, category = "常见成品"),
        FoodItem(name = "炸鸡块", calories = 296.0, protein = 17.0, carbs = 15.0, fat = 19.0, category = "常见成品"),
        FoodItem(name = "薯条", calories = 312.0, protein = 3.4, carbs = 41.0, fat = 15.0, category = "常见成品"),
        FoodItem(name = "方便面", calories = 472.0, protein = 9.5, carbs = 61.6, fat = 21.1, category = "常见成品"),
        FoodItem(name = "蛋炒饭", calories = 163.0, protein = 4.5, carbs = 24.5, fat = 5.2, category = "常见成品"),
        FoodItem(name = "小米粥(碗)", calories = 46.0, protein = 1.4, carbs = 8.4, fat = 0.7, category = "常见成品")
    )

    /** 按关键词搜索，返回匹配结果（名称包含关键词，短名优先） */
    fun search(keyword: String): List<FoodItem> {
        val k = keyword.trim()
        if (k.isEmpty()) return items
        return items
            .filter { it.name.contains(k, ignoreCase = true) }
            .sortedBy { it.name.length }
    }

    /** 精确查找同名食物 */
    fun findByName(name: String): FoodItem? =
        items.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    /** 首页展示的常用食物卡片 */
    val quickPicks: List<String> = listOf(
        "鸡胸肉", "鸡蛋", "白米饭", "燕麦片", "全麦面包",
        "香蕉", "纯牛奶", "西兰花", "红薯", "乳清蛋白粉"
    )

    val categories: List<String> = listOf(
        "肉蛋", "主食", "蔬菜", "豆制品", "乳制品", "水果", "坚果", "油脂", "补剂", "饮品", "常见成品"
    )
}
