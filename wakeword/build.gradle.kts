plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.dokka)
    `maven-publish`
}

android {
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    namespace = "com.rementia.openwakeword.lib"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        aarMetadata { minCompileSdk = 23 }
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.coroutinesAndroid)
    implementation(libs.onnxRuntimeAndroid)
    implementation(libs.commonsMath3)
    testImplementation(libs.junit4)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.rementia"
                artifactId = "openwakeword"
                version = "0.1.0"

                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
}
