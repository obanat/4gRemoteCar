package com.obana.remotecar;

import android.os.Bundle;
import androidx.preference.*;

public class SettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference, rootKey);

        bindPreferenceSummaryToValue(findPreference("clientId"));
        bindPreferenceSummaryToValue(findPreference("serverIp"));
        bindPreferenceSummaryToValue(findPreference("serverPort"));
        bindPreferenceSummaryToValue(findPreference("networkType"));
        bindPreferenceSummaryToValue(findPreference("mediaType"));
        bindPreferenceSummaryToValue(findPreference("netType"));
        bindPreferenceSummaryToValue(findPreference("controlType"));
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        if (preference != null) {
            preference.setOnPreferenceChangeListener(this);
            onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        String stringValue = newValue.toString();

        if (preference instanceof EditTextPreference) {
            preference.setSummary(stringValue);
        } else if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
        }
        return true;
    }
}
