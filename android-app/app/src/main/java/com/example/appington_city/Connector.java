package com.example.appington_city;

import android.app.Activity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/** Connects to a PC: checks the password (if the server has one) and saves the IP. */
public final class Connector {

    public interface Done {
        void onDone(boolean connected);
    }

    private Connector() {}

    public static void connect(final Activity act, final String ip, final Done done) {
        Api.postTo(ip, "/password", "", (ok, code, body) -> {
            if (act.isFinishing() || act.isDestroyed()) return;
            String reply = body == null ? "" : body.trim();
            if (ok && reply.equals("200")) {
                success(act, ip, done);
            } else if (ok && reply.equals("400")) {
                askPassword(act, ip, done);
            } else if (ok) {
                Toast.makeText(act, "Unexpected response from the server", Toast.LENGTH_SHORT).show();
                if (done != null) done.onDone(false);
            } else {
                Toast.makeText(act, "Could not reach the PC", Toast.LENGTH_SHORT).show();
                if (done != null) done.onDone(false);
            }
        });
    }

    private static void success(Activity act, String ip, Done done) {
        Prefs.setIp(act, ip);
        Toast.makeText(act, "Successfully connected", Toast.LENGTH_SHORT).show();
        if (done != null) done.onDone(true);
    }

    private static void askPassword(final Activity act, final String ip, final Done done) {
        final EditText input = new EditText(act);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter Password");

        FrameLayout box = new FrameLayout(act);
        int pad = (int) (24 * act.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        new AlertDialog.Builder(act)
                .setTitle("Password required")
                .setView(box)
                .setPositiveButton("OK", (dialog, which) ->
                        Api.postTo(ip, "/password", input.getText().toString(), (ok, code, body) -> {
                            if (act.isFinishing() || act.isDestroyed()) return;
                            if (ok && body != null && body.trim().equals("200")) {
                                success(act, ip, done);
                            } else {
                                Toast.makeText(act, "Wrong password", Toast.LENGTH_SHORT).show();
                                if (done != null) done.onDone(false);
                            }
                        }))
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (done != null) done.onDone(false);
                })
                .setOnCancelListener(dialog -> {
                    if (done != null) done.onDone(false);
                })
                .show();
    }
}
