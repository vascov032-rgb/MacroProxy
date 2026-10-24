package com.proxy.macro;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 123);
        } else {
            startService(new Intent(this, MacroService.class));
            finish();
        }
    }
    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == 123) { startService(new Intent(this, MacroService.class)); finish(); }
    }
}
