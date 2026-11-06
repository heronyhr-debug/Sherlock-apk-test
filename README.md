# Sherlock Android Termux (community integration)

本项目是独立 Android 中文 UI，通过 Termux 调用 Sherlock 官方 Python 包；不包含 Sherlock 核心源码，不是官方 Android APK。

## 部署到 GitHub
创建空仓库，将 ZIP 解压后的工程目录中的所有文件（含 `.github`）上传。
进入 Actions → Build Android APK → Run workflow，成功后在 Artifacts 下载 `sherlock-debug-apk`，解压后安装 `app-debug.apk`。
Debug APK 是开发测试签名，不是正式发布签名。GitHub Actions 构建尚需在你的仓库验证。

## 手机上初始化
从官方 Termux GitHub 或 F-Droid 安装 Termux（注意安装来源与签名一致）。
把工程解压到 Termux 可以访问的位置，例如 `~/storage/downloads/Sherlock-Android-Termux`。
首次需在 Termux 执行 `termux-setup-storage` 并允许存储访问，然后：
```sh
cd ~/storage/downloads/Sherlock-Android-Termux
bash termux/install.sh
```
在 Android 系统设置 → 应用 → Sherlock 检索 → 权限 → 额外权限中，授予在 Termux 环境运行命令的权限。
`allow-external-apps=true` 允许获得该权限的应用执行 Termux 命令，请仅授权可信 APK。

## 操作
输入一个或多个用户名，设置并发任务数（1–10）、可选代理 URL 和导出格式。
点击开始后，切换到 Termux 查看执行状态。结果存储在 Termux 的
`~/storage/downloads/SherlockResults/`，对应 Android Downloads/SherlockResults。
`summary.txt`、`summary.csv`、`summary.json` 是原始命令输出和退出状态汇总，不代表身份真实性；逐用户名文件由上游 Sherlock CLI 创建。
`run.log` 保存诊断信息。APP 的文件按钮调用 Android 文件选择器；不提供自动实时日志回传。

## 排错与局限
- 若 Termux 没反应：检查 `com.termux.permission.RUN_COMMAND`、`allow-external-apps=true`，并执行 `termux-reload-settings`。
- `No module named sherlock_project`：在 Termux 中重新执行 `python -m pip install sherlock-project`。
- 导出失败：检查 `termux-setup-storage` 和 Downloads 访问权限。
- 代理 127.0.0.1 指手机自身，需确保代理服务在手机上监听。
- 多线程为多个用户名的并行子进程，不是 Sherlock 内部网站级并发控制；可能触发网站限流。
- 各站点可能拒绝自动查询，失败与未找到不同；用户需遵守法律及站点条款。
- `targetSdk=28` 是兼容性折中，未来 Android 版本可能限制旧目标 SDK 的安装或运行。
- 当前工作流依赖 runner 提供的 Gradle 命令，建议后续固定 Gradle wrapper 与发布签名。
- 请只对合法公开信息进行查询。

## 本地 Python 测试
```sh
python3 -m unittest discover -s tests -v
```
