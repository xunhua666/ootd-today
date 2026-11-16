# 架构说明

本文档说明代码的组织方式、关键数据流和几个容易踩坑的构建细节。阅读顺序建议：先看「总览」，再按需跳到对应子系统。

## 总览

```
                    ┌─────────────────────────────────────────────┐
                    │                UI (Compose)                 │
                    │  today / wardrobe / add / camera / calendar │
                    │  stats / community / expert / settings      │
                    └───────────────┬─────────────────────────────┘
                                    │ StateFlow + 事件回调
                    ┌───────────────▼─────────────────────────────┐
                    │              ViewModel (Hilt)               │
                    │  状态编排、缓存、失败回退、SavedState 传参   │
                    └───────────────┬─────────────────────────────┘
                                    │
        ┌───────────────────────────┼────────────────────────────┐
        │                           │                            │
┌───────▼────────┐        ┌─────────▼─────────┐        ┌─────────▼─────────┐
│ domain (纯逻辑) │        │  data/repository  │        │   notification    │
│ 推荐/审美/版型  │◄───────┤ 衣橱/社区/天气/定位│        │ WorkManager 每日  │
│ 意图/文本解析   │        │ 云端与本地专家     │        │ 推荐推送          │
└───────┬────────┘        │ 云视觉             │        └─────────┬─────────┘
        │                 └─────────┬─────────┘                  │
        │                           │                            │
        │                 ┌─────────▼─────────┐                  │
        │                 │  data/local (Room)│                  │
        │                 │  DataStore        │                  │
        │                 │  Keystore 加密     │                  │
        │                 └───────────────────┘                  │
        └────────────────────────────────────────────────────────┘
```

设计原则：

1. **domain 不依赖 Android 类型**，因此推荐、审美、版型、解析等核心规则可以在 JVM 上直接单测。
2. **UI 只依赖仓库接口**，替换存储或服务商时不改 ViewModel / 页面。
3. **可选能力失败一律静默回退**：云专家、云视觉、定位、天气失败都不会阻塞主流程。

## 目录职责

| 目录 | 职责 |
|---|---|
| `data/model/` | 纯数据模型：`WardrobeItem`、`Outfit`、`OutfitLog`、`Weather`、`CalendarPlan`、`GarmentShape`、枚举（品类/场合/季节/风格/状态） |
| `data/local/` | Room 实体、DAO、迁移、`Converters`、`OutfitSnapshotCodec`、`DatabaseSeeder`、体验衣橱数据、模型下载存储 |
| `data/prefs/` | DataStore 设置（推送时间、专家配置、视觉配置、体验衣橱开关） |
| `data/crypto/` | `SecretStore`：Android Keystore AES/GCM，失败降级为 `plain:` 编码 |
| `data/repository/` | 仓库实现：衣橱、社区、天气（真实/离线/组合）、定位、云端专家、云视觉、本地专家运行时 |
| `domain/` | 推荐引擎、审美评分、版型规则、配色计算、社区文本解析、统计聚合、衣橱洞察 |
| `domain/stylist/` | 专家提示词、模型清单与完整性校验、建议解析、追问意图归一化 |
| `domain/vision/` | 视觉接口请求构造、响应解析、保守置信度策略 |
| `ui/` | Compose 页面、可复用组件、主题、导航、ViewModel |
| `notification/` | 每日推荐 Worker、通知渠道、调度器 |
| `di/` | Hilt 模块（数据库、DAO、仓库绑定、应用级 CoroutineScope） |

## 数据层

### Room

数据库 `ootd_today.db`，当前 **version 5**，6 张表：

| 表 | 说明 |
|---|---|
| `wardrobe_items` | 衣橱单品；v5 新增 `shapeJson`（版型） |
| `outfit_logs` | 打卡记录；搭配以 JSON 快照存储，避免为读记录再联表 |
| `community_users` / `posts` / `comments` | 本地示例社区 |
| `calendar_plans` | 日程；v4 新增，含单品快照 |

