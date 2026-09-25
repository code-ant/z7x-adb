package local.z7x.adb;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || !context.getSharedPreferences("settings",0).getBoolean("boot",true)) return;
        final PendingResult pending = goAsync();
        new Thread(() -> { try { AdbController.save(context,AdbController.enable()); } finally { pending.finish(); } },"boot-adb").start();
    }
}
