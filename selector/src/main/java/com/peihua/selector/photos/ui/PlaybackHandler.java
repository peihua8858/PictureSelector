package com.peihua.selector.photos.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.media3.ui.PlayerView;
import androidx.viewpager2.widget.ViewPager2;

import com.peihua.photopicker.R;
import com.peihua.selector.data.MuteStatus;
import com.peihua.selector.data.model.Item;

/**
 * A class to handle page selected state to initiate video playback or release video player
 * resources. All the public methods of this class must be called from main thread as ExoPlayer
 * should be prepared/released from main thread.
 */
class PlaybackHandler {
    private static final String TAG = "PlaybackHandler";
    // Only main thread can call the methods in this class, hence we don't need to guard mVideoUri
    // with lock while reading or writing to it.
    private Uri mVideoUri = null;
    private final ExoPlayerWrapper mExoPlayerWrapper;

    PlaybackHandler(Context context, MuteStatus muteStatus) {
        mExoPlayerWrapper = new ExoPlayerWrapper(context, muteStatus);
    }

    /**
     * Handles video playback for the {@link ViewPager2} page when it's selected i.e., completely
     * visible.
     * <ul>
     * <li> If the selected page is a video page, prepare and play the video associated with
     * selected page
     * <li> If the selected page is a video page and the same video is already playing, then no
     * action will be taken.
     * <li> If the selected page is non-video page, try releasing the ExoPlayer associated with
     * previous page that was selected.
     * </ul>
     * @param view {@link RecyclerView.ViewHolder#itemView} of the selected page.
     */
    public void handleVideoPlayback(View view) {
        assertMainThread();

        final Object tag = view.getTag();
        if (!(tag instanceof Item)) {
            throw new IllegalStateException("Expected Item tag to be set to " + view);
        }

        final Item item = (Item) tag;
        if (!item.isVideo()) {
            // We only need to handle video playback. For everything else, try releasing ExoPlayer
            // if there is a prepared ExoPlayer of the previous page, also reset any player states
            // when necessary.
            mExoPlayerWrapper.resetPlayerIfNecessary();
            mVideoUri = null;
            return;
        }

        final Uri videoUri = item.getContentUri();
        if (mVideoUri != null && mVideoUri.equals(videoUri)) {
            // Selected video is already handled. This must be a slight drag and drop, and we don't
            // have to change state of the player.
            Log.d(TAG, "Ignoring handlePageSelected of already selected page, with uri "
                    + videoUri);
            return;
        }

        final PlayerView styledPlayerView = view.findViewById(R.id.preview_player_view);
        if (styledPlayerView == null) {
            throw new IllegalStateException("Expected to find StyledPlayerView in " + view);
        }
        final ImageView imageView = view.findViewById(R.id.preview_video_image);

        mVideoUri = videoUri;
        mExoPlayerWrapper.prepareAndPlay(styledPlayerView, imageView, mVideoUri);
    }

    public void onViewAttachedToWindow(View itemView) {
        final ImageView imageView = itemView.findViewById(R.id.preview_video_image);
        imageView.setVisibility(View.VISIBLE);
        final PlayerView styledPlayerView = itemView.findViewById(R.id.preview_player_view);
        styledPlayerView.setVisibility(View.GONE);
        styledPlayerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) null);
        styledPlayerView.hideController();
    }

    /**
     * Releases ExoPlayer if there is any. Also resets the saved video uri.
     */
    public void releaseResources() {
        assertMainThread();

        mVideoUri = null;
        mExoPlayerWrapper.resetPlayerIfNecessary();
    }

    private void assertMainThread() {
        if (Looper.getMainLooper().isCurrentThread()) return;

        throw new IllegalStateException("PlaybackHandler methods are expected to be called from"
                + " main thread. Current thread " + Looper.myLooper().getThread()
                + ", Main thread" + Looper.getMainLooper().getThread());
    }
}
