package local.z7x.adb;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.*;

public final class MainActivity extends Activity {
    private TextView status;
    private Button run, boot;
    private boolean busy;
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        return view;
    }
    private GradientDrawable background(int color, boolean outlined) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(12));
        d.setStroke(dp(3), outlined ? Color.WHITE : Color.TRANSPARENT);
        return d;
    }
    private Button tvButton(int id) {
        Button b = new Button(this);
        b.setId(id); b.setTextSize(20); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setPadding(dp(20), dp(8), dp(20), dp(8));
        b.setFocusable(true); b.setFocusableInTouchMode(true);
        b.setBackgroundTintList(null); b.setStateListAnimator(null);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, background(Color.rgb(55,92,143),true));
        states.addState(new int[]{android.R.attr.state_focused}, background(Color.rgb(38,65,100),true));
        states.addState(new int[]{}, background(Color.rgb(29,41,58),false));
        b.setBackground(states);
        b.setOnFocusChangeListener((v, focused) -> {
            v.animate().cancel();
            v.animate().scaleX(focused ? 1.045f : 1f).scaleY(focused ? 1.045f : 1f).setDuration(140).start();
        });
        // Native Button maps DPAD_CENTER and ENTER to performClick().
        return b;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(40),dp(24),dp(40),dp(20));
        box.setClipChildren(false); box.setClipToPadding(false);
        box.setBackgroundColor(Color.rgb(17,24,39));
        box.addView(text("Z7X ADB 开启器",28,Color.WHITE));
        box.addView(text("打开应用即自动开启 ADB · 无需点击\n方向键选择，白框表示焦点，按 OK 确认",18,Color.LTGRAY));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setClipChildren(false); actions.setClipToPadding(false);
        actions.setPadding(dp(8),dp(18),dp(8),dp(18));
        run = tvButton(View.generateViewId()); run.setText("重新开启并检查");
        boot = tvButton(View.generateViewId()); updateBootLabel();
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0,dp(68),1);
        left.setMargins(0,0,dp(24),0); actions.addView(run,left);
        actions.addView(boot,new LinearLayout.LayoutParams(0,dp(68),1)); box.addView(actions);
        for (Button b : new Button[]{run,boot}) {
            int other = b == run ? boot.getId() : run.getId();
            b.setNextFocusLeftId(other); b.setNextFocusRightId(other);
            b.setNextFocusUpId(other); b.setNextFocusDownId(other);
        }
        run.setOnClickListener(v -> enable());
        boot.setOnClickListener(v -> {
            boolean enabled = !getSharedPreferences("settings",0).getBoolean("boot",true);
            getSharedPreferences("settings",0).edit().putBoolean("boot",enabled).apply();
            updateBootLabel();
        });
        box.addView(text("关闭开机恢复只停止应用的自动尝试，已写入的 ADB 配置仍会保留。\n开启网络 ADB 后，局域网内的电脑可尝试连接投影仪进行调试。",15,Color.LTGRAY));
        status = text("正在设置并验证……",18,Color.WHITE); status.setPadding(0,dp(12),0,0);
        ScrollView scroll = new ScrollView(this); scroll.setFocusable(false);
        scroll.setDescendantFocusability(ScrollView.FOCUS_BLOCK_DESCENDANTS);
        scroll.addView(status); box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(box); run.requestFocus();
        int delay = Math.max(0,Math.min(10000,getIntent().getIntExtra("verification_delay_ms",0)));
        if (delay == 0) enable();
        else new android.os.Handler().postDelayed(() -> { if (!isDestroyed()) enable(); },delay);
    }
    private void updateBootLabel() {
        boolean enabled = getSharedPreferences("settings",0).getBoolean("boot",true);
        boot.setText("开机自动恢复：" + (enabled ? "已开启" : "已关闭"));
    }
    private void enable() {
        if (busy) return;
        busy = true;
        // Keep the control enabled to preserve remote focus during background work.
        run.setText("正在检查……"); status.setText("正在设置并验证……");
        new Thread(() -> {
            String result = AdbController.enable(); AdbController.save(this,result);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                status.setText(result); busy=false; run.setText("重新开启并检查");
            });
        },"enable-adb").start();
    }
}
