package local.z7x.adb;

import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

public final class AdbController {
    public static String disable(android.content.Context context) {
        StringBuilder report = new StringBuilder();
        try {
            // This firmware explicitly defines mtp as its USB host default.
            String init = run("/system/bin/cat", "/vendor/etc/init/hw/init.m7642.usb.rc");
            if (!init.contains("set mtp as default to being host mode"))
                throw new IOException("未确认此固件的默认 USB 模式，未修改配置。");
            if (!context.getSharedPreferences("settings",0).edit()
                    .putBoolean("manual_only",true).putBoolean("boot",false).commit())
                throw new IOException("无法保存手动开启模式，未修改配置。");
            set("persist.adb.tcp.port","-1",report);
            set("service.adb.tcp.port","-1",report);
            set("persist.sys.usb.config","none",report);
            set("sys.usb.config","none",report);
            run("/system/bin/setprop","ctl.stop","adbd");
            set("persist.sys.usb.config","mtp",report);
            set("sys.usb.config","mtp",report);
            boolean stopped = false;
            for (int i=0;i<10;i++) {
                if ("stopped".equals(prop("init.svc.adbd"))) { stopped=true; break; }
                Thread.sleep(300);
            }
            if (!stopped) throw new IOException("USB 配置已设置，但尚未确认 ADB 停止。");
            report.insert(0,"ADB 已关闭，已切换到固件定义的默认 USB 模式。\n请重新插入 U 盘检查；若仍未识别，请重启投影仪。\n需要 ADB 时，请手动点击“重新开启并检查”。\n\n");
        } catch (Exception e) {
            report.insert(0,"关闭操作未完成：" + e.getMessage() + "\n部分设置可能已更改，请查看下方结果。\n\n");
        }
        try { report.append("adbd: ").append(prop("init.svc.adbd")); }
        catch (Exception e) { report.append("adbd: 状态未知"); }
        return report.toString();
    }
    public static void save(android.content.Context context, String text) {
        try {
            java.io.File dir = context.getExternalFilesDir(null);
            if (dir != null) try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir,"last-result.txt"))) { out.write(text.getBytes("UTF-8")); }
        } catch (Exception e) { Log.w("Z7xAdb", "保存诊断失败", e); }
    }
    public static String run(String... command) throws Exception {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!p.waitFor(4, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new IOException("命令超时"); }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[1024]; int n;
        while ((n = p.getInputStream().read(b)) != -1) out.write(b, 0, n);
        String s = out.toString("UTF-8").trim();
        if (p.exitValue() != 0) throw new IOException("退出码 " + p.exitValue() + ": " + s);
        return s;
    }
    static String prop(String key) throws Exception { return run("/system/bin/getprop", key); }
    private static String connectionCommands() {
        java.util.List<String> lan = new java.util.ArrayList<>();
        java.util.List<String> other = new java.util.ArrayList<>();
        try {
            java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback()) continue;
                java.util.Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()
                            || address.isLinkLocalAddress() || address.isAnyLocalAddress()) continue;
                    String command = "adb connect " + address.getHostAddress() + ":5555";
                    String name = network.getName();
                    if (name.startsWith("wlan") || name.startsWith("eth")) lan.add(command);
                    else other.add(command);
                }
            }
        } catch (SocketException e) { Log.w("Z7xAdb", "读取网络地址失败", e); }
        java.util.List<String> commands = lan.isEmpty() ? other : lan;
        return commands.isEmpty() ? "ADB 服务已开启，但未获取到 IPv4 地址，请连接 Wi-Fi 或有线网络后重新检查。"
                : "在电脑上执行以下命令连接：\n"
                    + android.text.TextUtils.join("\n", commands)
                    + "\n\n执行 adb devices 查看连接结果。\n"
                    + "如果无法连接且提示 unauthorized（未授权），请点击下方“不能连接ADB点这里”按钮。";
    }
    static void set(String key, String value, StringBuilder report) throws Exception {
        run("/system/bin/setprop", key, value);
        String actual = prop(key);
        if (!value.equals(actual)) throw new IOException(key + " 写入未生效: " + actual);
        Log.i("Z7xAdb", key + " = " + actual);
    }
    public static String restartFramework(android.content.Context context) {
        StringBuilder report = new StringBuilder();
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("settings",0);
            if (prefs.getBoolean("framework_restart_attempted",false))
                throw new IOException("已经尝试过框架重启，请在电脑检查连接结果。");
            // Require verified configuration before making a disruptive restart request.
            if (!"adb".equals(prop("persist.sys.usb.config")) ||
                !"adb".equals(prop("sys.usb.config")) ||
                !"5555".equals(prop("service.adb.tcp.port")) ||
                !"running".equals(prop("init.svc.adbd")))
                throw new IOException("ADB 配置或服务尚未就绪，请先执行“重新开启并检查”。");
            if (!"running".equals(prop("init.svc.zygote")))
                throw new IOException("未确认 zygote 正在运行，未请求重启。");
            // Persist before the command: a successful restart can kill this process immediately.
            if (!prefs.edit().putBoolean("framework_restart_attempted",true).commit())
                throw new IOException("无法保存尝试记录，未请求重启。");
            save(context,"已记录实验性框架重启请求；请在电脑验证授权，记录本身不表示成功。");
            run("/system/bin/setprop","ctl.restart","zygote");
            report.append("框架重启请求已提交，不能据此确认重启或授权成功。\n返回桌面后，请在电脑重新连接 ADB 并执行 shell 命令验证。");
        } catch (Exception e) {
            report.append("实验性操作未完成：").append(e.getMessage());
        }
        return report.toString();
    }
    public static String enable() {
        StringBuilder r = new StringBuilder();
        try {
            set("persist.adb.tcp.port", "5555", r);
            set("service.adb.tcp.port", "5555", r);
            set("persist.sys.usb.config", "adb", r);
            set("sys.usb.config", "adb", r);
            // Starting an already running service is harmless; do not stop the current connection.
            try { run("/system/bin/setprop", "ctl.start", "adbd"); }
            catch (Exception e) { r.append("直接启动: ").append(e.getMessage()).append('\n'); }
            boolean ok = false;
            for (int i = 0; i < 8; i++) {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress("127.0.0.1", 5555), 500);
                    ok = "running".equals(prop("init.svc.adbd"));
                } catch (Exception ignored) {}
                if (ok) break;
                Thread.sleep(400);
            }
            r.insert(0, ok ? connectionCommands() + "\n\n" : "属性已设置，但尚未确认 5555 端口可连接\n\n");
            r.append("adbd: ").append(prop("init.svc.adbd"));
        } catch (Exception e) {
            r.insert(0, "开启未完成\n\n");
            r.append("\n").append(e.getMessage()).append("\n此固件可能限制普通应用设置系统属性。");
        }
        Log.i("Z7xAdb", r.toString());
        return r.toString();
    }
}
