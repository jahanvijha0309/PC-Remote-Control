package com.example.appington_city;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny HTTP helper for talking to the Flask PC server. Callbacks always run on the UI thread. */
public final class Api {

    public interface Callback {
        /** ok = HTTP 2xx. code = HTTP status, or -1 if the PC could not be reached. */
        void onResult(boolean ok, int code, String body);
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(6);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static long lastErrorToast = 0;

    private Api() {}

    // ---- calls to the PC saved in settings ----

    public static void get(Context c, String path, Callback cb) {
        request(Prefs.getIp(c), "GET", path, null, 4000, cb);
    }

    public static void post(Context c, String path, String body, Callback cb) {
        request(Prefs.getIp(c), "POST", path, body == null ? "" : body, 4000, cb);
    }

    // ---- calls to a specific IP (used while connecting) ----

    public static void getTo(String ip, String path, Callback cb) {
        request(ip, "GET", path, null, 3000, cb);
    }

    public static void postTo(String ip, String path, String body, Callback cb) {
        request(ip, "POST", path, body == null ? "" : body, 3000, cb);
    }

    // ---- fire and forget: only shows a (rate limited) toast if something fails ----

    public static void fireGet(Context c, String path) {
        get(c, path, errorToaster(c));
    }

    public static void firePost(Context c, String path, String body) {
        post(c, path, body, errorToaster(c));
    }

    private static Callback errorToaster(Context c) {
        final Context app = c.getApplicationContext();
        return (ok, code, body) -> {
            if (!ok) toastError(app, code);
        };
    }

    public static void toastError(Context app, int code) {
        long now = System.currentTimeMillis();
        if (now - lastErrorToast < 3000) return;
        lastErrorToast = now;
        String msg;
        if (Prefs.getIp(app).isEmpty()) {
            msg = "No PC selected - open Settings first";
        } else if (code > 0) {
            msg = "PC returned an error";
        } else {
            msg = "Could not reach the PC";
        }
        Toast.makeText(app, msg, Toast.LENGTH_SHORT).show();
    }

    // ---- internals ----

    private static void request(final String ip, final String method, final String path,
                                final String body, final int readTimeout, final Callback cb) {
        POOL.execute(() -> {
            if (ip == null || ip.isEmpty()) {
                deliver(cb, false, -1, "No PC selected");
                return;
            }
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + ip + ":" + Prefs.PORT + path);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(readTimeout);
                conn.setRequestMethod(method);
                if (body != null) {
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
                    conn.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bytes);
                    }
                }
                int code = conn.getResponseCode();
                InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                String response = readAll(stream);
                deliver(cb, code >= 200 && code < 300, code, response);
            } catch (Exception e) {
                deliver(cb, false, -1, String.valueOf(e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    static String readAll(InputStream in) {
        if (in == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[2048];
            int n;
            while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static void deliver(final Callback cb, final boolean ok, final int code, final String body) {
        if (cb == null) return;
        MAIN.post(() -> cb.onResult(ok, code, body));
    }
}
