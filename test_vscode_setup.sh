#!/bin/bash

echo "🧪 测试VSCode配置..."

# 检查配置文件是否存在
config_files=(
    ".vscode/settings.json"
    ".vscode/extensions.json"
    ".vscode/tasks.json"
    ".vscode/launch.json"
    "DanDanPlay.code-workspace"
)

echo "📁 检查配置文件..."
for file in "${config_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file 存在"
    else
        echo "❌ $file 不存在"
    fi
done

# 检查Gradle构建
echo ""
echo "🔧 测试Gradle构建..."
if ./gradlew tasks --quiet > /dev/null 2>&1; then
    echo "✅ Gradle配置正常"
else
    echo "❌ Gradle配置有问题"
fi

# 检查项目结构
echo ""
echo "📂 检查项目结构..."
if ./gradlew projects --quiet > /dev/null 2>&1; then
    echo "✅ 项目结构正常"
    echo "📋 项目模块:"
    ./gradlew projects --quiet | grep "Project" | head -10
else
    echo "❌ 项目结构有问题"
fi

# 检查Kotlin文件
echo ""
echo "📝 检查Kotlin文件..."
kotlin_files=$(find . -name "*.kt" -not -path "./build/*" | head -5)
if [ -n "$kotlin_files" ]; then
    echo "✅ 找到Kotlin文件:"
    echo "$kotlin_files"
else
    echo "❌ 未找到Kotlin文件"
fi

echo ""
echo "🎯 配置测试完成！"
echo ""
echo "📋 使用说明:"
echo "1. 运行 'code DanDanPlay.code-workspace' 打开项目"
echo "2. 安装推荐的扩展（VSCode会自动提示）"
echo "3. 等待项目索引完成"
echo "4. 开始编码！"
