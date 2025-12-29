package org.libreblog.rss.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import org.libreblog.rss.R;
import org.libreblog.rss.core.MediaHandler;

public class FullscreenVideoActivity extends AppCompatActivity {
    static ExoPlayer player = null;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_video);

        Intent intent = getIntent();
        String uri = intent.getStringExtra("uri");
        if (uri == null) return;
        long pos = intent.getLongExtra("pos", 0);

        PlayerView playerView = findViewById(R.id.fullscreen_player_view);
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        1500,
                        3000,
                        250,
                        500)
                .build();

        player = new ExoPlayer.Builder(this).setLoadControl(loadControl).build();
        playerView.setPlayer(player);
        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setFullscreenButtonState(true);
        playerView.setFullscreenButtonClickListener(isFullscreen -> {
            player.stop();
            finish();
        });
        MediaItem mediaItem = MediaItem.fromUri(uri);
        MediaSource mediaSource = new ProgressiveMediaSource.
                Factory(MediaHandler.cacheDataSourceFactory).createMediaSource(mediaItem);
        player.setMediaSource(mediaSource);
        player.addListener(new ExoPlayer.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == ExoPlayer.STATE_READY && player.getPlayWhenReady()) {
                    playerView.hideController();
                }
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                if (playWhenReady && player.getPlaybackState() == ExoPlayer.STATE_READY) {
                    playerView.hideController();
                }
            }
        });
        player.prepare();
        if (pos > 0) player.seekTo(pos);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }
}
