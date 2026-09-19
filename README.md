# 练迹 (LianJi)

一个完全免费的健身打卡 App：训练计划、饮食记录、每日打卡。

> A completely free and open-source fitness tracking app — daily workout plans, diet logging and daily check-ins.

---

## 下载

最新版本：**[Releases](https://github.com/chensui5/Fitness-app/releases/latest)**

- 支持 Android 8.0（API 26）及以上
- 下载 APK 直接安装，首次需在系统设置中允许「安装未知来源应用」

## 功能

### 每日计划

- 当天的动作卡片点击即完成，全部完成自动打卡并发放金币
- 休息日不计入打卡目标，也不能记录动作
- 打卡后可以写下当天的训练感受，提醒下次如何调整

### 饮食记录

- 三餐与加餐分区记录
- 内置 80 余种常见食物的营养数据，点卡片自动带出热量、蛋白质、碳水、脂肪
- 营养明细按类别配色，另有三大营养素供能占比，一眼看出今天蛋白够不够

### 每周计划

- **按周独立制定**，不做永久循环模板；支持「延续上一周」与「导入」
- **从文本导入**：粘贴训练计划文本，自动识别星期、动作与组次，预览确认后写入指定周
- 动作支持长按左侧手柄上下拖动排序，点击动作即可修改，不必删除后重加
- 数量单位固定为「次 / 秒 / 分钟」三选一，只接受大于 0 的整数
- 只有昨天及以后的日期可以制定或修改计划，更早的日期只读

### 其他训练

- 跑步、跳绳、骑行、游泳等，可自行增删类型
- 与健身计划相互独立：奖励独立计算，只安排其他训练的日子不会发放健身奖励

### 金币与商店

- 商店商品的奖励名称、图片、描述、价格与限购次数均可自定义
- 上架后价格锁定 30 天
- 金币流水可追溯；删除计划、改为休息日等操作会自动扣回已发放的奖励（余额可为负）

### 个人主页

- 头像（从相册选择）、昵称、激励文案
- 身体数据记录：身高体重与锻炼建议，月底提醒更新，历史记录只读
- 日历打卡视图、深色 / 浅色 / 跟随系统
- 数据导出为 JSON 备份
- 检查更新（也可在打开 App 时自动检查）

## 从源码构建

### 环境要求

| 项 | 版本 |
|---|---|
| JDK | 21 |
| Android SDK | platform 36 + build-tools 36 |
| Gradle | 8.13（仓库自带 wrapper，无需另装） |

### 步骤

```bash
git clone https://github.com/chensui5/Fitness-app.git
cd Fitness-app
```

在项目根目录新建 `local.properties`，指向你自己的 Android SDK：

```properties
sdk.dir=C:/Users/<你的用户名>/AppData/Local/Android/Sdk
```

然后构建：

```bash
./gradlew assembleDebug        # Linux / macOS
gradlew.bat assembleDebug      # Windows
```

产物位于 `app/build/outputs/apk/debug/`。

也可以直接用 Android Studio 打开项目，`local.properties` 会自动生成。

### 关于 release 签名

`keystore/` 不入库。`app/build.gradle.kts` 的规则是：
**存在 `keystore/lianji.jks` 时使用正式签名，否则回退到 debug 签名** ——
所以 clone 之后 `assembleRelease` 同样可以正常构建，只是使用 debug 证书。

## 技术说明

- Kotlin + Jetpack Compose（Material 3），单 Activity
- 数据全部保存在本机（SharedPreferences + kotlinx.serialization 存 JSON），**没有服务端、不上传任何数据**
- 除「检查更新」会读取本仓库的 Releases 之外，App 不发起任何网络请求
- 视觉上刻意去除阴影、渐变与立体效果，只用字重和留白分层

## 许可证

[MIT](LICENSE)
