/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.tv.settings.device.displaysound;

import static android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION;
import static android.view.Display.HdrCapabilities.HDR_TYPE_HDR10;
import static android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS;
import static android.view.Display.HdrCapabilities.HDR_TYPE_HLG;
import static android.view.Display.HdrCapabilities.HDR_TYPE_INVALID;

import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_CLASSIC;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.HdrConversionMode;
import android.os.Bundle;
import android.view.Display;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.overlay.FlavorUtils;

/**
 * This Fragment is responsible for selecting the dynamic range or HDR conversion preference.
 */
@Keep
public class PreferredDynamicRangeFragment  extends SettingsPreferenceFragment {
    static final String PREFERRED_DYNAMIC_RANGE_FORCE_FRAGMENT =
            "com.android.tv.settings.device.displaysound.PreferredDynamicRangeForceFragment";
    static final String KEY_DYNAMIC_RANGE_SELECTION = "preferred_dynamic_range_selection_option";
    static final String KEY_DYNAMIC_RANGE_SELECTION_SYSTEM =
            "preferred_dynamic_range_selection_system";
    static final String KEY_DYNAMIC_RANGE_SELECTION_PASSTHROUGH =
            "preferred_dynamic_range_selection_passthrough";
    static final String KEY_DYNAMIC_RANGE_SELECTION_FORCE =
            "preferred_dynamic_range_selection_force";

    private static final String TAG = ResolutionSelectionFragment.class.getSimpleName();
    private DisplayManager mDisplayManager;

    private HdrConversionMode mHdrConversionMode;

    private boolean mShowWarningDialog;

    /** @return the new instance of the class */
    public static PreferredDynamicRangeFragment newInstance() {
        return new PreferredDynamicRangeFragment();
    }

    @Override
    public void onAttach(Context context) {
        mDisplayManager = getDisplayManager();
        mHdrConversionMode = mDisplayManager.getHdrConversionModeSetting();
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        mHdrConversionMode = mDisplayManager.getHdrConversionModeSetting();
        super.onResume();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferred_dynamic_range_selection, null);

