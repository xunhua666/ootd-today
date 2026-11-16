# 更新日志

本项目按 [语义化版本](https://semver.org/lang/zh-CN/) 标记功能版本，App 的 `versionName` 与仓库 tag 对应。

## 未发布 — 应用商店准备（第一阶段，2026-11）

- 新增正式 upload keystore 签名配置（`keystore.properties` + `ootd-release.jks`，均不入库），release 产物与 AAB 可用自有证书签名。
- ML Kit 图像打标由 17.0.7 升级到 **17.0.9**：传递依赖 `vision-internal-vkp` 18.2.2 → 18.2.3，原生库 **支持 16 KB 页面大小**，满足 Google Play 对 targetSdk 35+ 的硬性要求。
- 新增 `scripts/check_16kb.py`：解析 APK/AAB 内 64 位 `.so` 的 ELF `PT_LOAD` 对齐，CI 每次推送校验。
- 网络层强制 **HTTPS**：`HttpJson` 拒绝远端明文 HTTP，仅放行本机回环地址；新增 7 个传输安全单元测试。
- CI 新增 release APK + AAB 构建与 16 KB 校验步骤。
- 新增 [docs/RELEASING.md](docs/RELEASING.md)：签名、AAB、Play 上架与第二阶段待办清单。

## v1.5.0 — 首次开源（2026-11）

- 以 v1.5 代码为基线发布首个开源版本，许可证 Apache-2.0。
- 清理仓库：排除构建产物、验收证据、备份快照、APK、商业计划书与模型权重。
- 新增 README、LICENSE、NOTICE、CONTRIBUTING、SECURITY、行为准则、架构与隐私文档。
- 新增 GitHub Actions（JDK 21 单元测试 + Lint）与 Issue/PR 模板。
- 修复：移除两个仍使用旧包名 `com.example.xaiochenxu` 的脚手架测试（其中设备测试会断言错误包名）。

## v1.5 — 照片版型识别与搭配

- 云端视觉识别单品版型（fit / length / rise / cut / shoulder），上传前逐次确认，返回结果逐项人工确认。
- 照片本地缩放至最长边 1280、纠正 EXIF 旋转、重新编码并去除原始 EXIF。
- 版型标签参与搭配软评分与推荐理由；已确认版型同步给本地审美专家。
- 数据库 v4 → v5：`wardrobe_items` 新增 `shapeJson` 列（增量迁移）。
- 测试：158 项单元测试（当时基线）、5 项设备测试。

## v1.4 — 悬浮专家与日程日历

- 可拖动的卡通专家入口，位置按比例保存并限制在安全区内。
- 穿搭日历：月视图、日程增删改、完成/撤销、单品快照、月计划/待办/打卡统计。
- 数据库 v3 → v4：新增 `calendar_plans` 表。
- 测试：147 项单元测试，迁移与拖拽 UI 测试通过。

## v1.3 — 本地审美专家

- LiteRT-LM + Qwen3-1.7B INT4 本地推理，可离线交流。
- 模型下载（DownloadManager，默认 Wi-Fi）、进度/取消、SHA-256 校验、导入与删除。
- 用户可命名专家并填写风格偏好；本地专家不上传衣橱与聊天内容。
- 包体由约 41 MB 增至约 92 MB（新增本地推理运行时，模型不入包）。

## v1.2 — 体验衣橱与穿搭建议

- 明确标注的体验衣橱，可一键开始个人衣橱；体验数据不计入个人统计、不落库。
- 「围绕某件单品搭一套」、穿着成本投影与行动建议。
- 升级保留既有单品与打卡记录。

## v1.1 — 视觉重设计

- 撞色品牌色板、深色模式、字体层级与卡片体系重做。
- 衣橱分类/搜索/闲置排序，穿搭日历雏形，分享与采纳流程。
- 测试：131 项单元测试，lint 0 错误。