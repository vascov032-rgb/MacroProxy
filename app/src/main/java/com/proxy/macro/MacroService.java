package com.proxy.macro;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import java.io.DataOutputStream;

public class MacroService extends Service {
    private WindowManager wm;
    private FrameLayout rootLayout;
    private Button btn;
    private boolean visible = true; 
    private boolean isHolding = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
    }

    private void createOverlay() {
        rootLayout = new FrameLayout(this);
        btn = new Button(this);
        btn.setText("KICK+POWER");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#8000FF00"));
        bg.setStroke(3, Color.GREEN);
        btn.setBackground(bg);

        // CANTO SUPERIOR DIREITO
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(250, 250);
        btnParams.gravity = Gravity.TOP | Gravity.RIGHT;
        btnParams.setMargins(0, 150, 100, 0); 
        rootLayout.addView(btn, btnParams);

        // 4 dedos = Aparece/Desaparece
        rootLayout.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() == 4) {
                visible = !visible;
                btn.setAlpha(visible ? 1f : 0f);
            }
            return false;
        });

        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isHolding = true;
                        handler.postDelayed(holdRunnable, 100); // Se segurar por 100ms ativa o spam
                        return true;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(holdRunnable);
                        if (isHolding) {
                            isHolding = false;
                            if (event.getEventTime() - event.getDownTime() < 100) {
                                sendCommand(); // Clique rápido = 1 Kick+Power
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        int layoutType = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        wm.addView(rootLayout, params);
    }

    private Runnable holdRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHolding) {
                sendCommand(); // Spam de Kick+Power
                handler.postDelayed(this, 10); 
            }
        }
    };

    private void sendCommand() {
        new Thread(() -> {
            try {
                // O APP NÃO FAZ MAIS "input tap". Ele só manda o comando pro espião!
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(su.getOutputStream());
                os.writeBytes("echo -n 1 > /data/local/tmp/mamoball_cmd\n");
                os.flush();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootLayout != null) wm.removeView(rootLayout);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
