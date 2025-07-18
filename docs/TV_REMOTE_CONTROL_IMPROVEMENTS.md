# TV端遥控器控制优化说明

## 问题分析

### 原有问题
1. **焦点丢失问题**：`requestIndexChildFocus`方法中使用`scrollToPosition`后立即查找View，但View可能还未创建完成
2. **焦点重置问题**：在`previousItemIndex`和`nextItemIndex`方法中，当找不到目标时会循环到列表的另一端，导致焦点突然跳转
3. **焦点状态管理不完善**：缺乏焦点状态的保存和恢复机制，在数据更新时焦点容易丢失
4. **边界检查不足**：没有充分的边界检查，可能导致焦点跳转到无效位置
5. **边界按键事件处理问题**：当用户在列表边界（如最后一个元素）按下方向键时，事件没有被正确消费，导致按键事件向上传递，可能被其他组件处理产生意外行为

## 解决方案

### 1. 优化RecyclerView焦点管理

#### 改进的`requestIndexChildFocus`方法
- 添加边界检查，防止索引越界
- 使用`smoothScrollToPosition`替代`scrollToPosition`，提供更平滑的滚动体验
- 实现重试机制，确保焦点设置成功
- 添加焦点状态保存和恢复功能

#### 新增扩展方法
- `getCurrentFocusedPosition()`: 获取当前获得焦点的item位置
- `requestIndexChildFocusSafe()`: 安全的焦点请求，带有边界检查和状态验证

### 2. 修复边界按键事件处理

#### 问题描述
在之前的实现中，当用户在RecyclerView的边界位置（如最后一个元素）按下方向键时，`TvKeyEventHelper.handleRecyclerViewKeyEvent`方法会返回false，表示没有处理该事件。这导致按键事件继续向上传递，可能被其他组件处理，产生意外的行为。

#### 解决方案
修改`TvKeyEventHelper.handleRecyclerViewKeyEvent`方法的逻辑：
- 当`targetIndex`为-1时，表示无法处理该方向的导航，不消费事件（返回false）
- 当`targetIndex`与当前索引相同时，表示已经在边界位置，消费事件但不移动焦点（返回true）
- 只有当`targetIndex`不同于当前索引时，才尝试移动焦点

#### 改进的`handleDownKey`方法
针对垂直线性布局，明确检查是否已经在最后一个项目：
- 如果`currentIndex >= items.size - 1`，返回`currentIndex`（保持当前位置）
- 否则返回`currentIndex + 1`（移动到下一个项目）

### 3. 增强焦点导航逻辑

#### 改进的导航方法
- `previousItemIndexSafe()`: 安全的上一个Item查找，优先查找相邻项目
- `nextItemIndexSafe()`: 安全的下一个Item查找，优先查找相邻项目
- 默认不允许循环跳转，防止焦点突然从底部跳到顶部

### 4. 焦点状态管理器

#### TvFocusManager功能
- 保存和恢复RecyclerView的焦点状态
- 智能焦点恢复，在数据更新后自动尝试恢复焦点
- 焦点状态过期管理，防止内存泄漏
- 数据变化时的焦点位置自动调整

#### 使用方法
```kotlin
// 保存焦点状态
recyclerView.saveTvFocus("unique_key")

// 恢复焦点状态
recyclerView.restoreTvFocus("unique_key")

// 设置智能焦点恢复
recyclerView.setupSmartTvFocus("unique_key")
```

### 5. TV端按键事件处理增强

#### TvKeyEventHelper功能
- 智能处理不同布局管理器的按键事件
- 支持GridLayoutManager和LinearLayoutManager
- 提供更精确的方向键导航逻辑
- 边界检查和安全导航

#### 使用方法
```kotlin
// 处理RecyclerView的方向键事件
TvKeyEventHelper.handleRecyclerViewKeyEvent<ItemType>(
    recyclerView,
    keyCode,
    dataList
)
```

## 修改的文件

### 核心扩展文件
1. `common_component/src/main/java/com/xyoye/common_component/extension/RecyclerViewExt.kt`
   - 优化`requestIndexChildFocus`方法
   - 添加`getCurrentFocusedPosition`和`requestIndexChildFocusSafe`方法

2. `common_component/src/main/java/com/xyoye/common_component/extension/CollectionExt.kt`
   - 改进`previousItemIndex`和`nextItemIndex`方法
   - 添加`previousItemIndexSafe`和`nextItemIndexSafe`方法

### 新增工具类
1. `common_component/src/main/java/com/xyoye/common_component/utils/tv/TvFocusManager.kt`
   - TV端焦点状态管理器

2. `common_component/src/main/java/com/xyoye/common_component/utils/tv/TvKeyEventHelper.kt`
   - TV端按键事件处理增强工具类

### 播放器设置View更新
1. `player_component/src/main/java/com/xyoye/player/controller/setting/PlayerSettingView.kt`
2. `player_component/src/main/java/com/xyoye/player/controller/setting/SwitchVideoSourceView.kt`
3. `player_component/src/main/java/com/xyoye/player/controller/setting/SettingTracksView.kt`
4. `player_component/src/main/java/com/xyoye/player/controller/setting/SettingVideoAspectView.kt`
5. `player_component/src/main/java/com/xyoye/player/controller/setting/SwitchSourceView.kt`

### 控制器更新
1. `player_component/src/main/java/com/xyoye/player/controller/base/TvVideoController.kt`
   - 添加TV端按键检查

### Fragment更新
1. `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragment.kt`
   - 集成焦点状态管理

## 使用建议

### 对于开发者
1. 在新的RecyclerView实现中，使用`requestIndexChildFocusSafe`替代`requestIndexChildFocus`
2. 使用`TvKeyEventHelper.handleRecyclerViewKeyEvent`处理方向键事件
3. 为重要的RecyclerView设置智能焦点恢复：`recyclerView.setupSmartTvFocus(uniqueKey)`

### 对于用户
1. 焦点导航更加稳定，不会出现突然跳转的情况
2. 在数据更新后，焦点会智能恢复到合适的位置
3. 边界处理更加友好，不会跳转到无效位置

## 测试建议

1. 测试各种RecyclerView布局（线性、网格）的焦点导航
2. 测试数据更新时的焦点保持
3. 测试边界情况（列表为空、只有一个item等）
4. 测试长时间使用后的内存占用情况
5. 测试不同设备上的兼容性

## 注意事项

1. 焦点状态会自动清理过期数据，默认5分钟过期
2. 智能焦点恢复需要为每个RecyclerView设置唯一的key
3. 建议在Fragment的onResume/onPause中保存和恢复焦点状态
4. 对于复杂的布局，可能需要自定义焦点导航逻辑
