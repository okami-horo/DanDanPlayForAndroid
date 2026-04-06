# English

# DDPlayTV Privacy Policy

Last updated: 2026-04-06

DDPlayTV is an open-source local media player. This document describes what this repository version does (and does not) collect when you use the app.

If you use third-party services (for example Bilibili, DandanPlay, Baidu Pan, Shooter subtitle service), your requests and data submitted to those services are governed by their own privacy policies.

## 1. Data We Store Locally

DDPlayTV stores data on your device to provide features, for example:
- App settings and preferences
- Media library configurations
- Playback history and related metadata
- Login state (cookies/tokens) for third-party services you choose to log in to (e.g. Bilibili, Baidu Pan)

This local data is stored in the app’s private storage and is not uploaded automatically by DDPlayTV.

## 2. Data Sent to Third-Party Services

When you use online features, DDPlayTV may send data to third-party services, such as:
- Network requests to obtain playback URLs, subtitles, danmaku, or remote storage file lists
- Authentication cookies/tokens required by those services

DDPlayTV does not operate a dedicated “account system” or a self-hosted analytics backend in this repository by default.

## 3. Crash Reporting (Bugly)

This project integrates Tencent Bugly for crash reporting. If Bugly is enabled/configured in the build, the app may send crash/ANR reports that can include:
- Device model, OS version
- App version and basic runtime information
- Crash stack traces

For the exact behavior, please refer to Bugly’s documentation and privacy policy.

## 4. Permissions

The app may request permissions to enable specific features. The declared permissions may include:
- Network: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`
- Media access: `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED` (Android 13+), and `READ_EXTERNAL_STORAGE` (Android 12 and below)
- Storage compatibility: `WRITE_EXTERNAL_STORAGE` (legacy compatibility; behavior depends on Android version)
- Install packages: `REQUEST_INSTALL_PACKAGES` (only used when you choose to install an APK from the app)
- Background playback: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- Local network discovery: `CHANGE_WIFI_MULTICAST_STATE`
- Camera: the current TV-oriented manifest does **not** declare `CAMERA`; if camera/scanning capabilities are re-enabled in future builds, this policy will be updated accordingly

Some permissions may be disabled for TV adaptation builds (for example, `CAMERA` is currently removed from the main TV-oriented manifest; see the merged AndroidManifest at build time).

## 5. Logs

DDPlayTV always prints runtime logs to Android logcat (subject to the configured log level).

If “HTTP Log Server” is enabled in the app, DDPlayTV can expose logs over the local network via an HTTP server for debugging (page + structured APIs + SSE). Access is protected by a token (Authorization header or `?token=`) and restricted to LAN clients. Outputs are redacted by default. Logs are not uploaded automatically by DDPlayTV.

See `document/monitoring/logging-system.md` for how to enable and capture logs.

## 6. Third-Party Libraries

See `document/Third_Party_Libraries.md` for the major third-party dependencies used by this project.

## 7. Contact

If you have questions or want to report an issue, please open an issue on GitHub:
- https://github.com/okami-horo/DDPlayTV/issues/new/choose

---

# 中文版

# DDPlayTV 隐私政策

最后更新：2026-04-06

DDPlayTV 是一款开源的本地媒体播放器。本文用于说明**本仓库当前版本**在使用过程中会（或不会）收集/处理哪些数据。

如果你使用第三方服务（例如 Bilibili、弹弹 play、百度网盘、Shooter 字幕服务等），你提交给这些服务的请求与数据将受其各自隐私政策约束。

## 1. 本地存储的数据

为实现功能，DDPlayTV 会在你的设备本地存储一些数据，例如：
- App 设置与偏好
- 媒体库配置
- 播放历史与相关元信息
- 你主动登录的第三方服务登录态（Cookie/Token，例如 Bilibili、百度网盘）

上述数据默认仅保存在本地（应用私有目录/数据库/键值存储），DDPlayTV 不会自动上传。

## 2. 可能发送给第三方服务的数据

当你使用联网功能时，DDPlayTV 可能会向第三方服务发送数据，例如：
- 为获取播放链接/字幕/弹幕/远端存储列表等而发起的网络请求
- 访问第三方服务所需的鉴权信息（Cookie/Token 等）

本仓库默认不提供自建账号系统，也不提供自建的埋点/分析后端。

## 3. 崩溃上报（Bugly）

本项目集成了腾讯 Bugly 崩溃上报能力。若在构建时启用/配置了 Bugly，应用在发生崩溃/ANR 时可能会上报包含以下信息的报告：
- 设备型号、系统版本
- App 版本与基础运行态信息
- 崩溃堆栈信息

具体行为以 Bugly 的官方说明与隐私政策为准。

## 4. 权限说明

应用会按功能需要声明/申请权限。当前工程声明的权限可能包括：
- 网络：`INTERNET`、`ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE`
- 媒体读取：Android 13+ 的 `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` / `READ_MEDIA_VISUAL_USER_SELECTED`；Android 12 及以下的 `READ_EXTERNAL_STORAGE`
- 存储兼容：`WRITE_EXTERNAL_STORAGE`（历史兼容；具体效果取决于系统版本与系统策略）
- 安装 APK：`REQUEST_INSTALL_PACKAGES`（仅在你选择从应用内安装 APK 时使用）
- 后台播放：`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- 局域网发现：`CHANGE_WIFI_MULTICAST_STATE`
- 摄像头：当前 TV 适配主线默认**不声明** `CAMERA` 权限；若未来重新启用扫码/摄像能力，本文会同步更新

部分权限可能会因 TV 适配而被禁用（例如当前主线已移除 `CAMERA` 声明；以构建时合并后的 AndroidManifest 为准）。

## 5. 日志

DDPlayTV 默认会将运行日志输出到 Android logcat（受“日志级别”设置影响）。

当你在应用内开启“HTTP 日志服务”时，DDPlayTV 会在局域网内启动仅用于调试的 HTTP 日志服务，提供日志页面、结构化查询接口与实时推送（SSE）。访问需要 Token 鉴权，并限制仅局域网来源可访问。日志默认不会自动上传。

开启方式与抓取示例见：`document/monitoring/logging-system.md`。

## 6. 第三方库

主要依赖清单见：`document/Third_Party_Libraries.md`（非穷举）。

## 7. 联系方式

如有疑问或需要反馈问题，请在 GitHub 提交 issue：
- https://github.com/okami-horo/DDPlayTV/issues/new/choose
