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
import java.io.DataOutputStream;

public class MacroService extends Service {
    private WindowManager wm;
    private Button btn;
    private boolean visible = false;
    private boolean isHolding = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private float x, y; // Coordenadas onde o botão está na tela

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createButton();
    }

    private void createButton() {
        btn = new Button(this);
        btn.setText("KICK");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(18);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#8000FF00"));
        bg.setStroke(3, Color.GREEN);
        btn.setBackground(bg);
        btn.setAlpha(0f); // Começa invisível

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                250, 250,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 200; // Posição inicial
        params.y = 800;

        btn.setOnTouchListener(new View.OnTouchListener() {
            float initX, initY;
            float initTouchX, initTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x;
                        initY = params.y;
                        initTouchX = event.getRawX();
                        initTouchY = event.getRawY();
                        if (btn.getAlpha() == 0f) {
                            isHolding = true;
                            handler.postDelayed(holdRunnable, 300);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initX + (int) (event.getRawX() - initTouchX);
                        params.y = initY + (int) (event.getRawY() - initTouchY);
                        wm.updateViewLayout(btn, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(holdRunnable);
                        if (isHolding) { isHolding = false; }
                        else { triggerKick(); }
                        return true;
                }
                return false;
            }
        });

        // Listener para 4 dedos esconderem/mostrarem
        btn.setOnLongClickListener(v -> {
            visible = !visible;
            btn.setAlpha(visible ? 1f : 0f);
            return true;
        });

        wm.addView(btn, params);
    }

    private Runnable holdRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHolding) {
                triggerKick();
                handler.postDelayed(this, 50); // Loop rápido
            }
        }
    };

    private void triggerKick() {
        // Executa o comando de toque na tela com root
        new Thread(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(su.getOutputStream());
                // Simula o toque na coordenada central do botão
                int tapX = (int)(btn.getX() + btn.getWidth() / 2);
                int tapY = (int)(btn.getY() + btn.getHeight() / 2);
                os.writeBytes("input tap " + tapX + " " + tapY + "\n");
                os.flush();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btn != null) wm.removeView(btn);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
