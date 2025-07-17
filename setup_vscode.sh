#!/bin/bash

echo "🚀 设置VSCode Android Kotlin开发环境..."

# 检查VSCode是否安装
if ! command -v code &> /dev/null; then
    echo "❌ VSCode未安装，请先安装VSCode"
    exit 1
fi

echo "✅ VSCode已安装"

# 安装推荐的扩展
echo "📦 安装VSCode扩展..."

extensions=(
    "fwcd.kotlin"
    "redhat.java"
    "vscjava.vscode-java-pack"
    "vscjava.vscode-java-dependency"
    "vscjava.vscode-java-test"
    "vscjava.vscode-maven"
    "vscjava.vscode-gradle"
    "adelphes.android-dev-ext"
    "naco-siren.gradle-language"
    "eamodio.gitlens"
    "esbenp.prettier-vscode"
    "vscode-icons-team.vscode-icons"
    "CoenraadS.bracket-pair-colorizer-2"
    "christian-kohler.path-intellisense"
    "formulahendry.auto-rename-tag"
    "redhat.vscode-xml"
    "redhat.vscode-yaml"
    "usernamehw.errorlens"
)

for extension in "${extensions[@]}"; do
    echo "安装扩展: $extension"
    code --install-extension "$extension" --force
done

echo "✅ 扩展安装完成"

# 检查Java环境
echo "☕ 检查Java环境..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1)
    echo "✅ Java环境: $java_version"
else
    echo "❌ Java未安装，请先安装Java"
    exit 1
fi

# 检查Android SDK
echo "📱 检查Android SDK..."
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    echo "✅ Android SDK: $ANDROID_HOME"
else
    echo "⚠️  ANDROID_HOME未设置或路径不存在"
    echo "请确保Android SDK已正确安装并设置环境变量"
fi

# 检查Gradle
echo "🔧 检查Gradle..."
if [ -f "./gradlew" ]; then
    echo "✅ Gradle Wrapper存在"
    chmod +x ./gradlew
    ./gradlew --version | head -5
else
    echo "❌ Gradle Wrapper不存在"
    exit 1
fi

# 清理并构建项目
echo "🏗️  清理并构建项目..."
./gradlew clean

echo "🎉 VSCode环境设置完成！"
echo ""
echo "📋 下一步："
echo "1. 使用 'code DanDanPlay.code-workspace' 打开工作区"
echo "2. 等待VSCode索引项目（可能需要几分钟）"
echo "3. 在命令面板中运行 'Java: Reload Projects'"
echo "4. 开始编码！"
echo ""
echo "💡 提示："
echo "- 使用 Ctrl+Shift+P 打开命令面板"
echo "- 使用 Ctrl+Shift+O 快速跳转到文件中的符号"
echo "- 使用 F12 跳转到定义"
echo "- 查看 .vscode/README.md 获取更多帮助"
