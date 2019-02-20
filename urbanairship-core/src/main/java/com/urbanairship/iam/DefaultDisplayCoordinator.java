/* Copyright 2018 Urban Airship and Contributors */

package com.urbanairship.iam;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntRange;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.urbanairship.app.ActivityMonitor;

import java.util.concurrent.TimeUnit;

/**
 * Default display coordinator. Only allows a single in-app message to be displayed at
 * a given time.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultDisplayCoordinator extends DisplayCoordinator {

    private final ActivityMonitor activityMonitor;
    private InAppMessage currentMessage = null;
    private boolean isLocked = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long displayInterval = InAppMessageManager.DEFAULT_DISPLAY_INTERVAL_MS;

    private final Runnable postDisplayRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentMessage == null) {
                isLocked = false;
                notifyDisplayReady();
            }
        }
    };

    /**
     * Default constructor.
     *
     * @param activityMonitor The activity monitor.
     */
    public DefaultDisplayCoordinator(ActivityMonitor activityMonitor) {
        this.activityMonitor = activityMonitor;
    }

    /**
     * Sets the in-app message display interval. Defaults to {@link InAppMessageManager#DEFAULT_DISPLAY_INTERVAL_MS}.
     *
     * @param time The display interval.
     * @param timeUnit The time unit.
     */
    void setDisplayInterval(@IntRange(from = 0) long time, @NonNull TimeUnit timeUnit) {
        this.displayInterval = timeUnit.toMillis(time);
    }

    /**
     * Gets the display interval in milliseconds.
     *
     * @return The display interval in milliseconds.
     */
    long getDisplayInterval() {
        return this.displayInterval;
    }


    @MainThread
    @Override
    public boolean isReady() {
        // Only one message at a time
        if (currentMessage != null) {
            return false;
        }

        // Display lock
        if (isLocked) {
            return false;
        }

        // Require a resumed activity
        return !activityMonitor.getResumedActivities().isEmpty();
    }

    @MainThread
    @Override
    public void onDisplayStarted(@NonNull InAppMessage message) {
        currentMessage = message;
        isLocked = true;
        mainHandler.removeCallbacks(postDisplayRunnable);
    }

    @MainThread
    @Override
    public void onDisplayFinished(@NonNull InAppMessage message) {
        currentMessage = null;
        mainHandler.postDelayed(postDisplayRunnable, displayInterval);
    }
}