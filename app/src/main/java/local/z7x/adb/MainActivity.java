package local.z7x.adb;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.*;

public final class MainActivity extends Activity {
    private TextView status;
    private Button run, boot, troubleshoot, closeAdb;
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
        box.addView(text("极米 ADB 开启器",28,Color.WHITE));
        box.addView(text("默认打开应用即开启 ADB；恢复 USB 后需手动开启\n方向键选择，白框表示焦点，按 OK 确认",18,Color.LTGRAY));
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
            if (busy) return;
            if (getSharedPreferences("settings",0).getBoolean("manual_only",false)) {
                status.setText("当前为手动开启模式。需要 ADB 时，请先点击“重新开启并检查”，再设置开机恢复。");
                return;
            }
            boolean enabled = !getSharedPreferences("settings",0).getBoolean("boot",true);
            getSharedPreferences("settings",0).edit().putBoolean("boot",enabled).apply();
            updateBootLabel();
        });
        box.addView(text("关闭开机恢复只停止应用的自动尝试，已写入的 ADB 配置仍会保留。\n开启网络 ADB 后，局域网内的电脑可尝试连接投影仪进行调试。",15,Color.LTGRAY));
        status = text("正在设置并验证……",18,Color.WHITE); status.setPadding(0,dp(12),0,0);
        ScrollView scroll = new ScrollView(this); scroll.setFocusable(false);
        scroll.setDescendantFocusability(ScrollView.FOCUS_BLOCK_DESCENDANTS);
        scroll.addView(status); box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView projectLink = text("GitHub 项目：https://github.com/code-ant/z7x-adb\n如有问题，欢迎到项目的 Issues 页面提交反馈。",15,Color.LTGRAY);
        projectLink.setPadding(0,dp(8),0,0);
        box.addView(projectLink);
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(75,85,99));
        LinearLayout.LayoutParams dividerLayout = new LinearLayout.LayoutParams(-1,dp(1));
        dividerLayout.setMargins(0,dp(10),0,dp(10));
        box.addView(divider,dividerLayout);
        box.addView(text("以下功能为实验性功能 · 仅在遇到对应问题时使用",17,Color.rgb(251,191,36)));
        closeAdb = tvButton(View.generateViewId());
        closeAdb.setText("关闭 ADB，恢复默认 USB 设置");
        LinearLayout.LayoutParams closeLayout = new LinearLayout.LayoutParams(-1,dp(56));
        closeLayout.setMargins(dp(8),dp(8),dp(8),dp(6));
        box.addView(closeAdb,closeLayout);
        box.addView(text("仅在 U 盘功能受影响时使用；U 盘正常请勿使用。\n关闭 ADB 并恢复 USB 模式，不清除数据。以后使用 ADB 需手动开启。",15,Color.LTGRAY));
        closeAdb.setOnClickListener(v -> confirmDisable());
        troubleshoot = tvButton(View.generateViewId());
        troubleshoot.setText("不能连接ADB点这里");
        LinearLayout.LayoutParams troubleshootingLayout = new LinearLayout.LayoutParams(-1,dp(60));
        troubleshootingLayout.setMargins(dp(8),dp(12),dp(8),dp(8));
        box.addView(troubleshoot,troubleshootingLayout);
        box.addView(text("实验性功能：仅当电脑提示 unauthorized（未授权）时使用。\n将重启 Android 框架并退出所有应用；不适用于所有 ROM，不能保证授权成功。",15,Color.LTGRAY));
        run.setNextFocusDownId(closeAdb.getId());
        boot.setNextFocusDownId(closeAdb.getId());
        closeAdb.setNextFocusUpId(run.getId());
        closeAdb.setNextFocusDownId(troubleshoot.getId());
        troubleshoot.setNextFocusUpId(closeAdb.getId());
        troubleshoot.setNextFocusDownId(boot.getId());
        troubleshoot.setOnClickListener(v -> confirmFrameworkRestart());
        setContentView(box); run.requestFocus();
        if (getSharedPreferences("settings",0).getBoolean("manual_only",false)) {
            status.setText("当前为手动开启模式。需要 ADB 时，请点击“重新开启并检查”。");
            return;
        }
        int delay = Math.max(0,Math.min(10000,getIntent().getIntExtra("verification_delay_ms",0)));
        if (delay == 0) enable();
        else new android.os.Handler().postDelayed(() -> { if (!isDestroyed()) enable(); },delay);
    }
    private void updateBootLabel() {
        boolean enabled = getSharedPreferences("settings",0).getBoolean("boot",true);
        boot.setText("开机自动恢复：" + (enabled ? "已开启" : "已关闭"));
    }
    private void confirmDisable() {
        if (busy) return;
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("关闭 ADB，恢复 USB 模式？")
            .setMessage("仅在 U 盘功能受影响时使用；U 盘正常请取消。\n\n此操作会断开电脑 ADB 连接，关闭开机自动恢复，并切换默认 USB 模式，不清除应用或个人数据。\n\n完成后重新插入 U 盘检查，必要时重启投影仪。以后打开本应用不会自动开启 ADB，需要手动点击“重新开启并检查”。")
            .setNegativeButton("取消",null).setPositiveButton("关闭 ADB",(d,w) -> {
                busy=true; status.setText("正在关闭 ADB 并恢复 USB 设置……");
                new Thread(() -> {
                    String result=AdbController.disable(this); AdbController.save(this,result);
                    runOnUiThread(() -> { if (isDestroyed()) return; busy=false; status.setText(result); updateBootLabel(); });
                },"disable-adb").start();
            }).create();
        dialog.show(); dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }
    private void confirmFrameworkRestart() {
        if (busy) return;
        if (getSharedPreferences("settings",0).getBoolean("framework_restart_attempted",false)) {
            new AlertDialog.Builder(this).setTitle("已经尝试过框架重启")
                .setMessage("请在电脑重新连接并检查授权状态。若仍为 unauthorized，请检查 ROM 的授权实现，不要反复重启。")
                .setPositiveButton("知道了",null).show();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("实验性：重启 Android 框架")
            .setMessage("仅在电脑提示 unauthorized 时尝试。应用会先检查 ADB 配置，再请求重启框架；当前所有应用将退出，画面和连接可能暂时中断。\n\n仅部分 ROM 会自动授权。返回桌面后，请在电脑重新连接 ADB。本操作只允许尝试一次。")
            .setNegativeButton("取消",null)
            .setPositiveButton("确认重启",(d,which) -> restartFramework()).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }
    private void restartFramework() {
        if (busy) return;
        busy = true;
        status.setText("正在检查配置，准备请求框架重启……");
        new Thread(() -> {
            String result = AdbController.restartFramework(this);
            AdbController.save(this,result);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                busy = false; status.setText(result);
            });
        },"restart-framework").start();
    }
    private void enable() {
        if (busy) return;
        if (!getSharedPreferences("settings",0).edit().putBoolean("manual_only",false).commit()) {
            status.setText("无法保存开启模式，请重试。"); return;
        }
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
