package setup

import com.android.build.gradle.AppExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import setup.utils.setupDefaultDependencies
import setup.utils.setupKotlinOptions
import setup.utils.setupOutputApk
import setup.utils.setupSignConfigs

@Suppress("UnstableApiUsage", "DEPRECATION")
fun Project.applicationSetup() {
    extensions.getByName<AppExtension>("android").apply {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        ndkVersion = Versions.ndkVersion

        lintOptions {
            lintConfig = rootProject.file("lint.xml")
        }

        buildFeatures.apply {
            dataBinding.isEnabled = true
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
        setupSignConfigs(this@applicationSetup)
        setupOutputApk()
    }

    tasks.withType<Test>().configureEach {
        extensions.configure(JacocoTaskExtension::class.java) {
            isIncludeNoLocationClasses = true
            excludes?.add("jdk.internal.*")
        }
    }

    setupDefaultDependencies()
}