迁移链 `1→2→3→4→5` 全部为增量迁移（`AppDatabase.kt`），schema 导出到 `app/schemas/` 并随仓库提交，设备测试会校验迁移。**不要**用 destructive migration 替换，用户衣橱与打卡是真实数据。

体验衣橱使用**负数 id**（`WardrobeItem.isDemo`），只存在于内存，永不写库、不计入个人统计。用户首次保存真实单品时自动关闭体验模式。

### 设置与密钥

- 轻量设置走 DataStore（`ootd_settings`）。
- API Key 写入前经 `SecretStore.encrypt`：AES/GCM/NoPadding，密钥由 Android Keystore 保管；部分模拟器/定制 ROM 的 Keystore 不可用时降级为 `plain:` + Base64，仅应用私有目录可读。
- 本地模型状态存在 `local_models` SharedPreferences，模型文件在 `getExternalFilesDir()/expert_models/`，并从自动备份中排除（见 `res/xml/backup_rules.xml`）。

## 推荐引擎

`domain/OutfitRecommendEngine.kt`，两步打分：

**第一步：单品分（0..1）**

| 维度 | 权重 | 说明 |
|---|---|---|
| 保暖度匹配 | 0.34 | 由气温推导目标保暖度 1..5，非对称惩罚（穿少比穿多更严重） |
| 季节匹配 | 0.14 | 相邻季节仍有部分分 |
| 闲置唤醒 | 0.12 | 对数衰减，避免「一次没穿过」获得压倒性权重 |
| 近期去重 | 0.10 | 近 7 天穿过降权到 0.35，不归零 |
| 场合通用性 | 0.30 | 适用场合越多略占优；场合本身是**硬过滤** |

**第二步：组合分**

- 主体（上装+下装，或连体装）+ 鞋，冷天追加外套；枚举所有候选组合而非「各取最优」。
- 组合分 = 单品均分 × 0.65 + 整体审美 × 0.35。
- 包与配饰在主体确定后挑选，且要求加入后整体审美仍然协调；配饰额外受保暖度硬约束（防止热天推围巾）。

**审美分**（`OutfitAesthetics`）：配色 0.6（色相跨度分段 + 色系数量纪律）+ 风格 0.4，若上下装都有已确认版型，再与 `SilhouetteRules` 的版型分按 85:15 融合。

**推荐理由**与评分同源：配色描述来自 `OutfitAesthetics.describe`，版型描述来自 `SilhouetteRules.describe`。

## 专家子系统

```
StylistService
  ├─ StylistPromptBuilder  → 系统提示词 + 衣橱上下文（裁剪到 1400 字符）
  ├─ StylistProvider（接口）
  │    └─ OpenAiStylistProvider  → OpenAI 兼容 /chat/completions
  ├─ StylistAdviceParser   → 只接受发送给模型的那批单品 id，过滤编造项
  └─ LocalExpertRuntime    → LiteRT-LM Engine + Conversation（本地离线）

StylistIntent → 把口语追问归一化为明确指令（不消耗网络请求）
```

- 云端失败（无 Key、断网、限流、格式错误）统一返回 `null`，调用方回退本地算法，不向用户报错。
- 本地专家每次请求创建并释放 Engine（CPU 4 线程、3072 上下文、384 输出 token、关闭 thinking），要求 64 位 ABI 且可用内存 > 1.5 GB。
- 模型清单固定 revision + 文件大小 + SHA-256（`domain/stylist/LocalModel.kt`），下载/导入都必须校验通过才可用。

## 视觉子系统

流程：`CameraX / 相册 → 用户逐次确认上传 → 缩放至最长边 1280 → 纠正 EXIF 旋转 → 重新编码 JPEG（去除原始 EXIF）→ 云视觉 → 保守解析 → 用户逐项确认 → 写入衣橱`。

- `VisionProtocol` 定义严格 JSON 契约；证据不足必须填 `UNKNOWN`，低置信度直接丢弃版型而不是猜。
- 只允许 `BOTTOM` 填 `rise`/`cut`，`ONE_PIECE` 的 `cut` 只能是 `A_LINE` 等规则由 `GarmentShape.forCategory` 兜底。
- 未确认的版型不参与评分；`GarmentShape.confirmed` 是唯一开关。

