/*
Copyright 2009-2016 Urban Airship Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE URBAN AIRSHIP INC ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL URBAN AIRSHIP INC OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.urbanairship.push.notifications;

import android.app.Notification;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;

import com.urbanairship.Logger;
import com.urbanairship.push.PushMessage;
import com.urbanairship.util.NotificationIdGenerator;
import com.urbanairship.util.UAStringUtil;

import java.io.IOException;

/**
 * The default notification factory.
 * <p/>
 * Notifications generated by this factory use the standard Android notification
 * layout and defaults to the BigTextStyle.
 */
public class DefaultNotificationFactory extends NotificationFactory {
    private int titleId;
    private int smallIconId;
    private int largeIcon;
    private Uri sound = null;
    private int constantNotificationId = -1;
    private int accentColor = NotificationCompat.COLOR_DEFAULT;

    public DefaultNotificationFactory(Context context) {
        super(context);
        titleId = context.getApplicationInfo().labelRes;
        smallIconId = context.getApplicationInfo().icon;
    }

    @Override
    public Notification createNotification(@NonNull PushMessage message, int notificationId) {
        // do not display a notification if there is not an alert
        if (UAStringUtil.isEmpty(message.getAlert())) {
            return null;
        }

        NotificationCompat.Style defaultStyle = new NotificationCompat.BigTextStyle().bigText(message.getAlert());
        return createNotificationBuilder(message, notificationId, defaultStyle).build();
    }

    @Override
    public int getNextId(@NonNull PushMessage pushMessage) {
        if (constantNotificationId > 0) {
            return constantNotificationId;
        } else {
            return NotificationIdGenerator.nextID();
        }
    }

    /**
     * Creates a NotificationCompat.Builder with the default settings applied.
     *
     * @param message The PushMessage.
     * @param notificationId The notification id.
     * @param defaultStyle The default notification style.
     * @return A NotificationCompat.Builder.
     */
    protected NotificationCompat.Builder createNotificationBuilder(@NonNull PushMessage message, int notificationId, @Nullable NotificationCompat.Style defaultStyle) {
        String title = UAStringUtil.isEmpty(message.getTitle()) ? getDefaultTitle() : message.getTitle();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext())
                .setContentTitle(title)
                .setContentText(message.getAlert())
                .setAutoCancel(true)
                .setSmallIcon(smallIconId)
                .setColor(accentColor)
                .setLocalOnly(message.isLocalOnly())
                .setPriority(message.getPriority())
                .setCategory(message.getCategory())
                .setVisibility(message.getVisibility());

        // Public notification - Android L and above
        Notification notification = createPublicVersionNotification(message, smallIconId);
        if (notification != null) {
            builder.setPublicVersion(notification);
        }

        int defaults = NOTIFICATION_DEFAULTS;

        if (sound != null) {
            builder.setSound(sound);

            // Remove the Notification.DEFAULT_SOUND flag
            defaults &= ~Notification.DEFAULT_SOUND;
        }

        builder.setDefaults(defaults);

        if (largeIcon > 0) {
            builder.setLargeIcon(BitmapFactory.decodeResource(getContext().getResources(), largeIcon));
        }

        if (message.getSummary() != null) {
            builder.setSubText(message.getSummary());
        }

        NotificationCompat.Style style = null;
        try {
            style = createNotificationStyle(message);
        } catch (IOException e) {
            Logger.error("Failed to create notification style.", e);
        }

        if (style != null) {
            builder.setStyle(style);
        } else if (defaultStyle != null) {
            builder.setStyle(defaultStyle);
        }

        if (!message.isLocalOnly()) {
            try {
                builder.extend(createWearableExtender(message, notificationId));
            } catch (IOException e) {
                Logger.error("Failed to create wearable extender.", e);
            }
        }

        builder.extend(createNotificationActionsExtender(message, notificationId));

        return builder;
    }

    /**
     * Set the optional constant notification ID.
     *
     * @param id The integer ID as an int.
     */
    public void setConstantNotificationId(int id) {
        constantNotificationId = id;
    }

    /**
     * Get the constant notification ID.
     *
     * @return The constant notification ID as an int.
     */
    public int getConstantNotificationId() {
        return constantNotificationId;
    }

    /**
     * Set the title used in the notification layout.
     *
     * @param titleId The title as an int. A value of -1 will not display a title. A value of 0 will
     * display the application name as the title. A string resource ID will display the specified
     * string as the title.
     */
    public void setTitleId(@StringRes int titleId) {
        this.titleId = titleId;
    }

    /**
     * Get the title used in the notification layout.
     *
     * @return The title as an int.
     */
    @StringRes
    public int getTitleId() {
        return titleId;
    }

    /**
     * Set the small icon used in the notification layout.
     *
     * @param smallIconId The small icon ID as an int.
     */
    public void setSmallIconId(@DrawableRes int smallIconId) {
        this.smallIconId = smallIconId;
    }

    /**
     * Get the small icon used in the notification layout.
     *
     * @return The small icon ID as an int.
     */
    @DrawableRes
    public int getSmallIconId() {
        return smallIconId;
    }

    /**
     * Set the sound played when the notification arrives.
     *
     * @param sound The sound as a Uri.
     */
    public void setSound(Uri sound) {
        this.sound = sound;
    }

    /**
     * Get the sound played when the notification arrives.
     *
     * @return The sound as a Uri.
     */
    public Uri getSound() {
        return sound;
    }

    /**
     * Set the large icon used in the notification layout.
     *
     * @param largeIcon The large icon ID as an int.
     */
    public void setLargeIcon(@DrawableRes int largeIcon) {
        this.largeIcon = largeIcon;
    }

    /**
     * Get the large icon used in the notification layout.
     *
     * @return The large icon ID as a int.
     */
    @DrawableRes
    public int getLargeIcon() {
        return largeIcon;
    }

    /**
     * Set the accent color used in the notification.
     *
     * @param accentColor The accent color of the main notification icon.
     */
    public void setColor(@ColorInt int accentColor) {
        this.accentColor = accentColor;
    }

    /**
     * Get the accent color used in the notification.
     *
     * @return The accent color as an int.
     */
    @ColorInt
    public int getColor() {
        return accentColor;
    }

    /**
     * Gets the default title for the notification. If the {@link #getTitleId()} is 0,
     * the application label will be used, if greater than 0 the string will be fetched
     * from the resources, and if negative an empty String
     *
     * @return The default notification title.
     */
    protected String getDefaultTitle() {
        if (getTitleId() == 0) {
            return getContext().getPackageManager().getApplicationLabel(getContext().getApplicationInfo()).toString();
        } else if (getTitleId() > 0) {
            return getContext().getString(getTitleId());
        }

        return "";
    }
}
