# VSCode Android Kotlin 开发环境配置

## 概述
这个配置让VSCode能够像Android Studio一样提供完整的Kotlin/Android开发体验，包括：
- 代码智能提示和自动完成
- 语法错误检查和警告
- 代码格式化
- 重构支持
- 调试功能
- Gradle集成

## 必需的VSCode扩展

请安装以下扩展以获得最佳开发体验：

### 核心扩展
1. **Kotlin** (fwcd.kotlin) - Kotlin语言支持
2. **Extension Pack for Java** (vscjava.vscode-java-pack) - Java开发工具包
3. **Gradle for Java** (vscjava.vscode-gradle) - Gradle支持
4. **Android iOS Emulator** (adelphes.android-dev-ext) - Android开发支持

### 推荐扩展
- **GitLens** (eamodio.gitlens) - Git增强
- **Error Lens** (usernamehw.errorlens) - 错误提示增强
- **vscode-icons** (vscode-icons-team.vscode-icons) - 文件图标
- **Path Intellisense** (christian-kohler.path-intellisense) - 路径智能提示

## 使用方法

### 1. 打开工作区
使用以下命令打开工作区：
```bash
code DanDanPlay.code-workspace
```

### 2. 等待项目索引
首次打开时，VSCode会：
- 下载并配置Kotlin语言服务器
- 索引项目文件
- 解析Gradle依赖

这个过程可能需要几分钟，请耐心等待。

### 3. 构建项目
使用快捷键 `Ctrl+Shift+P` 打开命令面板，然后：
- 输入 "Tasks: Run Task"
- 选择 "Gradle: Build Debug" 来构建调试版本

或者在终端中运行：
```bash
./gradlew assembleDebug
```

### 4. 代码提示和错误检查
配置完成后，你将获得：
- 实时语法错误检查
- 智能代码补全
- 导入语句自动整理
- 代码格式化（保存时自动）
- 重构支持

## 故障排除

### 如果代码提示不工作
1. 确保所有推荐的扩展都已安装
2. 重新加载窗口：`Ctrl+Shift+P` -> "Developer: Reload Window"
3. 清理并重建项目：`./gradlew clean assembleDebug`

### 如果Gradle同步失败
1. 检查网络连接
2. 确保Java环境正确配置
3. 运行 `./gradlew --refresh-dependencies`

### 如果Kotlin语言服务器无响应
1. 重启语言服务器：`Ctrl+Shift+P` -> "Kotlin: Restart Language Server"
2. 检查输出面板中的错误信息

## 配置文件说明

- `settings.json` - VSCode工作区设置
- `extensions.json` - 推荐扩展列表
- `tasks.json` - Gradle构建任务
- `launch.json` - 调试配置
- `kotlin.json` - Kotlin语言服务器配置
- `DanDanPlay.code-workspace` - 工作区配置文件

## 快捷键

- `Ctrl+Shift+O` - 快速打开文件中的符号
- `Ctrl+T` - 在工作区中搜索符号
- `F12` - 跳转到定义
- `Shift+F12` - 查找所有引用
- `Ctrl+Shift+F` - 全局搜索
- `Ctrl+Shift+P` - 命令面板
