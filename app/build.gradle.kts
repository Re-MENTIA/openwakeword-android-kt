plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
    namespace = "com.rementia.openwakeword"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rementia.openwakeword"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
}

dependencies {
    testImplementation(libs.junit4)
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.material)

    implementation(libs.coroutinesAndroid)
    implementation(libs.onnxRuntimeAndroid)
    implementation(libs.commonsMath3)

    implementation(project(":wakeword"))
}
