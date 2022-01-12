package com.example.smarket

import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {
    fun MultiSelectListPreference.setSummaryFromValues(values: Set<String>) {
        summary = values.map {entries[findIndexOfValue(it)]}.joinToString(", ")
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val listSelectNames = listOf("time_selection_monday","time_selection_tuesday","time_selection_wednesday","time_selection_thursday","time_selection_friday")
        for (name in listSelectNames)
        {
            val listSelectElement = findPreference<MultiSelectListPreference>(name)
            if (listSelectElement != null) {
                listSelectElement.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { preference, newValue ->
                        listSelectElement.setSummaryFromValues(newValue as Set<String>)
                        true
                    }
                listSelectElement.setSummaryFromValues(listSelectElement.values)
            }
        }

    }
}