package com.example.appington_city;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** Lock, sleep, restart, shut down and cancel. Power actions ask for confirmation first. */
public class QuickActionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_actions);

        findViewById(R.id.lockButton).setOnClickListener(v -> send("/lock", "PC locked"));
        findViewById(R.id.sleepButton).setOnClickListener(v ->
                confirm("Put the PC to sleep?", "/sleep", "PC going to sleep"));
        findViewById(R.id.restartButton).setOnClickListener(v ->
                confirm("Restart the PC in 5 seconds?", "/restart", "Restarting in 5 seconds"));
        findViewById(R.id.shutdownButton).setOnClickListener(v ->
                confirm("Shut down the PC in 5 seconds?", "/shutdown", "Shutting down in 5 seconds"));
        findViewById(R.id.cancelButton).setOnClickListener(v -> send("/cancel", "Cancelled"));
    }

    private void confirm(String message, final String path, final String successMessage) {
        new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> send(path, successMessage))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void send(String path, final String successMessage) {
        Api.post(this, path, "", (ok, code, body) -> {
            if (ok) {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
            } else if (code > 0) {
                Toast.makeText(this, "PC returned an error", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, Prefs.getIp(this).isEmpty()
                        ? "No PC selected - open Settings first"
                        : "Could not reach the PC", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
