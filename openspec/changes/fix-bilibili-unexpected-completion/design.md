## Context

当前播放器内核（Media3/VLC/mpv）在不同异常场景下对“播放结束”的表现并不一致：部分网络异常、取流 URL 过期或服务端异常 EOF 可能不会抛出 `error`，而是进入 `ended/completed`。现有链路在 `DanDanVideoPlayer.onCompletion()` 里直接切换到 `PlayState.STATE_COMPLETED`，进而触发 `VideoController` 的“自动播放下一项”，导致 Bilibili 直播/长视频在观看较长时间后被打断并跳转到下一项。

项目已经具备 Bilibili 播放源的自动恢复能力（`PlaybackUrlRecoverableAddon.recover(...)`），但该能力目前仅在 `PlayState.STATE_ERROR` 分支被触发；当异常被内核误报为 completion 时，恢复链路不会执行。

约束与原则：

- 不引入新的播放编排层，不增加额外的跨模块调用链。
- 在现有 `DanDanVideoPlayer` 与 `PlayerActivity` 的错误恢复机制内完成修复。
- 保持非 Bilibili 源、正常播完行为不变。

## Goals / Non-Goals

**Goals:**

- 在 `MediaType.BILIBILI_STORAGE` 下区分“真实播完 completion”与“异常 completion”。
- 异常 completion 必须进入现有错误恢复链路，优先尝试自动恢复当前内容，而不是自动下一项。
- 异常 completion 不得触发 completed 心跳（`playedTimeSec = -1`）与“播完”历史写入。
- 行为可观测：在异常 completion 发生时保留必要日志与诊断字段，便于后续定位。

**Non-Goals:**

- 不调整 Bilibili 媒体库目录/列表的顺序语义与分组逻辑（例如 `/history/` 下的跨条目顺序播放）。
- 不重构播放器状态机，不引入新的 `PlayState` 枚举。
- 不扩展到所有媒体源的通用 completion 判定（本次仅聚焦 Bilibili）。

## Decisions

1. 在 `DanDanVideoPlayer.onCompletion()` 做“完成态可信度”判定

- 选择原因：`onCompletion()` 是三套内核统一出口，最靠近状态源头，能在不增加调用链的前提下阻断误 completion 带来的后续连锁反应（自动下一项、心跳 completed、历史完成写入）。
- 方案：对 `MediaType.BILIBILI_STORAGE`：
  - 若当前为直播（`isLive()` 为 true），任何 completion 视为异常 completion，转入 `PlayState.STATE_ERROR`。
  - 若为点播且视频时长达到“长视频”阈值，则只有在接近片尾（剩余时间小于窗口）时才视为真实播完；否则视为异常 completion，转入 `PlayState.STATE_ERROR`。
- 备选方案：在 `VideoController` 的 `STATE_COMPLETED` 分支增加二次校验并拒绝 auto-next。
  - 未选原因：只能阻止跳转，但无法复用现有 error-recover 流程；同时 completed 心跳/历史写入等副作用仍可能发生。

2. 异常 completion 通过“合成错误”触发既有恢复机制

- 方案：在异常 completion 分支设置 `lastPlaybackError` 为可读异常，并切换 `PlayState.STATE_ERROR`；由 `PlayerActivity.observerPlayError` 复用现有 `tryRecoverPlayback()` 完成恢复。
- 选择原因：不新增回调接口、不新增状态分支，最大化复用现有 addon 恢复逻辑与诊断采集逻辑。
- 备选方案：在 completion 处直接调用 addon.recover 并切源。
  - 未选原因：会把恢复编排分散到多个位置，扩大调用链并增加重复逻辑。

3. 进度记录策略

- 真实播完：写入 `position = 0`、`duration = 实际时长`，保持“下次从头播放”的既有语义，同时补齐历史记录中时长字段。
- 异常 completion：在转入 error 前写入当前位置与时长，确保恢复失败时仍可从最近位置继续播放。

## Risks / Trade-offs

- [风险] 对“接近片尾”的阈值选择不当可能导致极少数正常 completion 被当作异常，从而触发一次恢复。
  - 缓解：仅对长视频启用该判定，并采用相对保守的片尾窗口；同时记录日志便于后续调参。

- [风险] 少量异常 completion 可能来自非 Bilibili 源，当前不覆盖。
  - 缓解：本次先收敛修复 Bilibili；若后续确认存在普遍性，再扩展为通用能力并补充规格。
