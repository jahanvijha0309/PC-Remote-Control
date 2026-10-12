package com.example.appington_city;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        open(R.id.mediaButton, MediaActivity.class);
        open(R.id.freeButton, PCActivity.class);
        open(R.id.filesButton, FilesActivity.class);
        open(R.id.touchpadButton, TouchpadActivity.class);
        open(R.id.screenButton, ScreenActivity.class);
        open(R.id.shortcutsButton, ShortcutsActivity.class);
        open(R.id.settingsButton, SettingsActivity.class);
        open(R.id.statusButton, StatusActivity.class);
        open(R.id.actionsButton, QuickActionsActivity.class);

        if (Prefs.getIp(this).isEmpty()) {
            Toast.makeText(this, "Open Settings to connect to your PC", Toast.LENGTH_LONG).show();
        }
    }

    private void open(int viewId, final Class<?> target) {
        findViewById(viewId).setOnClickListener(v -> startActivity(new Intent(this, target)));
    }
}
