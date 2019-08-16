package com.arubanetworks.meridiansamples

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.arubanetworks.meridian.Meridian

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.prefs, rootKey)
        setVersionName()
    }

    private fun setVersionName() {
        val versionPref = findPreference("pref_version")
        versionPref.summary = Meridian.getShared().sdkVersion + " (" + Meridian.getShared().sdkVersionCode + ")"
    }

}
