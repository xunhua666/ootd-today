## 这个 PR 做了什么

<!-- 一句话说明改动目的 -->

## 为什么这样改

<!-- 关联 Issue：Closes #xxx；或说明背景与取舍 -->

## 改动范围

- [ ] 推荐 / 审美 / 版型等 domain 逻辑
- [ ] 数据层（Room / DataStore / 迁移）
- [ ] 界面与主题
- [ ] 可选能力（云视觉 / 云专家 / 本地模型）
- [ ] 构建、测试或文档
- [ ] 其他：

## 验证方式

<!-- 说明你跑了什么测试、在什么设备上验证过；纯文档改动可写「无需运行」 -->

```text
./gradlew :app:testDebugUnitTest
```

## 检查清单

- [ ] 新增/修改的逻辑有测试覆盖
- [ ] `./gradlew :app:testDebugUnitTest` 通过
- [ ] 没有提交构建产物、APK、模型文件、密钥或本地配置
- [ ] 若改变数据表结构：已递增版本、提供 Migration 并提交 schema
- [ ] 若改变隐私边界：已更新 `docs/PRIVACY.md`
- [ ] 提交信息符合 Conventional Commits