package com.arubanetworks.meridiansamples;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.arubanetworks.meridian.Meridian;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.prefs, rootKey);
        setVersionName();
    }

    private void setVersionName() {
        Preference versionPref = findPreference("pref_version");
        versionPref.setSummary(Meridian.getShared().getSDKVersion() + " (" + Meridian.getShared().getSDKVersionCode() + ")");
    }

}
