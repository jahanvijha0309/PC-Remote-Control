package com.example.appington_city;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

/** Scans the QR code shown by the PC server and connects to the IP inside it. */
public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 201;

    private SurfaceView surfaceView;
    private TextView scanInfo;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private boolean surfaceReady = false;
    private boolean handled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);
        surfaceView = findViewById(R.id.surfaceView);
        scanInfo = findViewById(R.id.scanInfo);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1280, 720)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surfaceReady = true;
                startCamera();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                surfaceReady = false;
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> codes = detections.getDetectedItems();
                if (codes.size() == 0 || handled) return;
                final String value = codes.valueAt(0).displayValue;
                handled = true;
                runOnUiThread(() -> handleQrValue(value));
            }
        });
    }

    private void startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        if (!surfaceReady) return;
        try {
            cameraSource.start(surfaceView.getHolder());
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Could not start the camera", Toast.LENGTH_LONG).show();
        }
    }

    private void handleQrValue(String value) {
        scanInfo.setText("QR Code Value: " + value);
        String ip = Prefs.parseIp(value);
        if (ip.isEmpty()) {
            handled = false;
            return;
        }
        Connector.connect(this, ip, connected -> {
            if (connected) {
                finish();
            } else {
                handled = false;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CAMERA_PERMISSION_REQUEST_CODE) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR code", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) cameraSource.release();
        if (barcodeDetector != null) barcodeDetector.release();
    }
}
