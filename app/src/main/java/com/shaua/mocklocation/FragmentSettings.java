package com.shaua.mocklocation;

import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.shaua.mocklocation.utils.GoUtils;

public class FragmentSettings extends PreferenceFragmentCompat {

    private void setupDecimalEditTextPreference(EditTextPreference preference) {
        if (preference != null) {
            preference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) EditTextPreference::getText);
            preference.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                editText.setSelection(editText.length());
            });
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue.toString().trim().isEmpty()) {
                    GoUtils.DisplayToast(this.getContext(), getResources().getString(R.string.app_error_input_null));
                    return false;
                }
                return true;
            });
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_main);

        EditTextPreference pfAltitude = findPreference("setting_altitude");
        setupDecimalEditTextPreference(pfAltitude);

        EditTextPreference pfLatOffset = findPreference("setting_lat_max_offset");
        setupDecimalEditTextPreference(pfLatOffset);

        EditTextPreference pfLonOffset = findPreference("setting_lon_max_offset");
        setupDecimalEditTextPreference(pfLonOffset);

        SwitchPreferenceCompat pLog = findPreference("setting_log_off");
        if (pLog != null) {
            pLog.setOnPreferenceChangeListener((preference, newValue) -> {
                if (((SwitchPreferenceCompat) preference).isChecked() != (Boolean) newValue) {
                    return true;
                } else {
                    return false;
                }
            });
        }

        String verName = GoUtils.getVersionName(FragmentSettings.this.getContext());
        Preference pfVersion = findPreference("setting_version");
        if (pfVersion != null) {
            pfVersion.setSummary(verName);
        }
    }
}