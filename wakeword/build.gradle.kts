plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
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

tasks.register<Jar>("androidJavadocsJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
    dependsOn(tasks.dokkaHtml)
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "centralPortal"
                url = uri("https://central.sonatype.com/api/v1/publisher/upload")
                credentials {
                    username = findProperty("ossrhUsername") as String? ?: ""
                    password = findProperty("ossrhPassword") as String? ?: ""
                }
            }
        }
        
        publications {
            register<MavenPublication>("release") {
                groupId = "xyz.rementia"
                artifactId = "openwakeword"
                version = "0.1.2"

                afterEvaluate {
                    from(components["release"])
                }

                pom {
                    name.set("OpenWakeWord Android")
                    description.set("Kotlin library for on-device wake word detection on Android using ONNX Runtime")
                    url.set("https://github.com/Re-MENTIA/openwakeword-android-kt")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("re-mentia")
                            name.set("Re-MENTIA")
                            email.set("dev@rementia.tech")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/Re-MENTIA/openwakeword-android-kt.git")
                        developerConnection.set("scm:git:ssh://github.com/Re-MENTIA/openwakeword-android-kt.git")
                        url.set("https://github.com/Re-MENTIA/openwakeword-android-kt")
                    }
                }
            }
        }
    }

    signing {
        val keyId = findProperty("signing.keyId") as String?
        val secretKey = findProperty("signing.secretKeyRingFile") as String?
        val password = findProperty("signing.password") as String?

        if (keyId != null && secretKey != null) {
            if (secretKey.endsWith(".gpg")) {
                // Use the secret key file directly
                sign(publishing.publications["release"])
            } else {
                // Use in-memory signing with the key content
                useInMemoryPgpKeys(keyId, file(secretKey).readText(), password ?: "")
                sign(publishing.publications["release"])
            }
        }
    }
}

tasks.configureEach {
    if (name == "generateMetadataFileForReleasePublication") {
        dependsOn("releaseSourcesJar")
    }
}

tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    
    dokkaSourceSets {
        configureEach {
            moduleName.set("OpenWakeWord Android Library")
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(uri("https://github.com/Re-MENTIA/openwakeword-android-kt/tree/main/wakeword/src/main/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}
