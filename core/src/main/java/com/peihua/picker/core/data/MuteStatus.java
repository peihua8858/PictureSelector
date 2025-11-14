package com.peihua.picker.core.data;

public final class MuteStatus {
    /**
     * Always start video preview with volume off
     */
    private boolean mIsVolumeMuted = true;

    public MuteStatus() {};

    /**
     * Sets the volume to mute/unmute
     * @param isVolumeMuted - {@code true} if the volume state should be set to mute.
     *                        {@code false} otherwise.
     */
    public void setVolumeMuted(boolean isVolumeMuted) {
        mIsVolumeMuted = isVolumeMuted;
    }

    /**
     * @return {@code isVolumeMuted}
     */
    public boolean isVolumeMuted() {
        return mIsVolumeMuted;
    }
}