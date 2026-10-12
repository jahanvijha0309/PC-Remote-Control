package com.example.appington_city;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/** Upload a file to the PC, or tap a file from the PC's download folder to save it on the phone. */
public class FilesActivity extends AppCompatActivity {

    private LinearLayout fileList;
    private TextView emptyText;

    private final ActivityResultLauncher<Intent> filePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null
                        && result.getData().getData() != null) {
                    uploadFile(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);
        fileList = findViewById(R.id.fileListLayout);
        emptyText = findViewById(R.id.filesEmptyText);

        findViewById(R.id.uploadButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            filePicker.launch(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFileList();
    }

    // ---------------- list of files on the PC ----------------

    private void loadFileList() {
        Api.get(this, "/downloadfolder", (ok, code, body) -> {
            fileList.removeAllViews();
            if (!ok) {
                showEmpty(Prefs.getIp(this).isEmpty()
                        ? "No PC selected - open Settings first"
                        : "Could not reach the PC");
                return;
            }
            try {
                JSONArray names = new JSONArray(body);
                if (names.length() == 0) {
                    showEmpty("No files in the PC's for_downloads folder");
                    return;
                }
                emptyText.setVisibility(View.GONE);
                for (int i = 0; i < names.length(); i++) {
                    addFileRow(names.getString(i));
                }
            } catch (JSONException e) {
                showEmpty("Error retrieving file list");
            }
        });
    }

    private void showEmpty(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
    }

    private void addFileRow(final String name) {
        TextView row = new TextView(this);
        row.setText(name);
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        row.setTextColor(ContextCompat.getColor(this, R.color.text_title));
        int pad = (int) (14 * getResources().getDisplayMetrics().density);
        row.setPadding(pad, pad, pad, pad);
        row.setClickable(true);
        row.setFocusable(true);
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            row.setBackgroundResource(tv.resourceId);
        }
        row.setOnClickListener(v -> downloadFile(name));
        fileList.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    // ---------------- download PC -> phone ----------------

    private void downloadFile(final String name) {
        final String ip = Prefs.getIp(this);
        if (ip.isEmpty()) {
            Toast.makeText(this, "No PC selected - open Settings first", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Downloading " + name + "...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + ip + ":" + Prefs.PORT + "/download");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(30000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                byte[] payload = new JSONObject().put("filename", name)
                        .toString().getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(payload.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }
                int code = conn.getResponseCode();
                if (code != 200) {
                    toast("Error downloading the file. Status code: " + code);
                    return;
                }
                String where;
                try (InputStream in = conn.getInputStream()) {
                    where = saveToPhone(name, in);
                }
                toast("Successfully downloaded file: " + where);
            } catch (Exception e) {
                toast("Error downloading the file: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }, "download").start();
    }

    /** Saves into Downloads/PC-Remote (Android 10+) or the app's own folder (older phones). */
    private String saveToPhone(String name, InputStream in) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String mime = URLConnection.guessContentTypeFromName(name);
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime != null ? mime : "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PC-Remote");
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Could not create the file");
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IOException("Could not open the file");
                copy(in, out);
            }
            return "Downloads/PC-Remote/" + name;
        } else {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = getFilesDir();
            File target = new File(dir, name);
            try (OutputStream out = new FileOutputStream(target)) {
                copy(in, out);
            }
            return target.getAbsolutePath();
        }
    }

    // ---------------- upload phone -> PC ----------------

    private void uploadFile(final Uri uri) {
        final String ip = Prefs.getIp(this);
        if (ip.isEmpty()) {
            Toast.makeText(this, "No PC selected - open Settings first", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            File temp = null;
            HttpURLConnection conn = null;
            try {
                String fileName = queryFileName(uri).replace("\"", "%22").replace("\r", "").replace("\n", "");
                temp = File.createTempFile("upload", ".tmp", getCacheDir());
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(temp)) {
                    if (in == null) throw new IOException("Could not read the file");
                    copy(in, out);
                }

                String boundary = "----PCRemote" + System.currentTimeMillis();
                byte[] head = ("--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
                byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

                URL url = new URL("http://" + ip + ":" + Prefs.PORT + "/upload");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(60000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setFixedLengthStreamingMode(head.length + temp.length() + tail.length);
                try (OutputStream out = conn.getOutputStream();
                     InputStream in = new FileInputStream(temp)) {
                    out.write(head);
                    copy(in, out);
                    out.write(tail);
                }
                int code = conn.getResponseCode();
                toast(code == 200 ? "File uploaded successfully"
                        : "Failed to upload file. Response code: " + code);
            } catch (Exception e) {
                toast("Error uploading file: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
                if (temp != null) //noinspection ResultOfMethodCallIgnored
                    temp.delete();
            }
        }, "upload").start();
    }

    private String queryFileName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = cursor.getString(index);
            }
        }
        if (name == null || name.isEmpty()) {
            String path = uri.getLastPathSegment();
            name = path != null ? path.substring(path.lastIndexOf('/') + 1) : "upload.bin";
        }
        return name;
    }

    // ---------------- helpers ----------------

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
    }

    private void toast(final String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
}
