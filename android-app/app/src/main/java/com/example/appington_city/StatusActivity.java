package com.example.appington_city;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

/** Live CPU, RAM, disk and battery of the PC (refreshes every 2 seconds). */
public class StatusActivity extends AppCompatActivity {

    private static final int REFRESH_MS = 2000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int generation = 0;

    private TextView statusText, cpuValue, ramValue, diskValue, batteryValue;
    private ProgressBar cpuBar, ramBar, diskBar, batteryBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        statusText = findViewById(R.id.statusText);
        cpuValue = findViewById(R.id.cpuValue);
        cpuBar = findViewById(R.id.cpuBar);
        ramValue = findViewById(R.id.ramValue);
        ramBar = findViewById(R.id.ramBar);
        diskValue = findViewById(R.id.diskValue);
        diskBar = findViewById(R.id.diskBar);
        batteryValue = findViewById(R.id.batteryValue);
        batteryBar = findViewById(R.id.batteryBar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        generation++;
        fetchStatus(generation);
    }

    @Override
    protected void onPause() {
        super.onPause();
        generation++;
        handler.removeCallbacksAndMessages(null);
    }

    private void fetchStatus(final int token) {
        Api.get(this, "/status", (ok, code, body) -> {
            if (token != generation) return;
            if (ok) {
                try {
                    showStatus(new JSONObject(body));
                } catch (JSONException e) {
                    showOffline();
                }
            } else {
                showOffline();
            }
            handler.postDelayed(() -> {
                if (token == generation) fetchStatus(token);
            }, REFRESH_MS);
        });
    }

    private void showStatus(JSONObject s) {
        int minutes = s.optInt("uptime_minutes", 0);
        String info = s.optString("hostname", "PC") + "  |  " + s.optString("os", "")
                + "  |  Uptime: " + (minutes / 60) + " h " + (minutes % 60) + " min";
        statusText.setText(info);
        statusText.setTextColor(color(R.color.ok_green));

        int cpu = (int) Math.round(s.optDouble("cpu", 0));
        int ram = (int) Math.round(s.optDouble("ram", 0));
        int disk = (int) Math.round(s.optDouble("disk", 0));
        setBar(cpuBar, cpuValue, cpu, cpu + "%", highIsBad(cpu));
        setBar(ramBar, ramValue, ram, ram + "%", highIsBad(ram));
        setBar(diskBar, diskValue, disk, disk + "%", highIsBad(disk));

        if (s.isNull("battery")) {
            setBar(batteryBar, batteryValue, 0, "No battery", color(R.color.bar_track));
        } else {
            int battery = (int) Math.round(s.optDouble("battery", 0));
            boolean charging = s.optBoolean("charging", false);
            String text = battery + "%" + (charging ? "  (charging)" : "");
            int tint = charging ? color(R.color.ok_green) : lowIsBad(battery);
            setBar(batteryBar, batteryValue, battery, text, tint);
        }
    }

    private void showOffline() {
        statusText.setText("Offline - check the IP in Settings and that the PC server is running");
        statusText.setTextColor(color(R.color.danger_red));
        for (TextView v : new TextView[]{cpuValue, ramValue, diskValue, batteryValue}) v.setText("--");
        for (ProgressBar b : new ProgressBar[]{cpuBar, ramBar, diskBar, batteryBar}) b.setProgress(0);
    }

    private void setBar(ProgressBar bar, TextView label, int percent, String text, int tint) {
        label.setText(text);
        bar.setProgress(Math.max(0, Math.min(100, percent)));
        bar.setProgressTintList(ColorStateList.valueOf(tint));
    }

    private int highIsBad(int percent) {
        if (percent >= 90) return color(R.color.danger_red);
        if (percent >= 70) return color(R.color.warn_amber);
        return color(R.color.ok_green);
    }

    private int lowIsBad(int percent) {
        if (percent <= 20) return color(R.color.danger_red);
        if (percent <= 40) return color(R.color.warn_amber);
        return color(R.color.ok_green);
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }
}
