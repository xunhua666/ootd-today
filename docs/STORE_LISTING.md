# Google Play 商店素材与文案

本文件是提交 Play Console 时的文案底稿。中文版本供参考，实际海外上架以英文为主。

## 1. 基本信息

| 字段 | 内容 |
|---|---|
| 应用名称（≤30） | `OOTD Today: Outfit Planner` |
| 包名 | `com.ootd.today`（不可更改） |
| 分类 | Lifestyle |
| 标签 | fashion, wardrobe, outfit planner, closet organizer |
| 联系邮箱 | `<替换为你的邮箱>`（Play 必填） |
| 隐私政策 URL | `https://xunhua666.github.io/ootd-today/privacy.html`（需先在仓库 Settings → Pages 启用 `/docs`） |
| 默认语言 | English (United States) |
| 内容分级 | 通过 IARC 问卷生成；目标人群 18+ |

## 2. 英文文案

### 简短描述（≤80 字符）

```
Plan outfits from your own wardrobe — weather-aware, private, no ads.
```

### 完整描述（≤4000 字符）

```
OOTD Today turns the clothes you already own into a wardrobe you can actually use.

Add the pieces you wear, tell the app what the weather is like, and get a complete outfit — top, bottom, shoes and
optional layers — chosen from your closet. Log what you wore, plan outfits for interviews or trips in the calendar,
and see the real cost per wear of every item.

WHY IT IS DIFFERENT

• Your clothes first. Recommendations only use items you own. When a complete outfit is not possible, the app tells
  you what is missing instead of showing you a shopping list.
• Explainable picks. Every suggestion comes with a short reason: temperature match, colour harmony, silhouette and
  the occasion it was chosen for.
• Local-first and private. Wardrobe, outfits, calendar and logs stay on your phone. No account, no ads, no analytics.
• Optional AI. Connect your own OpenAI-compatible service, or download a small on-device model for offline advice.
  AI features are optional; the core app works without them.
• Built for daily use. A daily reminder, a month calendar, and cost-per-wear statistics help you wear more of what
  you already own.

WHAT YOU CAN DO

• Add garments with category, colour, warmth, season, occasion and style; auto-fill category and colour from a photo.
• Get weather-aware outfit recommendations and shuffle for alternatives.
• Log today's outfit with one tap and track how often each piece is worn.
• Plan upcoming outfits in a calendar with date, time, occasion, location and notes.
• See cost per wear and get suggestions for pieces that deserve another chance.
• Optionally analyse a garment photo for fit, length, waist rise, cut and shoulder shape — you confirm every tag
  before it is saved.

PRIVACY

No account. No tracking. No ads. Wardrobe data stays on your device. Network requests use HTTPS. If you enable an
optional cloud AI feature, only the data you explicitly confirm is sent to the provider you configured. Read the full
policy: https://xunhua666.github.io/ootd-today/privacy.html

ABOUT

OOTD Today is an open-source project. Source code: https://github.com/xunhua666/ootd-today
AI-generated content may be inaccurate; please use your own judgement.
```

### 中文参考文案

- 简短描述：`把已有的衣服变成每天能用的衣橱：天气感知推荐、穿搭日历、穿着成本统计。`
- 完整描述：围绕「已有衣服优先、建议可解释、本地优先」「可选 AI 专家」「日程与成本统计」展开，与英文版一致。

## 3. 图形素材

| 素材 | 规格 | 说明 |
|---|---|---|
| 应用图标 | 512×512 PNG，无透明 | 从现有矢量图标导出；深色背景 + 浅色 T 恤图形 |
| Feature graphic | 1024×500 PNG/JPG | 建议：左侧 OOTD Today 字标 + 右侧手机界面截图叠放 |
| 手机截图 | 2–8 张，16:9 或 9:16 | 需**英文界面**重新截图：今日、衣橱、录入、日历、统计、专家、版型识别 |
| 平板截图 | 可选 | 当前未做平板适配，可不提供 |

### 截图拍摄清单（英文界面）

1. Today：天气 + 推荐搭配 + 推荐理由
2. Wardrobe：分类筛选 + 单品卡片
3. Add item：拍照/相册 + 品类与颜色预填
4. Calendar：月视图 + 日程
5. Stats：穿着次数与单次成本
6. Expert：本地/云端专家对话
7. Shape：版型识别结果确认
8. Dark mode：今日页深色模式

## 4. 内容分级（IARC 问卷参考）

| 问题 | 回答 |
|---|---|
| 暴力、血腥、性内容、脏话、毒品 | 无 |
| 用户间交流 / 陌生人社交 | 无（社区为本地示例内容，不上传、不互通） |
| 用户生成内容公开展示 | 无 |
| 与 AI 交互 | 有（AI 对话，已内置披露与举报入口） |
| 广告 | 无 |
| 内购 | 无（当前版本） |
| 位置共享 | 粗略位置仅用于天气查询，不公开 |

## 5. App access（审核说明，必填）

```
No login or account is required. All core features — wardrobe, outfit recommendations, calendar and statistics —
work offline and without any credentials.

Two optional AI features require user-provided configuration and are not needed to review the app:
1) Cloud outfit advice: users enter their own OpenAI-compatible endpoint and API key in Settings.
2) On-device advice: users download a ~977 MB model from the expert screen.

Reviewers can verify the core experience without any key. The community tab contains demo content only.
```

## 6. 提交前检查

- [ ] 英文界面完整（当前进行中，见 [I18N_PROGRESS.md](I18N_PROGRESS.md)）
- [ ] 隐私政策 URL 可公开访问（GitHub Pages 已启用）
- [ ] 512 图标 / 1024×500 feature graphic / 英文截图
- [ ] Data Safety 表单按 [DATA_SAFETY.md](DATA_SAFETY.md) 填写
- [ ] 内容分级问卷完成
- [ ] 目标人群设为 18+（非儿童向）
- [ ] 应用内举报入口可用（专家页「报告不当内容」）
- [ ] 联系邮箱与隐私政策中的联系方式一致