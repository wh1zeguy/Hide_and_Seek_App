//https://github.com/karim-eg/DataStore-Android-Example/tree/master/app/src/main/java/co/encept/datastore
//The link above is a reference to a data storage reference that was used to help with the creation of this file

package com.example.hide_and_seek

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore(name = "location_prefs")

object LocationKeys {
    val LOCATION_NAME = stringPreferencesKey("location_name")
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")
}
