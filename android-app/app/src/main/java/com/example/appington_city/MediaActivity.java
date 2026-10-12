package com.example.appington_city;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Media remote: mouse stick, play/pause, seek, volume, scroll and streaming shortcuts. */
public class MediaActivity extends AppCompatActivity {

    private MouseController mouse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        ThumbstickView stick = findViewById(R.id.thumbstick);
        mouse = new MouseController(this);
        stick.setListener(mouse);

        bindGet(R.id.closeButton, "/clP");
        bindGet(R.id.fullscreenButton, "/fuScr");
        bindGet(R.id.clickButton, "/lc");
        bindGet(R.id.rewindButton, "/left");
        bindGet(R.id.pauseButton, "/space");
        bindGet(R.id.forwardButton, "/right");

        bindPost(R.id.soundUpButton, "/sound", "0.1");
        bindPost(R.id.soundDownButton, "/sound", "-0.1");
        bindPost(R.id.scrollUpButton, "/scroll", "300");
        bindPost(R.id.scrollDownButton, "/scroll", "-300");

        bindGet(R.id.netflixButton, "/net");
        bindGet(R.id.disneyButton, "/dis");
        bindGet(R.id.spotifyButton, "/spo");
        bindGet(R.id.youtubeButton, "/you");
    }

    private void bindGet(int id, final String path) {
        findViewById(id).setOnClickListener(v -> Api.fireGet(this, path));
    }

    private void bindPost(int id, final String path, final String body) {
        findViewById(id).setOnClickListener(v -> Api.firePost(this, path, body));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mouse.stop();
    }
}
