# 发布与上架准备

这份文档记录「从源码到 Google Play」的可复现步骤，以及第一阶段已经完成的技术整改。

## 一、签名（Google Play App Signing 的 upload key）

### 1. 生成 keystore

在项目根目录执行（Windows 用 `keytool.exe`，JDK 21 自带）：

```bash
keytool -genkeypair -v \
  -keystore ootd-release.jks \
  -alias ootd \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=OOTD Today, O=OOTD Today, C=CN"
```

### 2. 写入 keystore.properties

```properties
storeFile=ootd-release.jks
storePassword=<你的密码>
keyAlias=ootd
keyPassword=<你的密码>
```

`storeFile` 相对**项目根目录**解析。`keystore.properties` 与 `*.jks` 都已在 `.gitignore` 中，不会被提交。

### 3. 必须备份

这个 keystore 是 Play App Signing 的 **upload key**。丢失后需要向 Google 申请重置上传密钥；重置前无法给已上架应用打新版本。请把它和密码一起存到密码管理器或离线介质。

> 真正给用户分发的应用签名由 Google 保管（App Signing key），upload key 只用于证明「这个包是我传的」。

## 二、构建发布产物

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

产物：

- `app/build/outputs/apk/release/app-release.apk`（本机验证用）
- `app/build/outputs/bundle/release/app-release.aab`（**上传 Play 用这个**）

验证签名：

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

预期输出包含你的证书 DN（不是 `CN=Android Debug`）。

## 三、16 KB 页面大小校验

Google Play 对 targetSdk ≥ 35 的应用强制要求 16 KB 页面大小支持（2025-11-01 起）。
`zipalign` 只检查 zip 内偏移，不够；仓库提供了检查 ELF 段对齐的脚本：

```bash
python3 scripts/check_16kb.py \
  app/build/outputs/apk/release/app-release.apk \
  app/build/outputs/bundle/release/app-release.aab
```

当前依赖（ML Kit 17.0.9 → vision-internal-vkp 18.2.3）已满足要求。
CI 每次推送都会跑这个检查，回归会直接失败。

## 四、上传 Google Play

1. 注册开发者账号（$25 一次性），完成身份验证。
2. Play Console → 创建应用 → 选择「应用」→ 默认语言/应用名。
3. **Play App Signing**：按提示上传 `.aab`，上传密钥即上面的 keystore。
4. 填写「应用内容」：隐私政策、数据安全、内容分级、目标受众、广告声明。
5. 先走**内部测试**轨道验证安装与签名，再进入封闭测试（新个人账号通常要求 12 人 × 14 天），最后申请正式发布。
6. 每次发布递增 `versionCode`（`app/build.gradle.kts`），`versionName` 只是展示用。

## 五、第一阶段已完成的整改

| 项目 | 状态 |
|---|---|
| 正式 upload keystore + release 签名配置 | ✅ 本地完成（keystore 与密码不入库） |
| AAB 构建与签名验证 | ✅ 本地 + CI |
| 16 KB 页面大小 | ✅ ML Kit 17.0.7 → 17.0.9，脚本 + CI 校验 |
| 强制 HTTPS | ✅ `HttpJson` 拒绝远端明文 HTTP，仅放行回环地址调试 |
| 传输安全单元测试 | ✅ 7 个用例覆盖协议白名单与回环调试 |

## 六、第二阶段待办（上架前必须完成）

- [x] 英文隐私政策页 `docs/privacy.html`（启用 GitHub Pages 后即可作为商店 URL）
- [x] Data Safety 填写对照 `docs/DATA_SAFETY.md`
- [x] 生成式 AI 合规：专家页「报告不当内容」入口 + AI 披露（`docs/AI_POLICY.md`）
- [x] 商店文案与素材清单 `docs/STORE_LISTING.md`
- [ ] 英文界面（当前 913 处中文字面量，计划见 [I18N_PROGRESS.md](I18N_PROGRESS.md)）
- [ ] 商店素材：512 图标、1024×500 feature graphic、英文截图
- [ ] 审核说明（App access）：AI 功能需用户自备 API Key，核心功能无需登录

## 七、检查清单（提交前）

- [ ] `keystore.properties` 存在且已备份
- [ ] `./gradlew :app:testDebugUnitTest` 通过
- [ ] `./gradlew :app:bundleRelease` 产出 `.aab`
- [ ] `scripts/check_16kb.py` 通过
- [ ] `apksigner` 显示自己的证书
- [ ] Play Console 的应用内容、数据安全、隐私政策均已完成
- [ ] 版本号已递增