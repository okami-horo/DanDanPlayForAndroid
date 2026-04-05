## 1. Completion 可信度守卫

- [ ] 1.1 在 `DanDanVideoPlayer.onCompletion()` 增加 Bilibili completion 可信度判定（直播一律异常、长视频片尾窗口判定）
- [ ] 1.2 对异常 completion 写入当前位置进度并合成错误，触发现有 `observerPlayError -> tryRecoverPlayback()` 自动恢复链路
- [ ] 1.3 对真实播完 completion 写入 `position=0` 且保留正确 `duration`，保持现有“播完从头”语义

## 2. 回归与验证

- [ ] 2.1 增加最小可测用例（可选：抽取纯函数并补充 JVM 单元测试；否则补充可复现的日志校验点）
- [ ] 2.2 运行 `./gradlew testDebugUnitTest` 并确认输出尾部为 `BUILD SUCCESSFUL`
- [ ] 2.3 冒烟验证：Bilibili 直播与长视频在异常断流/URL 过期场景下不再自动跳下一项，且能自动恢复或给出可理解的错误提示
