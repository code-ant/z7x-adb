# 极米 ADB 开启器 v1.2.0

## 更新内容

- 页面标题与应用名称改为“极米 ADB 开启器”。
- 显示实际局域网 IP 的 `adb connect` 命令，补充 `adb devices` 检查说明；移除 UID 和属性列表，保留 adbd 状态。
- 新增“不能连接ADB点这里”：仅用于 unauthorized，确认后检查配置并请求一次框架重启，持久标记防止重复尝试。
- 新增“关闭 ADB，恢复默认 USB 设置”：识别支持的固件后关闭调试并切换默认 USB 模式；关闭后禁用自动开启，需要手动恢复。不会清空个人数据。
- 保留 TV 遥控器方向键、白框放大与 OK 操作；增加实验功能分割线、说明及 GitHub／Issues 地址。
- README 简化为下载、使用和全部版本说明；完整手动过程及实验原理移入独立文档。

## 下载

`Z7X-ADB.apk` 与 `Z7X-ADB-v1.2.0.apk` 内容相同；前者用于 README 的 latest 固定下载链接。使用与旧版相同的发布签名，可覆盖安装 v1.0.0 / v1.1.0，保留设置。提供 SHA256SUMS.txt 校验文件。

## 验证与限制

普通开启流程、连接命令、遥控器页面和确认弹窗已在 Z7X 高亮版 Android 9 验证。发布 APK 使用 GitHub Actions 的 Release 构建产物并在本地签名。

实验性关闭／U 盘恢复和框架重启后的授权尚未完成实机闭环验证；NEW Z6X 及其他 ROM 未由本项目验证。不要将名称变化理解为全机型兼容。仅在 U 盘受影响或电脑提示 unauthorized 时使用对应实验按钮。

详见 [实验功能说明](https://github.com/code-ant/z7x-adb/blob/v1.2.0/docs/experimental-features.md)。
