package com.example.smarket

import android.os.Bundle
import android.util.Log
import androidx.preference.*

class SettingsFragment : PreferenceFragmentCompat() {
    fun MultiSelectListPreference.setSummaryFromValues(values: Set<String>) {
        summary = values.map {entries[findIndexOfValue(it)]}.joinToString(", ")
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val currentVals = PreferenceManager.getDefaultSharedPreferences(context).all
        Log.d("vals",currentVals.toString())

        val listSelectNames = listOf("time_selection_monday","time_selection_tuesday","time_selection_wednesday",
            "time_selection_thursday","time_selection_friday","time_selection_saturday","time_selection_sunday")
        for (name in listSelectNames)
        {
            val listSelectElement = findPreference<MultiSelectListPreference>(name)
            if (listSelectElement != null) {
                listSelectElement.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { preference, newValue ->
                        val newValueSet = newValue as HashSet<String>
                        UserData.updateDeliveryInfo(name, newValueSet).addOnSuccessListener {
                            listSelectElement.setSummaryFromValues(newValueSet)
                        }
                        true
                    }
                listSelectElement.setSummaryFromValues(listSelectElement.values)
            }
        }
        val editTextNames = listOf("first_name","last_name","phone_number","address")
        for(name in editTextNames)
        {
            val editTextElement = findPreference<EditTextPreference>(name)
            if(editTextElement != null)
            {
                editTextElement.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener{ preference, newValue->
                        UserData.updatePersonalInfo(name, newValue as String)
                        true
                    }
            }
        }

    }
}