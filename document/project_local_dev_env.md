# 项目独立开发环境

本文档说明如何在仓库内使用项目私有的 JDK 与 Android SDK，避免污染系统级 `JAVA_HOME`、`ANDROID_HOME` 与 `ANDROID_SDK_ROOT`。

## 目录约定

- 项目私有 JDK：`E:\Project\DDPlayTV\.dev-env\jdk\ms-17.0.15`
- 项目私有 Android SDK：`E:\Project\DDPlayTV\.dev-env\android-sdk`
- 项目私有 Gradle 用户目录：`E:\Project\DDPlayTV\.dev-env\gradle-home`
- 本地 Gradle 启动脚本：
  - `scripts/dev-env/gradlew-local.bat`
  - `scripts/dev-env/gradlew-local.ps1`
- 本地环境重建脚本：
  - `scripts/dev-env/setup-local-env.ps1`

`.dev-env/` 已加入 `.gitignore`，不会进入版本控制。

## 当前环境内容

- JDK：17
- Android Platform：35
- Android Build Tools：
  - `34.0.0`
  - `35.0.0`
- CMake：3.22.1
- NDK：`25.2.9519653`
- Platform Tools：本地安装

当前实际构建验证通过的关键组合是：

- `NDK 25.2.9519653`
- `CMake 3.22.1`
- `Build-Tools 34.0.0` 与 `35.0.0`

虽然工程公共配置里仍然声明了 `23.1.7779620`，但本次项目私有环境验证中并未要求额外安装该版本，`player_component` 使用的 `25.2.9519653` 已能通过当前 Gradle 配置校验。

## 使用方式

### 首次重建本地环境

```powershell
.\scripts\dev-env\setup-local-env.ps1
```

该脚本会复制 JDK 17、复制 `cmdline-tools`、同步 licenses，并安装项目编译所需的 SDK/NDK/CMake 包。

### PowerShell

```powershell
.\scripts\dev-env\gradlew-local.ps1 :app:assembleDebug
```

### CMD

```bat
scripts\dev-env\gradlew-local.bat :app:assembleDebug
```

## 设计原则

- 不修改系统环境变量
- 不依赖系统 `JAVA_HOME`
- 不依赖系统 `ANDROID_SDK_ROOT`
- 不依赖系统 `GRADLE_USER_HOME`
- 项目通过 `local.properties` 固定使用仓库内 SDK
- 仅在执行本地脚本时临时注入 JDK / SDK / Gradle 相关环境变量

## 注意事项

- 直接运行仓库根目录的 `gradlew.bat` 时，仍然会走当前 shell 的 `JAVA_HOME` / `PATH`。如果你要稳定复现当前环境，请始终使用 `scripts/dev-env/gradlew-local.*`
- 如果后续工程重新引入对 `23.1.7779620` 的硬依赖，再补装对应 NDK 即可
- 若迁移仓库目录，需要同步更新 `local.properties` 里的 `sdk.dir`
