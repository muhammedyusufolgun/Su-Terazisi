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
    private TextView title;
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
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff181a18);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                         insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        title = new TextView(this);
        title.setText("Su Terazisi");
        title.setTextSize(22);
        title.setTextColor(0xfff3f5ed);
        title.setGravity(Gravity.CENTER);
        title.setMinHeight(dp(48));
        title.setContentDescription("Su Terazisi, referans ayarları");
        title.setOnClickListener(v -> referenceDialog());
        header.addView(title, -1, -2);
        TextView angles = new TextView(this);
        angles.setTextSize(17);
        angles.setGravity(Gravity.CENTER);
        angles.setTextColor(0xffdeedab);
        header.addView(angles, -1, -2);
        instrument = new Instrument(this, angles);
        root.addView(instrument, -1, -1);
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);

        root.addView(header, headerParams);
        TextView signature = new TextView(this);
        signature.setText("Muhammed Yusuf OLGUN");
        signature.setGravity(Gravity.CENTER);
        signature.setTextColor(0xffcccccc);
        signature.setTextSize(14);
        signature.setSingleLine(true);
        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(signature, footerParams);
        instrument.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            int edge = Math.round(instrument.edgeWidth()*2/3) + dp(4);
            int labelHeight = title.getMinHeight() + angles.getLineHeight() + dp(8);
            float radius = instrument.fitCenter(labelHeight);
            int inset = Math.round(instrument.sideWidth()) + dp(7);
            int bandHeight = Math.max(1, Math.round((b-t)/2f-radius)-edge);
            headerParams.height = bandHeight;
            headerParams.setMargins(inset, edge, inset, 0);
            header.setLayoutParams(headerParams);
            float space = Math.max(1, r-l-2*inset);
            signature.setTextScaleX(1);
            float textWidth = signature.getPaint().measureText(signature.getText().toString());
            signature.setTextScaleX(Math.min(1, space/textWidth));
            footerParams.height = bandHeight;
            footerParams.setMargins(inset, 0, inset, edge);
            signature.setLayoutParams(footerParams);
        });
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
        title.setEnabled(false);
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
            title.setEnabled(false);
            instrument.status("Telefonu sabit tutun");
            return;
        }
        rawX = (float)Math.toDegrees(Math.atan2(gx, Math.hypot(gy, gz)));
        rawY = (float)Math.toDegrees(Math.atan2(gy, gz));
        title.setEnabled(true);
        displayAngles();
    }
    private void displayAngles() {
        float x = rawX-zeroX, y = (rawY-zeroY+540f)%360f-180f;
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90: instrument.measure(-y, x); break;
            case Surface.ROTATION_180: instrument.measure(-x, -y); break;
            case Surface.ROTATION_270: instrument.measure(y, -x); break;
            default: instrument.measure(x, y);
        }
    }
}