        Preference pref =
                findPreference(getPreferenceFromMode(mHdrConversionMode.getConversionMode()));
        if (pref != null) {
            selectRadioPreference(pref);
        }
        setPreferredDynamicRangeForceFragment(
                mHdrConversionMode.getConversionMode() == HdrConversionMode.HDR_CONVERSION_FORCE
        );
        // Do not show sidebar info texts in case of 1 panel settings.
        if (FlavorUtils.getFlavor(getContext()) != FLAVOR_CLASSIC) {
            createInfoFragments();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        if (key == null) {
            return super.onPreferenceTreeClick(preference);
        }

        if (preference instanceof RadioPreference) {
            Preference oldPreference = getSelectedRadioPreference(getPreferenceGroup());
            selectRadioPreference(preference);
            // Enable auto option in HDR types if hdr conversion mode to SDR was selected. This
            // is done because when SDR is chosen, we disable all HDR types.
            switch (key) {
                case KEY_DYNAMIC_RANGE_SELECTION_SYSTEM: {
                    mHdrConversionMode = new HdrConversionMode(
                            HdrConversionMode.HDR_CONVERSION_SYSTEM);
                    mDisplayManager.setHdrConversionMode(mHdrConversionMode);
                    setPreferredDynamicRangeForceFragment(false);
                    if (mHdrConversionMode.equals(new HdrConversionMode(
                            HdrConversionMode.HDR_CONVERSION_FORCE, HDR_TYPE_INVALID))) {
                        mDisplayManager.setAreUserDisabledHdrTypesAllowed(true);
                    }
                    break;
                }
                case KEY_DYNAMIC_RANGE_SELECTION_PASSTHROUGH: {
                    mHdrConversionMode =
                            new HdrConversionMode(HdrConversionMode.HDR_CONVERSION_PASSTHROUGH);
                    mDisplayManager.setHdrConversionMode(mHdrConversionMode);
                    setPreferredDynamicRangeForceFragment(false);
                    if (mHdrConversionMode.equals(new HdrConversionMode(
                            HdrConversionMode.HDR_CONVERSION_FORCE, HDR_TYPE_INVALID))) {
                        mDisplayManager.setAreUserDisabledHdrTypesAllowed(true);
                    }
                    break;
                }
                case KEY_DYNAMIC_RANGE_SELECTION_FORCE: {
                    if (mShowWarningDialog && mHdrConversionMode.getConversionMode()
                            != HdrConversionMode.HDR_CONVERSION_FORCE) {
                        showWarningDialogOnDynamicRangeSelectionForce(oldPreference);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown dynamic range selection pref value"
                            + ": " + key);
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @VisibleForTesting
    DisplayManager getDisplayManager() {
        return getContext().getSystemService(DisplayManager.class);
    }

    private void selectRadioPreference(Preference preference) {
        final RadioPreference radioPreference = (RadioPreference) preference;
        radioPreference.setChecked(true);
        radioPreference.clearOtherRadioPreferences(getPreferenceGroup());
    }

    private Preference getSelectedRadioPreference(PreferenceGroup preferenceGroup) {
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            RadioPreference preference = (RadioPreference) preferenceGroup.getPreference(i);
            if (preference.isChecked()) {
                return preference;
            }
        }
        return null;
    }

    private PreferenceGroup getPreferenceGroup() {
        return (PreferenceGroup) findPreference(KEY_DYNAMIC_RANGE_SELECTION);
    }

    private void createInfoFragments() {
        Preference dynamicRangeSystemPref = findPreference(KEY_DYNAMIC_RANGE_SELECTION_SYSTEM);
        if (dynamicRangeSystemPref != null) {
            dynamicRangeSystemPref.setFragment(
                    PreferredDynamicRangeInfo
                            .PreferredDynamicRangeSystemInfoFragment.class.getName());
        }
    }

    private String getPreferenceFromMode(int hdrConversionMode) {
        switch (hdrConversionMode) {
            case HdrConversionMode.HDR_CONVERSION_PASSTHROUGH:
                return KEY_DYNAMIC_RANGE_SELECTION_PASSTHROUGH;
            case  HdrConversionMode.HDR_CONVERSION_SYSTEM:
                return KEY_DYNAMIC_RANGE_SELECTION_SYSTEM;
            case HdrConversionMode.HDR_CONVERSION_FORCE:
                return KEY_DYNAMIC_RANGE_SELECTION_FORCE;
            default:
                return null;
        }
    }

    //oldPref is preference to restore in case user selects Cancel from the dialog.
    private void showWarningDialogOnDynamicRangeSelectionForce(Preference oldPref) {
        // Select the first HDR output mode returned by getSupportedHdrOutputTypes
        int selectedFormat = mDisplayManager.getSupportedHdrOutputTypes()[0];
        String dialogDescription =
                getResources().getString(R.string.preferred_dynamic_range_force_dialog_desc,
                        getFormatString(selectedFormat));
        final Display display = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);
        Display.Mode currentMode = display.getMode();
        boolean is4k60HzMode = (currentMode.getPhysicalHeight() == 2160
                || currentMode.getPhysicalWidth() == 2160)
                && currentMode.getRefreshRate() >= 59.9;
        // If HDR output mode returned by getSupportedHdrOutputTypes is DolbyVision, the active mode
        // is greater than 4k30Hz, and the TV has does not support DolbyVision at this mode,
        // add this information in dialog.
        if (is4k60HzMode && mDisplayManager.getSupportedHdrOutputTypes()[0]
                == HDR_TYPE_DOLBY_VISION) {
            if (!PreferredDynamicRangeUtils.isDolbyVisionSupported(currentMode)
                    && PreferredDynamicRangeUtils.isDolbyVisionSupported(
                    display.getSupportedModes())) {
                dialogDescription += getContext().getString(
                        R.string.preferred_dynamic_range_force_dialog_desc_4k30_issue);
            }
        }
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.preferred_dynamic_range_force_dialog_title)
                .setMessage(dialogDescription)
                .setPositiveButton(
                        R.string.settings_confirm,
                        (dialog, which) -> {
                            mHdrConversionMode = new HdrConversionMode(
                                    HdrConversionMode.HDR_CONVERSION_FORCE, selectedFormat);
                            mDisplayManager.setHdrConversionMode(mHdrConversionMode);
                            setPreferredDynamicRangeForceFragment(true);
                            dialog.dismiss();
                            onPreferenceTreeClick(
                                    findPreference(KEY_DYNAMIC_RANGE_SELECTION_FORCE));
                        })
                .setNegativeButton(
                        R.string.settings_cancel,
                        (dialog, which) -> {
                            if (oldPref != null) {
                                selectRadioPreference(oldPref);
                            }
                            dialog.dismiss();
                        })
                .create()
                .show();
    }

    private String getFormatString(int hdrType) {
        switch (hdrType) {
            case HDR_TYPE_DOLBY_VISION:
                return getContext().getString(R.string.hdr_format_dolby_vision);
            case HDR_TYPE_HDR10:
                return getContext().getString(R.string.hdr_format_hdr10);
            case HDR_TYPE_HLG:
                return getContext().getString(R.string.hdr_format_hlg);
            case HDR_TYPE_HDR10_PLUS:
                return getContext().getString(R.string.hdr_format_hdr10plus);
            default:
                return "";
        }
    }

    private void setPreferredDynamicRangeForceFragment(boolean shouldSet) {
        Preference pref = findPreference(KEY_DYNAMIC_RANGE_SELECTION_FORCE);
        String fragment = shouldSet ? PREFERRED_DYNAMIC_RANGE_FORCE_FRAGMENT : null;
        mShowWarningDialog = !shouldSet;

        if (pref != null) {
            pref.setFragment(fragment);
        }
    }
}