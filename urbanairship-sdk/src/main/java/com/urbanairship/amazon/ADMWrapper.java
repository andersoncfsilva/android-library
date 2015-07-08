/*
Copyright 2009-2015 Urban Airship Inc. All rights reserved.

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

package com.urbanairship.amazon;

import android.content.Context;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;
import com.urbanairship.Logger;
import com.urbanairship.UAirship;

/**
 * Wrapper around ADM methods.
 */
class ADMWrapper {

    /**
     * Wraps {@link com.amazon.device.messaging.development.ADMManifest#checkManifestAuthoredProperly(android.content.Context)}.
     */
    public static void validateManifest() {
        try {
            ADMManifest.checkManifestAuthoredProperly(UAirship.getApplicationContext());
        } catch (RuntimeException ex) {
            Logger.error("AndroidManifest invalid ADM setup.", ex);
        }
    }

    /**
     * Wraps {@link com.amazon.device.messaging.ADM#isSupported()}.
     *
     * @return The value returned by {@link com.amazon.device.messaging.ADM#isSupported()}.
     */
    public static boolean isSupported() {
        try {
            return new ADM(UAirship.getApplicationContext()).isSupported();
        } catch (RuntimeException ex) {
            Logger.error("Failed to call ADM. Make sure ADM jar is not bundled with the APK.");
            return false;
        }
    }

    /**
     * Wraps {@link com.amazon.device.messaging.ADM#startRegister()}.
     */
    public static void startRegistration(Context context) {
        new ADM(context).startRegister();
    }
}