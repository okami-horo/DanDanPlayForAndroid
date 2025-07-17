#!/bin/bash

echo "🔄 重新加载VSCode项目..."

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}📋 步骤 1: 确保DataBinding文件已生成${NC}"
echo "生成player_component的DataBinding文件..."
./gradlew :player_component:dataBindingGenBaseClassesDebug --quiet

echo -e "${BLUE}📋 步骤 2: 检查生成的文件${NC}"
if [ -f "player_component/build/generated/data_binding_base_class_source_out/debug/out/com/xyoye/player_component/databinding/LayoutSwitchVideoSourceBinding.java" ]; then
    echo -e "${GREEN}✅ LayoutSwitchVideoSourceBinding.java 已生成${NC}"
else
    echo -e "${YELLOW}⚠️  LayoutSwitchVideoSourceBinding.java 未找到${NC}"
fi

if [ -f "player_component/build/generated/data_binding_base_class_source_out/debug/out/com/xyoye/player_component/databinding/ItemVideoSourceBinding.java" ]; then
    echo -e "${GREEN}✅ ItemVideoSourceBinding.java 已生成${NC}"
else
    echo -e "${YELLOW}⚠️  ItemVideoSourceBinding.java 未找到${NC}"
fi

echo -e "${BLUE}📋 步骤 3: 重新加载项目的建议${NC}"
echo ""
echo -e "${GREEN}🎯 现在请在VSCode中执行以下操作:${NC}"
echo ""
echo "1. 打开命令面板 (Ctrl+Shift+P 或 Cmd+Shift+P)"
echo "2. 运行以下命令 (按顺序执行):"
echo "   📌 Java: Reload Projects"
echo "   📌 Kotlin: Restart Language Server"
echo "   📌 Developer: Reload Window (如果上面两个不起作用)"
echo ""
echo "3. 等待VSCode重新索引项目 (可能需要1-2分钟)"
echo ""
echo "4. 如果问题仍然存在，尝试:"
echo "   📌 关闭VSCode"
echo "   📌 重新打开: code DanDanPlay.code-workspace"
echo ""
echo -e "${YELLOW}💡 提示:${NC}"
echo "- 确保你已经安装了推荐的VSCode扩展"
echo "- 查看VSCode输出面板中的 'Kotlin Language Server' 日志"
echo "- 如果仍有问题，可以尝试删除 .vscode 目录并重新运行 setup_vscode.sh"
echo ""
echo -e "${GREEN}🔧 DataBinding文件位置:${NC}"
echo "player_component/build/generated/data_binding_base_class_source_out/debug/out/com/xyoye/player_component/databinding/"
echo ""
echo -e "${GREEN}✅ 重新加载指南完成！${NC}"
