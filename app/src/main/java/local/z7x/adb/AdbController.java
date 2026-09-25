package local.z7x.adb;

import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

public final class AdbController {
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
    static void set(String key, String value, StringBuilder report) throws Exception {
        run("/system/bin/setprop", key, value);
        String actual = prop(key);
        if (!value.equals(actual)) throw new IOException(key + " 写入未生效: " + actual);
        report.append(key).append(" = ").append(actual).append('\n');
    }
    public static String enable() {
        StringBuilder r = new StringBuilder("应用 UID: " + android.os.Process.myUid() + "\n");
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
            r.insert(0, ok ? "ADB 已开启：本机 5555 端口可连接\n\n" : "属性已设置，但尚未确认 5555 端口可连接\n\n");
            r.append("adbd: ").append(prop("init.svc.adbd"));
        } catch (Exception e) {
            r.insert(0, "开启未完成\n\n");
            r.append("\n").append(e.getMessage()).append("\n此固件可能限制普通应用设置系统属性。");
        }
        Log.i("Z7xAdb", r.toString());
        return r.toString();
    }
}
