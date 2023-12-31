
package com.peihua.selector.photos.ui;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.Clock;
import androidx.media3.datasource.ContentDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector;
import androidx.media3.exoplayer.source.MediaParserExtractorAdapter;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.ui.PlayerView;

import com.peihua.photopicker.R;
import com.peihua.selector.data.MuteStatus;

/**
 * A helper class that assists in initialize/prepare/play/release of ExoPlayer. The class assumes
 * that all its public methods are called from main thread only.
 */
class ExoPlayerWrapper {
    private static final String TAG = "ExoPlayerWrapper";
    // The minimum duration of media that the player will attempt to ensure is buffered at all
    // times.
    private static final int MIN_BUFFER_MS = 1000;
    // The maximum duration of media that the player will attempt to buffer.
    private static final int MAX_BUFFER_MS = 2000;
    // The duration of media that must be buffered for playback to start or resume following a user
    // action such as a seek.
    private static final int BUFFER_FOR_PLAYBACK_MS = 1000;
    // The default duration of media that must be buffered for playback to resume after a rebuffer.
    private static final int BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1000;
    private static final LoadControl sLoadControl = new DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                    MIN_BUFFER_MS,
                    MAX_BUFFER_MS,
                    BUFFER_FOR_PLAYBACK_MS,
                    BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS).build();
    private static final long PLAYER_CONTROL_ON_PLAY_TIMEOUT_MS = 1000;
    private final Context mContext;
    private final MuteStatus mMuteStatus;
    private ExoPlayer mExoPlayer;
    private boolean mIsPlayerReleased = true;
    private boolean mShouldShowControlsForNext = true;
    private boolean mIsAccessibilityEnabled = false;

    public ExoPlayerWrapper(Context context, MuteStatus muteStatus) {
        mContext = context;
        mMuteStatus = muteStatus;
    }

    /**
     * Prepares the {@link ExoPlayer} and attaches it to given {@code styledPlayerView} and starts
     * playing.
     * Note: The method tries to release the {@link ExoPlayer} before preparing the new one. As we
     * don't have previous page's {@link PlayerView}, we can't switch the player from previous
     * {@link PlayerView} to new one. Hence, we try to create a new {@link ExoPlayer} instead.
     */
    public void prepareAndPlay(PlayerView styledPlayerView, ImageView imageView, Uri uri) {
        // TODO(b/197083539): Explore options for not re-creating ExoPlayer everytime.
        initializeExoPlayer(uri);

        setupPlayerLayout(styledPlayerView, imageView);

        // Prepare the player and play the video
        mExoPlayer.prepare();
        mExoPlayer.setPlayWhenReady(true);
        mIsPlayerReleased = false;
    }

    public void resetPlayerIfNecessary() {
        // Clear state of the previous player controls visibility state. Controls visibility state
        // will only be tracked and used for contiguous videos in the preview.
        mShouldShowControlsForNext = true;
        // Release the player if necessary.
        releaseIfNecessary();
    }

    private void initializeExoPlayer(Uri uri) {
        // Try releasing the ExoPlayer first.
        releaseIfNecessary();

        mExoPlayer = createExoPlayer();
        // We always start from the beginning of the video, and we always repeat the video in a loop
        mExoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        // We only play one video in the player, hence we should always use setMediaItem instead of
        // ExoPlayer#addMediaItem
        mExoPlayer.setMediaItem(MediaItem.fromUri(uri));
    }

    private ExoPlayer createExoPlayer() {
        // ProgressiveMediaFactory will be enough for video playback of videos on the device.
        // This also reduces apk size.
        ProgressiveMediaSource.Factory mediaSourceFactory;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            mediaSourceFactory = new ProgressiveMediaSource.Factory(
                    () -> new ContentDataSource(mContext), MediaParserExtractorAdapter.FACTORY);
        } else {
            mediaSourceFactory = new ProgressiveMediaSource.Factory(
                    () -> new ContentDataSource(mContext));
        }

        return new ExoPlayer.Builder(mContext,
                new DefaultRenderersFactory(mContext),
                mediaSourceFactory,
                new DefaultTrackSelector(mContext),
                sLoadControl,
                DefaultBandwidthMeter.getSingletonInstance(mContext),
                new DefaultAnalyticsCollector(Clock.DEFAULT)).build();
    }

    private void setupPlayerLayout(PlayerView styledPlayerView, ImageView imageView) {
        // Step1: Set-up Player layout
        // TODO(b/197083539): Remove this if it drains battery.
        styledPlayerView.setKeepScreenOn(true);
        styledPlayerView.setPlayer(mExoPlayer);
        styledPlayerView.setVisibility(View.VISIBLE);

        // Hide ImageView when the player is ready.
        mExoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    imageView.setVisibility(View.GONE);
                }
            }
        });

        // Step2: Set-up player control view
        // Set-up video controls for accessibility mode
        // Set Accessibility listeners and update the video controller visibility accordingly
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(
                enabled -> updateControllerForAccessibilty(enabled, styledPlayerView));
        updateControllerForAccessibilty(accessibilityManager.isEnabled(), styledPlayerView);

        // Set-up video controls for non-accessibility mode
        // Track if the controller layout should be visible for the next video.
        styledPlayerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> mShouldShowControlsForNext = (visibility == View.VISIBLE));
        // Video controls will be visible if
        // 1. this is the first video preview page or
        // 2. the previous video had controls visible when the page was swiped or
        // 3. the previous page was not a video preview
        // or if we are in accessibility mode.
        if (mShouldShowControlsForNext) {
            styledPlayerView.showController();
        }

        // Player controls needs to be auto-hidden if they are shown
        // 1. when the video starts previewing or
        // 2. when the video starts playing from paused state.
        // To achieve this, we hide the controller whenever player state changes to 'play'
        mExoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (mIsAccessibilityEnabled) {
                    // Player controls are always visible in accessibility mode.
                    return;
                }

                // We don't have to hide controls if the state changed to PAUSED or controller
                // isn't visible.
                if (!isPlaying || !mShouldShowControlsForNext) {
                    return;
                }

                // Set controller visibility of the next video to false so that we don't show the
                // controls on the next video.
                mShouldShowControlsForNext = false;
                // Auto hide controller after 1s of player state changing to "Play".
                styledPlayerView.postDelayed(styledPlayerView::hideController, PLAYER_CONTROL_ON_PLAY_TIMEOUT_MS);
            }
        });

        // Step3: Set-up mute button
        final ImageButton muteButton = styledPlayerView.findViewById(R.id.preview_mute);
        final boolean isVolumeMuted = mMuteStatus.isVolumeMuted();
        if (isVolumeMuted) {
            // If the previous volume was muted, set the volume status to mute.
            mExoPlayer.setVolume(0f);
        }
        updateMuteButtonState(muteButton, isVolumeMuted);

        // Add click listeners for mute button
        muteButton.setOnClickListener(v -> {
            if (mMuteStatus.isVolumeMuted()) {
                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager == null) {
                    Log.e(TAG, "Couldn't find AudioManager while trying to set volume,"
                            + " unable to set volume");
                    return;
                }
                mExoPlayer.setVolume(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mMuteStatus.setVolumeMuted(false);
            } else {
                mExoPlayer.setVolume(0f);
                mMuteStatus.setVolumeMuted(true);
            }
            updateMuteButtonState(muteButton, mMuteStatus.isVolumeMuted());
        });
    }

    private void updateControllerForAccessibilty(boolean isEnabled,
                                                 PlayerView styledPlayerView) {
        mIsAccessibilityEnabled = isEnabled;
        if (isEnabled) {
            styledPlayerView.showController();
            styledPlayerView.setControllerHideOnTouch(false);
        } else {
            styledPlayerView.setControllerHideOnTouch(true);
        }
    }

    private void updateMuteButtonState(ImageButton muteButton, boolean isVolumeMuted) {
        updateMuteButtonContentDescription(muteButton, isVolumeMuted);
        updateMuteButtonIcon(muteButton, isVolumeMuted);
    }

    private void updateMuteButtonContentDescription(ImageButton muteButton, boolean isVolumeMuted) {
        muteButton.setContentDescription(
                mContext.getString(
                        isVolumeMuted ? R.string.picker_unmute_video : R.string.picker_mute_video));
    }

    private void updateMuteButtonIcon(ImageButton muteButton, boolean isVolumeMuted) {
        muteButton.setImageResource(
                isVolumeMuted ? R.drawable.picker_ic_volume_off : R.drawable.picker_ic_volume_up);
    }

    private void releaseIfNecessary() {
        // Release the player only when it's not already released. ExoPlayer doesn't crash if we try
        // to release already released player, but ExoPlayer#release() may not be a no-op, hence we
        // call release() only when it's not already released.
        if (!mIsPlayerReleased) {
            mExoPlayer.release();
            mIsPlayerReleased = true;
        }
    }
}
