# Z7X ADB 开启器 v1.0.0

用于已验证的极米 Z7X 高亮版 Android 9 固件。安装后首次打开即自动设置网络 ADB 5555，并验证属性、adbd 状态及本机端口。

- 原生 Java Android Studio 工程，MIT 开源。
- README 提供从 Activity Launcher 到联发科 Design Menu 的完整图文教程。
- 包含用户设备的 Execute Shell 对话框原始截图。
- APK 不需要 root、系统签名、辅助功能或已有 ADB 连接。
- 支持开机广播后自动尝试恢复，默认启用；整机重启后的流程尚未实测。
- 普通应用 UID 设置属性及 adbd 停止后的恢复已实机通过。

下载 `Z7X-ADB-v1.0.0.apk`，用 U 盘安装到投影仪并点“打开”。电脑与投影仪在同一局域网后执行 `adb connect 投影仪IP:5555`。

本次固件 SELinux 为 Permissive；不保证其他型号或固件适用。关闭开机恢复选项或卸载应用不会撤销已写入的 ADB 配置。请在可信网络中使用。

`SHA256SUMS.txt` 提供下载校验；`极米Z7X开启ADB完整教程.html` 为可离线打开的图文教程，截图已嵌入文件。
