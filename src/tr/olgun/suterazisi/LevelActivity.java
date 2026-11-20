package tr.olgun.suterazisi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.hardware.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public final class LevelActivity extends Activity implements SensorEventListener {
    private SensorManager manager;
    private Instrument instrument;
    private Button calibrate;
    private SharedPreferences settings;
    private float rawX, rawY, gx, gy, gz, zeroX, zeroY;
    private long previous;

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        settings = getSharedPreferences("reference", MODE_PRIVATE);
        zeroX = settings.getFloat("x", 0);
        zeroY = settings.getFloat("y", 0);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff181a18);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                         insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), 0, dp(8), 0);
        TextView title = new TextView(this);
        title.setText("Su Terazisi");
        title.setTextSize(20);
        title.setTextColor(0xfff3f5ed);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        calibrate = new Button(this, null, android.R.attr.borderlessButtonStyle);
        calibrate.setText("⊕");
        calibrate.setTextSize(29);
        calibrate.setTextColor(0xffc4df6e);
        calibrate.setContentDescription("Referans ayarları");
        calibrate.setOnClickListener(v -> referenceDialog());
        bar.addView(calibrate, dp(52), dp(48));
        root.addView(bar, -1, -2);
        instrument = new Instrument(this);
        root.addView(instrument, -1, -1);
        setContentView(root);
    }

    private void referenceDialog() {
        new AlertDialog.Builder(this).setTitle("Referans ayarı")
            .setMessage("Telefonu sabit tutun. Bu konumu iki eksen için sıfır kabul edebilirsiniz.")
            .setPositiveButton("Sıfırla", (d,w) -> saveZero(rawX, rawY))
            .setNeutralButton("Varsayılana dön", (d,w) -> saveZero(0, 0))
            .setNegativeButton("Vazgeç", null).show();
    }
    private void saveZero(float x, float y) {
        zeroX = x;
        zeroY = y;
        settings.edit().putFloat("x", x).putFloat("y", y).apply();
        displayAngles();
    }
    @Override protected void onResume() {
        super.onResume();
        previous = 0;
        calibrate.setEnabled(false);
        boolean listening = false;
        for (int type : new int[]{Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER}) {
            Sensor sensor = manager.getDefaultSensor(type);
            if (sensor != null && manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)) {
                listening = true;
                break;
            }
        }
        instrument.status(listening ? "Ölçüm bekleniyor…" : "Eğim sensörü kullanılamıyor");
    }
    @Override protected void onPause() { manager.unregisterListener(this); super.onPause(); }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onSensorChanged(SensorEvent event) {
        float gain = previous == 0 ? 1 : (float)(1 - Math.exp(-(event.timestamp - previous) / 105000000.0));
        previous = event.timestamp;
        gx += gain * (event.values[0] - gx);
        gy += gain * (event.values[1] - gy);
        gz += gain * (event.values[2] - gz);
        double length = Math.sqrt(gx*gx + gy*gy + gz*gz);
        if (length < 1) {
            calibrate.setEnabled(false);
            instrument.status("Telefonu sabit tutun");
            return;
        }
        rawX = (float)Math.toDegrees(Math.atan2(gx, Math.hypot(gy, gz)));
        rawY = (float)Math.toDegrees(Math.atan2(gy, Math.hypot(gx, gz)));
        calibrate.setEnabled(true);
        displayAngles();
    }
    private void displayAngles() {
        float x = rawX-zeroX, y = rawY-zeroY;
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90: instrument.measure(-y, x); break;
            case Surface.ROTATION_180: instrument.measure(-x, -y); break;
            case Surface.ROTATION_270: instrument.measure(y, -x); break;
            default: instrument.measure(x, y);
        }
    }
}
