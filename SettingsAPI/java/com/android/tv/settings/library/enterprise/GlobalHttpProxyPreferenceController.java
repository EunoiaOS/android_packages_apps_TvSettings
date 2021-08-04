/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.settings.library.enterprise;

import android.content.Context;
import android.net.ConnectivityManager;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.util.AbstractPreferenceController;

public class GlobalHttpProxyPreferenceController extends AbstractPreferenceController {
    private static final String KEY_GLOBAL_HTTP_PROXY = "global_http_proxy";
    private final ConnectivityManager mCm;

    public GlobalHttpProxyPreferenceController(
            Context context, UIUpdateCallback callback, int stateIdentifier) {
        super(context, callback, stateIdentifier);
        mCm = context.getSystemService(ConnectivityManager.class);
    }

    @Override
    public boolean isAvailable() {
        return mCm.getGlobalProxy() != null;
    }

    @Override
    public String[] getPreferenceKey() {
        return new String[] {KEY_GLOBAL_HTTP_PROXY};
    }
}
