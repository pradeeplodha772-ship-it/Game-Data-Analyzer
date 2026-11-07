package com.amuria.gamedata;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingControlService extends Service {
    private static final String CHANNEL = "pr_network_floating";
    private WindowManager windowManager;
    private View floatingView;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification notification = builder.setContentTitle("P.R Network Intelligence")
                .setContentText("Floating controls are active")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pending).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(41, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else startForeground(41, notification);
        showFloatingView();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "P.R Floating Controls", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void showFloatingView() {
        if (Build.VERSION.SDK_INT < 23 || !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf(); return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(18, 14, 18, 14);
        panel.setBackgroundColor(0xF0141928);

        TextView heading = new TextView(this);
        heading.setText("◉  P.R NETWORK INTELLIGENCE");
        heading.setTextColor(0xFFB68CFF);
        heading.setTextSize(13);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(heading);

        TextView status = new TextView(this);
        status.setText("FLOATING CONTROLS ACTIVE\nCapture engine: not connected");
        status.setTextColor(0xFFE6EDF3);
        status.setTextSize(11);
        status.setPadding(0, 10, 0, 10);
        panel.addView(status);

        LinearLayout actions = new LinearLayout(this);
        Button open = new Button(this);
        open.setText("OPEN APP");
        open.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
        Button close = new Button(this);
        close.setText("CLOSE");
        close.setOnClickListener(v -> stopSelf());
        actions.addView(open, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(close, new LinearLayout.LayoutParams(0, -2, 1));
        panel.addView(actions);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.82),
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 180;

        heading.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x; startY = params.y;
                    touchX = event.getRawX(); touchY = event.getRawY(); return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = startX + (int)(event.getRawX() - touchX);
                    params.y = startY + (int)(event.getRawY() - touchY);
                    windowManager.updateViewLayout(panel, params); return true;
                }
                return true;
            }
        });
        floatingView = panel;
        windowManager.addView(floatingView, params);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (floatingView == null) showFloatingView();
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        if (windowManager != null && floatingView != null) {
            try { windowManager.removeView(floatingView); } catch (Exception ignored) {}
        }
        floatingView = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
