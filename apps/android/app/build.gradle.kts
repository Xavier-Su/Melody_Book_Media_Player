plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.melodybook.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.melodybook.player"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            // 预置背景图在构建时从旧工程复制进来，见下方 copyPresetBackgrounds
            assets.srcDir(layout.buildDirectory.dir("generated/presetBackgrounds").get().asFile)
        }
    }
}

/**
 * 复用旧版的 21 张预置背景图（legacy/android-java/src/main/res/drawable/bg*.jpg），
 * 构建时复制到 assets/backgrounds/，不在新工程里存第二份。
 * 仓库重构后旧工程移到了 legacy/，这里的路径要跟着走。
 * 如果以后删掉 legacy/，把图片移到 app/src/main/assets/backgrounds/ 即可，代码不用改。
 */
val copyPresetBackgrounds by tasks.registering(Sync::class) {
    from(rootProject.file("../../legacy/android-java/src/main/res/drawable")) {
        include("bg*.jpg")
    }
    into(layout.buildDirectory.dir("generated/presetBackgrounds/backgrounds"))
}
tasks.named("preBuild") { dependsOn(copyPresetBackgrounds) }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.kotlinx.coroutines.guava)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    // 农历、干支、宜忌都在本机计算，不依赖网络
    implementation(libs.lunar)

    testImplementation(libs.junit)
}
