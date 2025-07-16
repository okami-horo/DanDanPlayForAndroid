import subprocess
import getpass
import os
import base64
import tempfile

# 脚本中的常量
KEYSTORE_FILENAME = "dandanplay.jks"
KEY_ALIAS_DEFAULT = "dandanplay"

def print_color(text, color="green"):
    """在终端打印彩色文本"""
    colors = {
        "red": "\033[91m",
        "green": "\033[92m",
        "yellow": "\033[93m",
        "blue": "\033[94m",
        "purple": "\033[95m",
        "cyan": "\033[96m",
        "end": "\033[0m",
    }
    color_code = colors.get(color, colors["green"])
    print(f"{color_code}{text}{colors['end']}")

def check_command_exists(command):
    """检查外部命令是否存在"""
    try:
        if command == "keytool":
            # keytool 不支持 --version，使用 -help 参数
            result = subprocess.run([command, "-help"], capture_output=True, check=False)
            # keytool -help 返回码为0表示成功
            return result.returncode == 0
        elif command == "gh":
            # GitHub CLI 检查
            result = subprocess.run([command, "--version"], capture_output=True, check=False)
            return result.returncode == 0
        else:
            # 其他命令使用 --version
            subprocess.run([command, "--version"], capture_output=True, check=True)
            return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False

def get_repo_name():
    """获取当前Git仓库的名称 (owner/repo)"""
    try:
        url = subprocess.check_output(["git", "config", "--get", "remote.origin.url"]).decode().strip()
        if url.startswith("https://"):
            repo_name = url.split("github.com/")[-1].replace(".git", "")
        elif url.startswith("git@"):
            repo_name = url.split("github.com:")[-1].replace(".git", "")
        else:
            return None
        return repo_name
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None

def main():
    """主函数"""
    print_color("欢迎使用 GitHub Actions Secrets 自动配置脚本（简化版）", "cyan")
    print_color("-" * 60, "cyan")

    # 1. 检查依赖
    print_color("\n[步骤 1/4] 检查环境依赖...", "yellow")
    if not check_command_exists("keytool"):
        print_color("错误: 未找到 `keytool` 命令。请确保您已安装JDK并将其配置到系统环境变量中。", "red")
        return
    if not check_command_exists("gh"):
        print_color("错误: 未找到 `gh` 命令。请确保您已安装GitHub CLI并登录。", "red")
        print_color("安装: winget install --id GitHub.cli", "yellow")
        print_color("登录: gh auth login", "yellow")
        return
    print_color("依赖检查通过！", "green")

    # 2. 获取用户输入
    print_color("\n[步骤 2/4] 请输入您的签名密钥信息:", "yellow")

    keystore_pass = getpass.getpass("请输入密钥库(Keystore)的密码: ")
    while not keystore_pass:
        print_color("密码不能为空，请重新输入。", "red")
        keystore_pass = getpass.getpass("请输入密钥库(Keystore)的密码: ")

    alias_name = input(f"请输入密钥别名(Alias) (默认为: {KEY_ALIAS_DEFAULT}): ") or KEY_ALIAS_DEFAULT

    alias_pass = getpass.getpass(f"请输入别名 '{alias_name}' 的密码: ")
    while not alias_pass:
        print_color("密码不能为空，请重新输入。", "red")
        alias_pass = getpass.getpass(f"请输入别名 '{alias_name}' 的密码: ")

    # 3. 生成密钥库
    print_color(f"\n[步骤 3/4] 正在生成密钥库文件 '{KEYSTORE_FILENAME}'...", "yellow")
    keytool_command = [
        "keytool", "-genkey", "-v",
        "-keystore", KEYSTORE_FILENAME,
        "-alias", alias_name,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-storepass", keystore_pass,
        "-keypass", alias_pass,
        "-dname", "CN=DanDanPlay, OU=DanDanPlay, O=DanDanPlay, L=Unknown, S=Unknown, C=CN"
    ]
    try:
        subprocess.run(keytool_command, check=True, capture_output=True)
        print_color(f"密钥库 '{KEYSTORE_FILENAME}' 生成成功！", "green")
    except subprocess.CalledProcessError as e:
        print_color(f"错误: 密钥库生成失败。\n{e.stderr.decode()}", "red")
        return

    # 4. 转换密钥库为base64并设置GitHub Secrets
    print_color(f"\n[步骤 4/4] 正在设置GitHub Secrets...", "yellow")

    try:
        # 读取密钥库文件并转换为base64
        with open(KEYSTORE_FILENAME, "rb") as f:
            keystore_base64 = base64.b64encode(f.read()).decode()

        print_color("密钥库已转换为base64格式", "green")

        # 设置GitHub Secrets
        secrets = {
            "KEYSTORE_PASS": keystore_pass,
            "ALIAS_NAME": alias_name,
            "ALIAS_PASS": alias_pass,
            "KEYSTORE_FILE": keystore_base64
        }

        repo_name = get_repo_name()
        repo_flag = f" -R {repo_name}" if repo_name else ""

        print_color("正在设置GitHub Secrets...", "yellow")

        for secret_name, secret_value in secrets.items():
            try:
                # 使用临时文件传递secret值
                with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as temp_file:
                    temp_file.write(secret_value)
                    temp_file_path = temp_file.name

                # 使用GitHub CLI设置secret
                cmd = f'gh secret set {secret_name}{repo_flag} < "{temp_file_path}"'
                result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

                # 清理临时文件
                os.unlink(temp_file_path)

                if result.returncode == 0:
                    print_color(f"✓ {secret_name} 设置成功", "green")
                else:
                    print_color(f"✗ {secret_name} 设置失败: {result.stderr}", "red")

            except Exception as e:
                print_color(f"✗ {secret_name} 设置失败: {str(e)}", "red")

        print_color("\nGitHub Secrets 设置完成！", "green")

    except Exception as e:
        print_color(f"错误: 处理密钥库失败。\n{str(e)}", "red")
        return

    # 清理密钥库文件
    try:
        os.remove(KEYSTORE_FILENAME)
        print_color(f"\n本地密钥库文件 '{KEYSTORE_FILENAME}' 已被安全删除", "green")
    except:
        print_color(f"\n警告: 无法删除本地密钥库文件 '{KEYSTORE_FILENAME}'，请手动删除", "yellow")

    print_color("\n🎉 配置完成！", "cyan")
    print_color("-" * 60, "cyan")
    print_color("现在您的GitHub Actions可以使用以下Secrets进行Android应用签名:", "green")
    print_color("• KEYSTORE_PASS - 密钥库密码", "green")
    print_color("• ALIAS_NAME - 密钥别名", "green")
    print_color("• ALIAS_PASS - 别名密码", "green")
    print_color("• KEYSTORE_FILE - 密钥库文件(base64编码)", "green")

    if repo_name:
        print_color(f"\n仓库: {repo_name}", "cyan")
        print_color("您可以在GitHub Actions中直接使用这些secrets了！", "green")
    else:
        print_color("\n注意: 未能自动检测仓库名称", "yellow")


if __name__ == "__main__":
    main()
