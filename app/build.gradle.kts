import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ootd.today"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ootd.today"
        minSdk = 24
        targetSdk = 37
        versionCode = 6
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // 正式签名从项目根目录的 keystore.properties 读取（该文件不入库）。
        // 文件不存在时不创建 config，release 回退到 debug 签名以便本机验证。
        val propsFile = rootProject.file("keystore.properties")
        if (propsFile.exists()) {
            create("release") {
                val props = Properties().apply { FileInputStream(propsFile).use { load(it) } }
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            if (providers.gradleProperty("ootdPreview").isPresent) applicationIdSuffix = ".preview"
        }
        release {
            optimization {
                // AGP 9.3 新 DSL：一个开关同时启用代码优化与优化版资源裁剪。
                // keep 规则放在 src/main/keepRules/*.keep，默认平台规则已隐含包含。
                enable = true
            }
            // 有正式签名配置时用它；没有则回退 debug 签名，保证本机可安装验证。
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

// Note: with AGP 9 built-in Kotlin there is no `kotlinOptions {}` DSL.
// jvmTarget defaults to `compileOptions.targetCompatibility` above.

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

/**
 * AGP 9 的内置 Kotlin（KGP [kotlinVersion]）**不会发布 kotlin-stdlib 的版本约束**，
 * 传递依赖会把 stdlib 抬到远高于编译器的版本（实测被抬到 2.4.10），
 * 编译时全线报 “metadata version is 2.4.0, but the compiler version 2.2.0
 * can read versions up to 2.3.0”。
 *
 * 这里显式锁定到内置 Kotlin 对应的版本。若以后升级到独立的 KGP，
 * 同步修改 [kotlinVersion] 即可。
 */
val kotlinVersion = libs.versions.kotlin.get()
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
            useVersion(kotlinVersion)
        }
    }
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // androidx.hilt 1.2+ 把 Compose 的 hiltViewModel() 拆到了这个 artifact，
    // 旧的 androidx.hilt.navigation.compose.hiltViewModel 已标记废弃。
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Images
    implementation(libs.coil.compose)

    // CameraX (used by the item-capture flow in a later step)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // WorkManager: daily outfit reminder
    implementation(libs.androidx.work.runtime.ktx)
    // Hilt <-> WorkManager integration so Workers can use constructor injection
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // ML Kit on-device image labeling (P1: auto pre-fill category & color)
    implementation(libs.mlkit.image.labeling)
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.19.0")

    // Tests
    testImplementation(libs.junit)
    // Android 自带的 org.json 在单元测试里是空壳实现（方法一调就抛
    // "not mocked"），因此需要引入一份真正可用的实现，
    // 否则任何用到 JSONObject 的解析逻辑都无法单测。
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
