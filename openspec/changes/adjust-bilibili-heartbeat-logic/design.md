## Context

当前仓库把 Bilibili 播放心跳是否生效挂在 `BilibiliPlaybackPreferences.enableHeartbeatReport` 上：该值由 `BilibiliPlaybackPreferencesStore` 按 `storageKey` 持久化，UI 入口位于 `storage_component` 的 `BilibiliStorageEditDialog` / `dialog_bilibili_storage.xml`，播放期则在 `BilibiliPlaybackSession.reportPlaybackHeartbeat()` 中直接作为总开关判断。与此同时，`BilibiliPlaybackHeartbeat` 已经负责 5 秒进度心跳、暂停心跳和 completed 心跳调度，而 `BilibiliRepositoryCore.playbackHeartbeat()` 也已经根据 `BilibiliKeys` 自动区分 `ArchiveKey`、`PgcEpisodeKey`、`LiveKey` 与 `PgcSeasonKey`。

这意味着当前设计存在两层语义冲突：

- “是否允许上报”被错误地建模为播放偏好的一部分，和画质、编码、CDN 等真正的取流偏好混在一起。
- 程序已经具备按内容类型自动判断是否可上报的能力，但又被一个按媒体库、默认关闭的人工开关提前短路。

参考 `E:\Project\PiliPlus\lib\plugin\pl_player\controller.dart`、`E:\Project\PiliPlus\lib\http\video.dart`、`E:\Project\PiliPlus\lib\http\user.dart` 与历史状态相关控制器可以看到，PiliPlus 的心跳策略本质上是“程序自动决策 + 账号/隐私状态约束”：按登录态、历史暂停状态、匿名模式与视频类型决定是否调用心跳接口。映射到本仓库时，需要保留其业务语义，但必须按当前模块边界做本地化适配：把状态判断收敛在 `bilibili_component`，把 TV 端设置入口保留在现有 Bilibili 连接配置页面，而不是照搬 Flutter 侧页面结构。

## Goals / Non-Goals

**Goals:**

- 让 Bilibili 播放心跳变成“程序自动决定是否上报”的能力，而不是依赖按媒体库手动开启。
- 将用户控制重新定义为当前 Bilibili 登录会话（`storageKey`）级别的“退出历史同步”语义，并默认为自动。
- 对齐 Bilibili 服务端历史暂停状态，在本地退出开关、服务端暂停状态和内容类型之间建立清晰优先级。
- 保持 `BilibiliRepositoryCore.playbackHeartbeat()` 作为最终参数构建与类型校验位置，不把网络细节散落到 UI 或播放器模块。
- 维持 TV 端“修改即保存”的交互方式，避免新增复杂确认流程。

**Non-Goals:**

- 不重构整个 Bilibili 账号体系，不引入独立的全局账号中心或跨媒体源统一历史中心。
- 不改造直播房间进入心跳、直播弹幕心跳或其他非 `/x/click-interface/web/heartbeat` 的接口语义。
- 不在本次变更中扩展尚未被当前 `BilibiliKeys` 模型覆盖的新内容类型；缺失 key 建模的类型维持现状。
- 不重新定义播放器“何时算完成播放”的状态机，仅要求心跳逻辑跟随播放器最终产出的播放状态语义。

## Decisions

### 1. 引入独立的“历史同步/心跳策略”存储与决策层

新增独立于 `BilibiliPlaybackPreferences` 的历史同步偏好存储（例如 `BilibiliHistorySyncPreferencesStore`），按 `storageKey` 维护用户对当前 Bilibili 登录会话的历史同步模式。模式至少包含：

- `AUTO`：默认模式，表示允许程序根据登录态、服务端状态和内容类型自动决定是否上报。
- `DISABLED`：显式退出，仅禁止当前会话在本客户端发起播放心跳。

同时新增集中式决策入口（例如 `BilibiliHeartbeatPolicy` 或同等职责对象），由 `BilibiliPlaybackSession`/`BilibiliPlaybackHeartbeat` 在准备发送前查询。该入口统一回答“当前内容、当前会话、当前服务端状态下是否允许上报”，而不再由 UI 直接控制底层网络行为。

**Why this way:**

- 心跳/历史同步不属于取流偏好，拆分后职责更清晰，也便于后续单测和状态迁移。
- 将策略收敛在 `bilibili_component` 可以同时服务播放期、登录后刷新和设置页展示，避免重复判断。

**Alternatives considered:**

- **保留 `enableHeartbeatReport` 在 `BilibiliPlaybackPreferences` 中，仅改默认值为 true。** 未选，因为这只能修正默认值，无法修正“播放偏好混入历史同步语义”与“按媒体库建模”两个结构问题。
- **完全取消用户开关，只保留程序自动判断。** 未选，因为仍需要为隐私敏感用户保留显式退出能力。

### 2. 将 `storageKey` 视为当前架构下的“账号/会话边界”

