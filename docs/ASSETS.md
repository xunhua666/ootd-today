# 素材与出处说明

本仓库包含的图片素材只有两类：应用内吉祥物，以及文档截图。均无第三方版权素材。

## 吉祥物

- 文件：`app/src/main/res/drawable-nodpi/expert_mascot.png`
- 来源：为本项目用内置图像生成工具原创生成，非临摹或拼贴现有 IP。
- 生成参数（透明背景、单角色、正面三分之四视角、黏土质感）留档如下，供后续风格统一时参考：

```
Use case: stylized-concept.
Asset type: transparent PNG cartoon character for an Android wardrobe app,
a friendly aesthetic advisor users can name.
Primary request: one original adorable chibi human fashion designer, an
approachable little adult with short soft chestnut bob hair, round
expressive dark eyes, rosy cheeks and a gentle confident smile, wearing a
sage green beret, cream oversized knit sweater over moss green trousers and
cream sneakers. Hold a tiny fan of three fabric color swatches at waist
height, wave with the other hand. Tasteful contemporary collectible clay toy
illustration, soft matte 3D material, charming rounded proportions, refined
simple silhouette readable at 90dp. Full body centered, front three-quarter
view with face looking at viewer, entire character and beret visible, ample
transparent padding, soft studio light. A small lime star pin on the beret
links to the wardrobe app palette. Single character only. True transparent
alpha background. No lettering, no name, no logo, no border, no scene, no
floor, no contact shadow.
```

## 文档截图

`docs/screenshots/` 下的 8 张截图来自本项目的测试构建（Pixel 8 Pro 模拟器），画面中的单品、穿着次数、日程与聊天内容均为演示数据，不含真实用户信息。截图为运行界面，非设计稿，已等比缩放至宽度 720 px 以控制仓库体积。

## 字体与图标

- 字体使用 Android 系统默认字体，未打包第三方字体文件。
- 图标使用 Jetpack Compose Material Icons 与自绘矢量图，未引入第三方图标库资源。

## 第三方 SDK 附带的资源

ML Kit、LiteRT-LM、AndroidX/Compose 等依赖会随 APK 打包各自的资源与原生库，其许可证见仓库根目录 [NOTICE](../NOTICE)。本仓库不包含这些依赖的源码或独立资源文件。