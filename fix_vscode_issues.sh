#!/bin/bash

echo "🔧 修复VSCode Android Kotlin项目配置问题..."

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查是否在项目根目录
if [ ! -f "build.gradle.kts" ]; then
    echo -e "${RED}❌ 请在项目根目录运行此脚本${NC}"
    exit 1
fi

echo -e "${BLUE}📋 步骤 1: 清理并重新构建项目${NC}"
echo "清理构建缓存..."
./gradlew clean

echo "构建项目以生成必要的文件..."
./gradlew build -x test --continue || {
    echo -e "${YELLOW}⚠️  构建过程中有一些错误，但继续执行...${NC}"
}

echo -e "${BLUE}📋 步骤 2: 重新加载Gradle项目${NC}"
# 删除IDE缓存
if [ -d ".idea" ]; then
    echo "删除IntelliJ IDEA缓存..."
    rm -rf .idea
fi

# 重新生成Gradle wrapper
echo "重新生成Gradle wrapper..."
./gradlew wrapper

echo -e "${BLUE}📋 步骤 3: 检查生成的DataBinding文件${NC}"
databinding_files=$(find . -name "*Binding.java" -path "*/build/generated/*" | head -5)
if [ -n "$databinding_files" ]; then
    echo -e "${GREEN}✅ 找到DataBinding文件:${NC}"
    echo "$databinding_files"
else
    echo -e "${YELLOW}⚠️  未找到DataBinding文件，尝试重新生成...${NC}"
    ./gradlew :player_component:dataBindingGenBaseClassesDebug
fi

echo -e "${BLUE}📋 步骤 4: 更新VSCode配置${NC}"

# 创建或更新 .vscode/kotlin.json
cat > .vscode/kotlin.json << 'EOF'
{
    "kotlin.languageServer.enabled": true,
    "kotlin.languageServer.path": "",
    "kotlin.compiler.jvm.target": "1.8",
    "kotlin.linting.run": "onSave",
    "kotlin.completion.snippets.enabled": true,
    "kotlin.debugAdapter.enabled": true,
    "kotlin.inlayHints.enabled": true,
    "kotlin.inlayHints.typeHints": true,
    "kotlin.inlayHints.parameterHints": true,
    "kotlin.inlayHints.chainingHints": true
}
EOF

echo -e "${GREEN}✅ 更新了Kotlin配置${NC}"

echo -e "${BLUE}📋 步骤 5: 检查Android SDK配置${NC}"
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    echo -e "${GREEN}✅ Android SDK: $ANDROID_HOME${NC}"
else
    echo -e "${YELLOW}⚠️  ANDROID_HOME未设置，尝试查找Android SDK...${NC}"
    
    # 常见的Android SDK路径
    possible_paths=(
        "/opt/android-sdk"
        "/usr/local/android-sdk"
        "$HOME/Android/Sdk"
        "$HOME/android-sdk"
    )
    
    for path in "${possible_paths[@]}"; do
        if [ -d "$path" ]; then
            echo -e "${GREEN}✅ 找到Android SDK: $path${NC}"
            echo "请将以下行添加到你的 ~/.bashrc 或 ~/.zshrc:"
            echo "export ANDROID_HOME=$path"
            echo "export PATH=\$PATH:\$ANDROID_HOME/tools:\$ANDROID_HOME/platform-tools"
            break
        fi
    done
fi

echo -e "${BLUE}📋 步骤 6: 生成VSCode任务配置${NC}"

# 更新tasks.json以包含有用的任务
cat > .vscode/tasks.json << 'EOF'
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Gradle: Clean",
            "type": "shell",
            "command": "./gradlew",
            "args": ["clean"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        },
        {
            "label": "Gradle: Build",
            "type": "shell",
            "command": "./gradlew",
            "args": ["build", "-x", "test"],
            "group": {
                "kind": "build",
                "isDefault": true
            },
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        },
        {
            "label": "Gradle: Generate DataBinding",
            "type": "shell",
            "command": "./gradlew",
            "args": ["dataBindingGenBaseClassesDebug"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        },
        {
            "label": "Reload Java Projects",
            "type": "shell",
            "command": "echo",
            "args": ["请在VSCode命令面板中运行: Java: Reload Projects"],
            "group": "build"
        }
    ]
}
EOF

echo -e "${GREEN}✅ 更新了VSCode任务配置${NC}"

echo -e "${BLUE}📋 步骤 7: 最终检查${NC}"

# 检查关键文件
echo "检查关键配置文件..."
files_to_check=(
    ".vscode/settings.json"
    ".vscode/tasks.json"
    ".vscode/kotlin.json"
    "DanDanPlay.code-workspace"
)

for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✅ $file${NC}"
    else
        echo -e "${RED}❌ $file 缺失${NC}"
    fi
done

echo ""
echo -e "${GREEN}🎉 VSCode配置修复完成！${NC}"
echo ""
echo -e "${YELLOW}📋 接下来的步骤:${NC}"
echo "1. 重启VSCode"
echo "2. 使用 'code DanDanPlay.code-workspace' 打开工作区"
echo "3. 等待VSCode索引项目（可能需要几分钟）"
echo "4. 在命令面板 (Ctrl+Shift+P) 中运行以下命令："
echo "   - 'Java: Reload Projects'"
echo "   - 'Kotlin: Restart Language Server'"
echo "5. 如果仍有问题，尝试："
echo "   - 'Developer: Reload Window'"
echo ""
echo -e "${BLUE}💡 提示:${NC}"
echo "- 确保安装了推荐的VSCode扩展"
echo "- 如果DataBinding类仍然报错，运行: ./gradlew build"
echo "- 检查VSCode输出面板中的Kotlin Language Server日志"