当前仓库中 Cookie、csrf、播放偏好与清理逻辑都已经按 `storageKey` 隔离，因此本次变更不尝试引入真正的全局 `mid` 级偏好仓库，而是将“账号级用户控制”在当前实现中落地为“登录会话级（`storageKey`）用户控制”。UI 仍可挂在 `BilibiliStorageEditDialog` 附近，但语义需改为“这个 Bilibili 连接是否跟随服务端同步历史”，而不是“这个媒体库是否允许心跳”。

**Why this way:**

- 最小化跨模块改动，和现有鉴权、清理、自动保存模型对齐。
- 避免因为引入真正的全局账号层而扩大到 `user_component`、账号切换、跨 URL 会话冲突等更大问题。

**Alternatives considered:**

- **按 `mid` 做全局唯一用户开关。** 未选，因为当前仓库并没有稳定的全局 Bilibili 账号中心，多个连接的 URL/API 类型也可能不同，实现成本高且会外溢到更多模块。

### 3. 新增服务端“历史暂停状态”读取，并建立优先级规则

在 `BilibiliService` / `BilibiliRepositoryCore` 中新增对 `/x/v2/history/shadow?jsonp=jsonp` 的读取封装，并按 `storageKey` 缓存最近一次结果（包含 paused 状态与刷新时间）。心跳许可的优先级为：

1. 本地模式为 `DISABLED` → 一律禁止；
2. 服务端历史状态为 paused → 一律禁止；
3. 当前未登录、csrf 不可用、内容类型不支持或缺少必需参数 → 禁止；
4. 其余情况 → 允许自动上报。

当服务端状态刷新失败时，采用“优先使用最近一次已知结果；若首次无缓存则暂时按未暂停处理，同时记录日志并在后续时机重试”的策略。

建议的刷新时机：

- 登录成功后立即刷新一次；
- 打开 Bilibili 存储编辑页时刷新一次展示状态；
- 新建或恢复 `BilibiliPlaybackSession` 时，如缓存过期则后台刷新；
- 后续可在应用启动或账号恢复时补充懒刷新，但不作为本次强依赖。

**Why this way:**

- 这样既能尊重 Bilibili 服务端的真实暂停状态，又不会因为一次网络异常把所有心跳永久打死。
- “读服务端状态 + 本地退出”的双层结构与 PiliPlus 的行为语义一致，但更贴合当前 Kotlin/模块化架构。

**Alternatives considered:**

- **服务端状态未知即一律禁用。** 未选，因为会把短暂接口失败放大成长期历史不同步，用户感知非常差。
- **完全忽略服务端暂停状态，只用本地开关。** 未选，因为这会继续造成客户端与 Bilibili 服务端行为不一致。

### 4. 保持 repository 负责内容类型映射与参数兜底，policy 负责“要不要发”

`BilibiliHeartbeatPolicy`（或同等职责层）只负责心跳许可判断；真正的请求参数映射与兜底校验继续放在 `BilibiliRepositoryCore.playbackHeartbeat()`：

- `ArchiveKey` → `type=3`，必须携带 `bvid + cid`；
- `PgcEpisodeKey` → `type=4`，必须携带 `epid + cid`，有 `seasonId` 时附带 `sid`；
- `LiveKey` / `PgcSeasonKey` → 直接跳过播放心跳；
- 其他不满足参数条件的情况 → 视为不可上报，不抛出影响播放的致命错误。

**Why this way:**

- 可以保留现有 repository 作为网络契约的唯一出口，避免 UI/播放器层了解具体参数规则。
- 便于未来扩展到新 key 类型时，只需要在 repository 与 policy 两处受控增加能力。

**Alternatives considered:**

- **把参数构建和类型判断一起移动到 policy。** 未选，因为那会弱化 repository 的契约边界，让业务层承担太多 API 细节。

### 5. 通过“键是否存在”迁移旧的 `heartbeat_report` 语义

旧逻辑中 `heartbeat_report` 默认值是 false，但“false”同时可能表示“用户从未设置过”与“用户明确关闭过”，直接沿用会把错误默认永远保留下来。迁移方案如下：

- 若旧 key 不存在：视为未设置，迁移为新模式 `AUTO`；
- 若旧 key 存在且为 `true`：迁移为 `AUTO`；
- 若旧 key 存在且为 `false`：迁移为 `DISABLED`；
- 新 store 写入成功后，移除旧 key 或将其标记为已迁移，避免双写长期共存。

迁移触发时机可选其一或组合：

- 新 store 首次读取时进行惰性迁移；
- 打开存储编辑页时顺手迁移；
- 清理/断开连接时一并清理新旧 key。

**Why this way:**

- 唯一能区分“旧默认 false”与“用户显式关闭”的信息，就是 key 是否存在。
- 能最大程度修复历史错误默认值，又不强行覆盖已经明确做出的用户选择。

**Alternatives considered:**

