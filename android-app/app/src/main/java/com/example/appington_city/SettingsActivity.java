package com.example.appington_city;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/** Dark mode, joystick sensitivity and choosing which PC to control. */
public class SettingsActivity extends AppCompatActivity {

    private LinearLayout deviceList;
    private TextView deviceStatus;
    private TextView currentPc;
    private ImageButton refreshButton;
    private boolean scanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        deviceList = findViewById(R.id.deviceListLayout);
        deviceStatus = findViewById(R.id.deviceStatusText);
        currentPc = findViewById(R.id.currentPcText);
        refreshButton = findViewById(R.id.refreshButton);

        SwitchCompat darkSwitch = findViewById(R.id.darkModeSwitch);
        darkSwitch.setChecked(Prefs.isDarkMode(this));
        darkSwitch.setOnCheckedChangeListener((button, checked) -> {
            Prefs.setDarkMode(this, checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        SeekBar sensitivity = findViewById(R.id.sensitivitySeekBar);
        sensitivity.setProgress(Prefs.getSensitivity(this));
        sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) Prefs.setSensitivity(SettingsActivity.this, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });

        SeekBar deadZone = findViewById(R.id.deadZoneSeekBar);
        deadZone.setProgress(Prefs.getDeadZone(this));
        deadZone.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) Prefs.setDeadZone(SettingsActivity.this, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });

        refreshButton.setOnClickListener(v -> startNetworkScanning());
        findViewById(R.id.manageIpButton).setOnClickListener(v -> showIpActions());

        startNetworkScanning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentPc();
    }

    private void updateCurrentPc() {
        String ip = Prefs.getIp(this);
        currentPc.setText(ip.isEmpty() ? "No PC selected yet" : "Connected PC: " + ip);
    }

    // ---------------- scanning ----------------

    private void startNetworkScanning() {
        if (scanning) return;
        scanning = true;
        refreshButton.setEnabled(false);
        refreshButton.setAlpha(0.5f);
        deviceList.removeAllViews();
        deviceStatus.setText("Scanning for PCs on your Wi-Fi...");

        NetworkScanner.scan(this, new NetworkScanner.Listener() {
            @Override
            public void onDevice(Device device) {
                deviceStatus.setText("Found Devices:");
                addDeviceButton(device);
            }

            @Override
            public void onDone() {
                scanning = false;
                refreshButton.setEnabled(true);
                refreshButton.setAlpha(1f);
                if (deviceList.getChildCount() == 0) {
                    deviceStatus.setText("No devices found.");
                }
            }
        });
    }

    private void addDeviceButton(final Device device) {
        MaterialButton button = new MaterialButton(this);
        button.setAllCaps(false);
        button.setText(device.name + "  (" + device.ip + ")");
        button.setTextColor(ContextCompat.getColor(this, R.color.white));
        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.tile_indigo));
        button.setOnClickListener(v -> Connector.connect(this, device.ip, connected -> updateCurrentPc()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        deviceList.addView(button, lp);
    }

    // ---------------- manage IPs ----------------

    private void showIpActions() {
        final String[] items = {"Scan QR Code", "Manual Input", "IP List"};
        new AlertDialog.Builder(this)
                .setTitle("IP Actions")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, QRScannerActivity.class));
                    } else if (which == 1) {
                        showManualInputDialog();
                    } else {
                        showIpList();
                    }
                })
                .show();
    }

    private void showManualInputDialog() {
        final EditText input = new EditText(this);
        input.setHint("e.g. 192.168.1.20");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        FrameLayout box = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Manual Input")
                .setView(box)
                .setPositiveButton("OK", (dialog, which) -> {
                    String ip = Prefs.parseIp(input.getText().toString());
                    if (ip.isEmpty()) {
                        Toast.makeText(this, "Enter an IP address", Toast.LENGTH_SHORT).show();
                    } else {
                        Connector.connect(this, ip, connected -> updateCurrentPc());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showIpList() {
        final List<String> ips = Prefs.getIpList(this);
        if (ips.isEmpty()) {
            Toast.makeText(this, "No IP addresses found", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("IP List")
                .setItems(ips.toArray(new String[0]), (dialog, which) -> showIpItemActions(ips.get(which)))
                .show();
    }

    private void showIpItemActions(final String ip) {
        final String[] items = {"Use this IP", "Delete IP Address"};
        new AlertDialog.Builder(this)
                .setTitle(ip)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        Connector.connect(this, ip, connected -> updateCurrentPc());
                    } else {
                        Prefs.deleteIp(this, ip);
                        updateCurrentPc();
                        Toast.makeText(this, "IP Address deleted: " + ip, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