## 天气与定位

- 天气：Open-Meteo 当前天气 + 逐日预报；失败时 `ResilientWeatherProvider` 回退到离线示例（`isLive=false`），UI 会标注「模拟」。
- 定位：系统 `LocationManager` 粗定位，不依赖 Google Play 服务；拿不到就用默认城市，不阻塞推荐。

## UI 与主题

- 单 Activity + Navigation Compose；4 个底部 Tab（今日/衣橱/日历/我的）+ 全屏流程（录入、详情、相机、发布、设置、专家）。
- 主题是**自定义撞色品牌色板**，刻意关闭 Material You 动态取色；深色模式使用单独设计的深色渐变以保证白字对比度。
- 页面级状态在 ViewModel 中用 `StateFlow` 暴露；跨页传参用 `SavedStateHandle`（相机结果、聚焦单品、待筛选话题）。

## 构建细节（容易踩坑）

- **AGP 9 内置 Kotlin，不可使用 kapt**，全部注解处理走 KSP。
- 根 `build.gradle.kts` 通过 `buildscript` 固定 Kotlin 插件版本为 2.3.21；`app/build.gradle.kts` 里额外把 `kotlin-stdlib` 锁定到同一版本，否则传递依赖会抬到 2.4.x，导致「metadata 2.4.0 超出 2.3 编译器可读范围」。
- 没有 `kotlinOptions {}` DSL（AGP 9 已移除），`jvmTarget` 跟随 `compileOptions.targetCompatibility`（Java 11）。
- Room schema 使用 KSP 参数导出到 `app/schemas`，并作为 androidTest assets 挂载。
- Release 使用 AGP 9 的 `optimization { enable = true }`，keep 规则在 `app/src/main/keepRules/`。
- 构建要求 JDK 21；Gradle wrapper 为 9.5.0。
- **签名**：`app/build.gradle.kts` 从项目根目录的 `keystore.properties` 读取 upload key（`storeFile` 相对项目根解析）；文件不存在时 release 回退 debug 签名，仅用于本机验证。上架流程见 [RELEASING.md](RELEASING.md)。
- **发布产物**：Play 上传 `.aab`（`./gradlew :app:bundleRelease`），本机验证用 `.apk`（`assembleRelease`）。
- **16 KB 页面大小**：`scripts/check_16kb.py` 解析 APK/AAB 内 64 位 `.so` 的 ELF `PT_LOAD` 对齐，CI 每次推送都会校验；`zipalign -P 16` 只检查 zip 偏移，不能替代它。
- **网络传输**：`HttpJson` 只允许 HTTPS，仅本机回环地址例外（便于 `adb reverse` 调试）；这条约束有单元测试保护。

## 测试策略

| 层 | 方式 |
|---|---|
| domain 纯逻辑 | JVM 单元测试（推荐、审美、版型、配色、意图、解析、编解码、日期） |
| 数据层 | Room 迁移仪器测试 + 快照编解码单测 |
| 可选能力 | 视觉协议 fixture 单测、同意流程 UI 测试、本地专家运行时测试 |
| UI | Compose UI 测试覆盖日程拖拽/增删与视觉同意 |

新增逻辑时优先把它写成不依赖 Android 类型的纯函数，这样测试成本最低。

## 扩展点

- **换天气源**：实现 `WeatherProvider`，改 `DataModule` 的绑定。
- **换社区后端**：实现 `CommunityRepository`，UI 与 ViewModel 不动。
- **换专家服务商**：在设置里改 baseUrl / 模型即可，无需改代码（OpenAI 兼容格式）。
- **接正式视觉服务**：替换设置中的视觉接口地址/模型；产品化时应增加服务端代理而不是让用户填 Key。
- **加本地模型**：在 `LocalModel.ALL` 中登记 id、revision、大小与 SHA-256。