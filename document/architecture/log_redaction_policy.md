# 日志与异常上报脱敏策略（统一工具）

## 背景

本仓库将“日志/异常上报默认脱敏”作为长期治理要求；当前以本文与 `document/monitoring/logging-system.md` 作为维护基线。  
核心诉求是：在 **logcat / HTTP 对外日志 / 异常上报上下文** 中，默认不泄露 token、cookie、密码、secret、URL query 等敏感信息，同时仍保留足够的定位线索（host/path、指纹等）。

## 统一入口

- 统一脱敏工具：`core_log_component/src/main/java/com/xyoye/common_component/log/privacy/SensitiveDataSanitizer.kt`
- 统一接入点（默认生效）：
  - `LogFormatter`：对日志 `message/context/throwable` 做脱敏后再输出到 logcat/HTTP（对外接口同样要求默认脱敏）
  - `ErrorReportHelper`：对调试输出的 tag/extraInfo 做脱敏（避免本地调试阶段泄露）

## 默认规则（摘要）

### 1) Key-based 脱敏（context/params/header）

当 key 命中以下敏感字段（大小写不敏感）时，value 会直接替换为 `<redacted>`：

- `token` / `access_token` / `refresh_token`
- `authorization` / `proxy-authorization`
- `cookie` / `set-cookie`
- `password` / `passwd`
- `secret` / `appsecret` / `app_sec` / `appsec`
- `x-appid` / `x-appsecret`（避免泄露构建期注入或运行期拼装的敏感 header）

> 说明：如需要扩展敏感 key，统一在 `SensitiveDataSanitizer.sensitiveKeys` 中维护，避免各模块重复实现。

### 2) URL 默认策略

- 默认模式（SAFE）：仅保留 `scheme://host[:port]/path`，**不输出 query/fragment**
- “仅保留 query key”模式（KEYS_ONLY）：保留 query key，但 value 统一 `<redacted>`（fragment 仍不输出）

### 3) 磁力链（magnet）

- 默认仅保留 `btih` hash：`magnet:btih=<hash>`
- 无法提取 hash 时输出：`magnet:btih=<redacted>`

### 4) 本地路径

- 默认输出 `文件名#指纹`，避免输出完整目录结构：`movie.mp4#1a2b3c4d`

## HTTP 日志服务安全门禁

HTTP 日志服务属于“仅局域网调试”的对外输出通道：

- 必须 Token 鉴权（Header + URL 参数，Header 优先）
- 强制限制仅局域网来源可访问
- 对外输出与落盘默认先脱敏（优先“先脱敏再存储/返回”）

## 调用建议

### 业务日志（推荐）

- 使用 `LogFacade` + `context` 传递结构化字段
- 不要把完整 URL query/token/cookie 拼到 message；即使拼了，Formatter 也会做兜底脱敏，但更推荐让上下文结构化、可控

### 异常上报上下文（推荐）

- 在构造 `extraInfo` 时，优先传“可定位但不敏感”的信息：
  - URL：使用 SAFE（host/path）+ 可选 fingerprint
  - 磁力链：仅 hash
  - 路径：仅文件名或 fingerprint
