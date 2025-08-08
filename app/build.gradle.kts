// build.gradle.kts - Android アプリケーションのビルド設定
// Gradle のプラグインと依存関係を Kotlin DSL で定義しています

plugins {
    // alias: libs.versions.toml で定義されたプラグインを参照
    alias(libs.plugins.android.application) // Android アプリケーション用プラグイン
    alias(libs.plugins.kotlin.android)     // Kotlin Android プラグイン
    alias(libs.plugins.kotlin.compose)     // Jetpack Compose プラグイン
}

android {
    // アプリケーションのパッケージ名
    namespace = "com.negi.pipechat"
    // コンパイル対象の Android SDK バージョン
    compileSdk = 35

    defaultConfig {
        // アプリケーション ID
        applicationId = "com.negi.pipechat"
        // 最小サポート API レベル: MediaPipe と Compose の安定動作には 28 以上推奨
        minSdk = 28
        // ターゲット API レベル
        targetSdk = 35
        // バージョンコードとバージョン名
        versionCode = 1
        versionName = "1.0"

        // Instrumentation テスト用ランナー
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            // リリースビルドはコード圧縮・難読化を無効化
            isMinifyEnabled = false
            // ProGuard 設定ファイル
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // デバッグ用署名設定を参照
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // Java ソース互換性を設定
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // Kotlin コンパイル時の JVM ターゲットバージョン
        jvmTarget = "17"
    }

    buildFeatures {
        // Jetpack Compose を有効化
        compose = true
    }

    composeOptions {
        // Compose Compiler のバージョンを BOM に合わせて指定
        kotlinCompilerExtensionVersion = "1.5.12"
    }
}

dependencies {
    // --- MediaPipe LLMInference タスク: オンデバイス LLM 呼び出しに使用 ---
    implementation("com.google.mediapipe:tasks-genai:0.10.25")
    implementation("com.google.mediapipe:solution-core:0.10.20")

    // --- AndroidX Core ライブラリ ---
    implementation(libs.androidx.core.ktx)               // 拡張関数や便利メソッドを提供
    implementation(libs.androidx.lifecycle.runtime.ktx)  // ライフサイクル対応
    implementation(libs.androidx.activity.compose)       // Compose 版 Activity ライブラリ

    // --- Jetpack Compose BOM: 依存バージョンの一貫性を保証 ---
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI コンポーネント
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3 デザインシステム
    implementation("androidx.compose.material3:material3")
    // ※libs.androidx.material3 が定義されている場合はこちらに統一可能
    // implementation(libs.androidx.material3)

    // 開発時のデバッグツール
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
