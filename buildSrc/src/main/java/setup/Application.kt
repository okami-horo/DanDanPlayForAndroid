package setup

import com.android.build.gradle.AppExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName
import setup.utils.setupDefaultDependencies
import setup.utils.setupKotlinOptions
import setup.utils.setupOutputApk
import setup.utils.setupSignConfigs

@Suppress("UnstableApiUsage")
fun Project.applicationSetup() {
    extensions.getByName<AppExtension>("android").apply {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        buildFeatures.apply {
            dataBinding.isEnabled = true
        }

        // 添加BuildConfig字段支持弹弹play认证信息
        defaultConfig {
            buildConfigField("String", "DANDANPLAY_APP_ID", "\"${System.getenv("DANDANPLAY_APP_ID") ?: ""}\"")
            buildConfigField("String", "DANDANPLAY_APP_SECRET", "\"${System.getenv("DANDANPLAY_APP_SECRET") ?: ""}\"")
        }

        setupKotlinOptions()
        setupSignConfigs(this@applicationSetup)
        setupOutputApk()
    }

    setupDefaultDependencies()
}