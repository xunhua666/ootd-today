# 参与贡献

感谢你愿意花时间改进这个项目。这是一个个人维护的原型，节奏会比较慢，但每个 Issue 和 PR 都会被认真看。

## 开始之前

- 先读 [README](README.md) 和 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)，大部分「为什么这样写」的问题都能在那里找到答案。
- 大的改动（换架构、加新数据源、改推荐算法主线）请**先开 Issue 讨论**，避免做完才发现方向不一致。
- 不要提交 API Key、个人照片、`local.properties`、`keystore.properties` 或任何签名文件。

## 开发环境

- JDK 21
- Android SDK Platform 37.0（API 37）、Build-Tools 36.0.0
- 首次构建建议先跑一遍基线：

```bash
./gradlew :app:testDebugUnitTest
```

Windows 下把 `./gradlew` 换成 `.\gradlew.bat`。

## 分支与提交

- 从 `main` 切分支：`feat/xxx`、`fix/xxx`、`docs/xxx`、`chore/xxx`。
- 提交信息使用 Conventional Commits（`feat:`、`fix:`、`docs:`、`refactor:`、`test:`、`chore:`），中文或英文均可，一句话说清做了什么。
- 一个 PR 只做一件事；顺手重构请单独提交。

## 代码风格

- Kotlin 官方风格，4 空格缩进，遵循仓库根目录的 `.editorconfig`。
- 注释用中文或英文都可以，但同一文件内保持一致。**解释「为什么」比解释「是什么」更有价值**，现有代码基本遵循这个习惯。
- 领域逻辑尽量写成不依赖 Android 类型的纯函数（`domain/` 下已有大量例子），这样单测成本最低。

## 测试要求

- 新增或修改推荐、审美、版型、解析等纯逻辑时，**必须**补 JVM 单元测试。
- 修改 Room 表结构时：递增数据库版本、提供 Migration、提交 `app/schemas/` 下新生成的 schema，并确认迁移测试通过。**不要**改用 destructive migration。
- 修改可选能力（云视觉、云专家、本地模型）时，至少覆盖失败回退路径。
- CI 会跑 `testDebugUnitTest` 与 `lintDebug`，本地先跑一遍能省一轮来回。

## PR 检查清单

- [ ] 说明改动目的与影响范围
- [ ] 新增/修改的逻辑有测试覆盖
- [ ] `./gradlew :app:testDebugUnitTest` 通过
- [ ] 没有提交构建产物、模型文件、APK、密钥或本地配置
- [ ] 如果改变了隐私边界（新增网络请求、新增权限、上传新数据），同步更新 `docs/PRIVACY.md`

## 关于 AI 辅助代码

欢迎使用 AI 工具辅助开发，但提交者需要对代码的正确性、可读性和许可证合规负责。请自己读懂再提交，不要直接把大段未经验证的生成代码塞进 PR。

## 行为准则

参与本项目即表示同意遵守 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。