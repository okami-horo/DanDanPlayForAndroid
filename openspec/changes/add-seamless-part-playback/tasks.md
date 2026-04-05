## 1. 顺序续播状态建模

- [x] 1.1 引入后端无关的 `SequentialPlaybackCoordinator` 分层，统一承接续播会话生命周期、预解析、降级与状态同步编排
- [x] 1.2 定义 `SequentialPlaybackBackendAdapter`、`SequentialPlaybackStateApplier` 与 `SequentialPlaybackFallbackSwitcher` 等边界，避免将顺序续播逻辑散落在 Activity 与具体后端实现中
- [x] 1.3 为顺序续播定义稳定的 `sessionToken` / `queueItemId` 身份模型，并明确手动切源、错误恢复、释放重建时的失效与取消语义

## 2. Media3 无感续播实现

- [x] 2.1 实现 `Media3SequentialPlaybackAdapter`，将当前项与紧邻下一项映射到 Media3 队列并回传带身份标识的切换事件
- [x] 2.2 在当前源稳定播放后预解析紧邻下一项，并在源变化、手动切换、错误恢复或退出时按 `sessionToken` 正确取消/刷新预准备
- [x] 2.3 当无感续播条件满足时，通过协调层完成轻量切换；条件不满足时统一回退到现有 `applyPlaySource()` 重建链路
- [x] 2.4 为非 Media3 内核接入 fallback/no-op adapter，确保它们继续走现有自动下一项流程而不感知 Media3 专用实现细节

## 3. 源状态与资源同步

- [x] 3.1 在自动切换到下一项后，通过统一 `StateApplier` 更新标题、`VideoSourceManager`、播放会话元数据与当前源引用
- [x] 3.2 记录上一项完成状态，并按下一项自身历史恢复进度、弹幕、字幕与外挂音轨
- [x] 3.3 确保过期的预准备结果或切换回调不会覆盖当前播放状态
- [x] 3.4 确保上一项的文件级绑定状态不会泄漏到下一项；当无感续播失败时回退到现有自动下一项流程

## 4. 共享契约测试与回归

- [x] 4.1 为 `SequentialPlaybackCoordinator` 补充跨后端共享 contract tests，覆盖“有下一项”“无下一项”“过期回调忽略”“预准备失败降级”等公共语义
- [x] 4.2 为 `sessionToken` / `queueItemId` 的失效、取消和错位拦截补充单元测试
- [x] 4.3 为 `Media3SequentialPlaybackAdapter` 补充适配层测试，验证队列准备、媒体项切换事件与 token 透传行为
- [x] 4.4 为非 Media3 fallback adapter 补充回归测试，确保现有自动下一项逻辑不被破坏

## 5. 集成验证

- [ ] 5.1 为 Media3 顺序续播补充或调整集成/冒烟验证，覆盖“有下一项”“无下一项”“手动切换后旧回调到达”“预准备失败降级”场景
- [ ] 5.2 执行相关 Gradle 验证并确认 TV 播放主路径未因本次改动退化
