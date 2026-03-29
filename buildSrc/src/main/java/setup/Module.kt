package setup

import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import setup.utils.setupDefaultDependencies
import setup.utils.currentCommit
import setup.utils.setupKotlinOptions

@Suppress("UnstableApiUsage", "DEPRECATION")
fun Project.moduleSetup() {
    extensions.getByName<LibraryExtension>("android").apply {
        compileSdk = Versions.compileSdkVersion
        defaultConfig {
            minSdk = Versions.minSdkVersion
            targetSdk = Versions.targetSdkVersion
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        ndkVersion = Versions.ndkVersion

        lintOptions {
            lintConfig = rootProject.file("lint.xml")
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }

            getByName("debug") {
                initWith(buildTypes.getByName("debug"))
            }

            create("beta") {
                initWith(buildTypes.getByName("beta"))
            }

            buildTypes.forEach {
                it.buildConfigField("String", "BUILD_COMMIT", "\"${currentCommit()}\"")
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        buildFeatures {
            dataBinding = true
            buildConfig = true
        }

        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
            unitTests.all {
                it.systemProperty("robolectric.enabledSdks", Versions.targetSdkVersion.toString())
            }
        }

        setupKotlinOptions()
    }

    tasks.withType<Test>().configureEach {
        extensions.configure(JacocoTaskExtension::class.java) {
            isIncludeNoLocationClasses = true
            excludes?.add("jdk.internal.*")
        }
    }

    setupDefaultDependencies()
}