- **把所有旧 false 都解释为 `DISABLED`。** 未选，因为会让大量从未主动配置的用户继续处于静默不同步状态。
- **把所有旧 false 都解释为 `AUTO`。** 未选，因为会覆盖一部分用户明确关闭的历史选择。

### 6. TV 端 UI 从“心跳上报 开/关”改为“历史同步 自动/关闭”

保留现有 `BilibiliStorageEditDialog` 的 TV 端设置入口，但将文案和含义调整为：

- 标题从“心跳上报”改为“历史同步”（或同等用户可理解文案）；
- 选项从“开启/关闭”改为“自动/关闭”；
- `AUTO` 不是强制立即发包，而是“跟随登录态、服务端状态和内容类型自动处理”；
- 保持现有 DPAD 可达、即时保存、焦点反馈和恢复默认行为。

若服务端状态为 paused，可在 UI 上补充只读说明（例如“B站账号已暂停历史记录”），但不要求在本次变更中新增复杂提示框或二级页面。

**Why this way:**

- 用户看到的是“我是否同步历史”，不是“是否允许底层发心跳包”；文案对齐后，认知成本更低。
- 继续使用现有对话框能减少 TV 路径回归风险。

**Alternatives considered:**

- **直接移除所有用户控制。** 未选，因为会牺牲隐私可控性。
- **把该设置迁移到 `user_component` 通用设置页。** 本次未选，留作后续体验统一化优化。

### 7. 增加可观测性与测试覆盖，避免“为什么没同步”变成黑盒

在 `bilibili_component` 中为“心跳被跳过”的主要原因增加可诊断日志（例如 local disabled / server paused / unsupported key / missing csrf / missing cid），并补充以下测试：

- 历史同步模式迁移测试；
- 服务端暂停状态优先级测试；
- `ArchiveKey` / `PgcEpisodeKey` / `LiveKey` 的策略与参数映射测试；
- UI/状态绑定测试（若现有测试栈可承载，则至少覆盖 ViewModel/对话框逻辑，否则以单测 + 手工回归说明补足）。

**Why this way:**

- 心跳相关问题常常不是“报错崩溃”，而是“悄悄没生效”；没有原因日志会让排查体验非常痛苦。

## Risks / Trade-offs

- [风险] 同一真实 Bilibili 账号如果被配置在多个不同 `storageKey` 连接中，本次仍会按会话分别保存 `AUTO/DISABLED`。
  - 缓解：在设计和文案中明确当前语义是“当前连接/登录会话”，后续如果仓库引入全局账号层，再统一收口。

- [风险] 服务端历史状态接口不稳定时，可能出现短时间“UI 显示未知，但播放继续上报”。
  - 缓解：使用最近一次已知缓存、增加刷新日志，并在登录后/编辑页打开时主动刷新降低窗口期。

- [风险] 迁移逻辑处理不当会把旧用户误判成显式关闭或显式开启。
  - 缓解：以“key 是否存在”为迁移依据，补充单测覆盖三种迁移路径（不存在 / true / false）。

- [风险] UI 文案变化可能让老用户误以为功能被新增或行为改变过大。
  - 缓解：使用更贴近业务语义的“历史同步”文案，并在变更说明中明确这是对原心跳开关语义的纠偏，而不是新增独立能力。

## Migration Plan

1. 在 `bilibili_component` 内新增历史同步模式 store、服务端历史状态读取模型与策略入口。
2. 接入惰性迁移：首次读取新 store 时检查旧 `heartbeat_report` key 是否存在，并按存在性映射到 `AUTO/DISABLED`。
3. 更新 `BilibiliPlaybackSession` / `BilibiliPlaybackHeartbeat` 调用链，改为先查策略再发心跳。
4. 更新 `BilibiliStorageEditDialog` 与 `dialog_bilibili_storage.xml`，将旧文案和绑定逻辑替换成新模式展示。
5. 添加单元测试与必要的 UI 回归说明，确认旧 key 迁移、服务端暂停、内容类型映射与 TV 焦点路径都符合预期。
6. 若实现中保留旧 key 兼容读取，则在验证稳定后安排一次清理；若实现直接移除旧 key，则确保断开连接清理路径同步删除新旧配置。

回滚策略：若新策略导致大面积历史不同步，可临时退回到“仅本地 `AUTO/DISABLED` 控制，不读取服务端历史暂停状态”的保守模式，但不应回滚到旧的按媒体库默认关闭设计。

## Open Questions

- 用户在 UI 中切换为“关闭历史同步”时，是否需要顺带调用 Bilibili 的服务端暂停历史接口（`/x/v2/history/shadow/set`）来对齐其他客户端行为？本次设计默认先把该开关定义为“仅控制当前客户端”，后续可按产品取舍扩展。
- 当前 `BilibiliKeys` 未显式覆盖的内容类型（例如未来若补充 PUGV 课程 key）是否需要与本 capability 同步扩展？本次先限制在现有 key 模型可表达的内容范围内。
