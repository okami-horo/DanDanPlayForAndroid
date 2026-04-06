## 1. 历史同步模式与迁移

- [x] 1.1 在 `bilibili_component` 中新增独立的历史同步模式模型与存储（如 `AUTO` / `DISABLED`），并实现基于旧 `heartbeat_report` key 存在性的惰性迁移逻辑
- [x] 1.2 更新断开连接、恢复默认和配置清理路径，确保新的历史同步配置与遗留心跳配置都能被正确清除或重置

## 2. 服务端状态与策略决策

- [x] 2.1 为 Bilibili 服务端历史暂停状态补充接口、数据模型、repository 封装和按 `storageKey` 的缓存/刷新逻辑
- [x] 2.2 实现集中式心跳策略判断，统一合并本地历史同步模式、服务端 paused 状态、登录态、内容类型和参数可用性

## 3. 播放链路接入

- [x] 3.1 更新 `BilibiliPlaybackSession` 与 `BilibiliPlaybackHeartbeat`，在发送进度/完成心跳前先走策略判断，并补充可诊断的跳过原因日志
- [x] 3.2 保持 `BilibiliRepositoryCore.playbackHeartbeat()` 作为参数映射与兜底校验出口，确认投稿/剧集/直播等 key 类型行为符合新规格

## 4. TV 设置与交互调整

- [x] 4.1 将 `BilibiliStorageEditDialog` 与 `dialog_bilibili_storage.xml` 中的“心跳上报 开启/关闭”改为“历史同步 自动/关闭”，并绑定到新的历史同步模式存储
- [x] 4.2 在配置页展示或刷新服务端历史暂停状态的只读信息/状态反馈，保持 TV 端 DPAD 焦点、自动保存与恢复默认体验不退化

## 5. 验证与回归

- [x] 5.1 为迁移逻辑、策略优先级、内容类型映射和关键跳过路径补充单元测试或等价可验证测试
- [x] 5.2 运行与改动范围匹配的 Gradle 测试/构建命令，明确记录输出尾部的 `BUILD SUCCESSFUL` 或 `BUILD FAILED` 作为实施阶段验证证据
