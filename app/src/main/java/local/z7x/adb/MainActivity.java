package local.z7x.adb;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.Color;

public final class MainActivity extends Activity {
    TextView status; Button run; boolean busy;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout box = new LinearLayout(this); box.setOrientation(1); box.setPadding(48, 28, 48, 24);
        box.setBackgroundColor(Color.rgb(17, 24, 39));
        TextView title = new TextView(this); title.setText("Z7X ADB 开启器"); title.setTextSize(28); title.setTextColor(Color.WHITE); box.addView(title);
        TextView hint = new TextView(this); hint.setText("打开应用即自动配置网络 ADB（5555）。仅适配已验证的 Z7X 固件。\n开启后，局域网内的电脑可尝试连接投影仪进行调试。"); hint.setTextSize(17); hint.setTextColor(Color.LTGRAY); box.addView(hint);
        run = new Button(this); run.setText("重新开启并检查"); box.addView(run); run.setOnClickListener(v -> enable());
        Switch boot = new Switch(this); boot.setText("开机自动尝试恢复 ADB"); boot.setTextSize(18); boot.setTextColor(Color.WHITE);
        boot.setChecked(getSharedPreferences("settings",0).getBoolean("boot", true));
        boot.setOnCheckedChangeListener((v, checked) -> getSharedPreferences("settings",0).edit().putBoolean("boot",checked).apply()); box.addView(boot);
        TextView note = new TextView(this); note.setText("首次安装后需要打开一次。关闭上方选项仅停止应用的开机恢复；已经写入的持久化 ADB 配置仍会保留。"); note.setTextColor(Color.LTGRAY); note.setTextSize(15); box.addView(note);
        ScrollView scroll = new ScrollView(this); status = new TextView(this); status.setTextSize(18); status.setTextColor(Color.WHITE); status.setPadding(0,20,0,0); scroll.addView(status); box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(box); run.requestFocus();
        int delay = Math.max(0,Math.min(10000,getIntent().getIntExtra("verification_delay_ms",0)));
        if (delay == 0) enable();
        else { status.setText("等待断开恢复验证……"); new android.os.Handler().postDelayed(() -> { if (!isDestroyed()) enable(); },delay); }
    }
    void enable() {
        if (busy) return; busy=true; run.setEnabled(false); status.setText("正在设置并验证……");
        new Thread(() -> { String result=AdbController.enable(); AdbController.save(this,result); runOnUiThread(() -> { if (isDestroyed()) return; status.setText(result); busy=false; run.setEnabled(true); }); },"enable-adb").start();
    }
}
