## Why

当前项目在播放 Bilibili 媒体库的直播与长视频（时长较长的投稿/番剧）时，观看一段时间后可能因取流 URL 过期、CDN 断流或异常 EOF 等原因触发播放器 `completion`，但并未进入 `error` 恢复链路。现有实现会将该 `completion` 视为“正常播完”，从而自动跳转到下一项，导致观看被打断并可能污染播放历史/心跳上报。

## What Changes

- 为 `MediaType.BILIBILI_STORAGE` 播放源增加“完成态可信度”判定：仅当满足“非直播且接近片尾”时才视为真实播完。
- 对“不可信 completion”（直播 completion、或长视频远未到片尾的 completion）改为进入错误恢复路径：触发既有 `PlaybackUrlRecoverableAddon.recover(...)` 自动换源续播当前内容，而不是跳到下一项。
- 避免在不可信 completion 场景下上报 `completed` 心跳（`playedTimeSec = -1`）或写入“播完”历史。
- 保持改动收敛在既有播放器链路内，优先复用现有日志与自动恢复逻辑，不引入新的协调层或额外调用链。

## Capabilities

### New Capabilities
- `bilibili-unexpected-completion`: 定义 Bilibili 播放源在 `completion` 发生时的可信度判定、自动恢复与自动下一项的边界行为。

### Modified Capabilities
- 无

## Impact

- 受影响模块：`player_component`（完成态判定、错误恢复触发与历史记录写入）、`bilibili_component`（心跳/异常 completion 观测结果将随行为调整而变化）。
- 对 UI/交互影响：在异常 completion 场景下不再跳到下一项，改为自动恢复当前内容；正常播完行为保持不变。
