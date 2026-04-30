package xyz.chambaz.tilde.data

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsRepositoryTest {

    @Test
    fun defaults_when_empty() {
        val prefs = PrefsRepository.mapPreferences(emptyPreferences())
        assertEquals(4, prefs.favoriteCount)
        assertEquals(emptyList<Favorite>(), prefs.favorites)
        assertEquals("", prefs.timeTapPackage)
        assertEquals("", prefs.dateTapPackage)
        assertEquals("", prefs.swipeLeftPackage)
        assertEquals("", prefs.swipeRightPackage)
    }

    @Test
    fun maps_favorite_count() {
        val p = mutablePreferencesOf(intPreferencesKey("favorite_count") to 7)
        assertEquals(7, PrefsRepository.mapPreferences(p).favoriteCount)
    }

    @Test
    fun maps_favorites_list() {
        val p = mutablePreferencesOf(
            intPreferencesKey("favorite_count")           to 2,
            stringPreferencesKey("favorite_0_package")    to "com.example.foo",
            stringPreferencesKey("favorite_0_label")      to "Foo",
            stringPreferencesKey("favorite_1_package")    to "com.example.bar",
            stringPreferencesKey("favorite_1_label")      to "Bar",
        )
        val prefs = PrefsRepository.mapPreferences(p)
        assertEquals(2, prefs.favorites.size)
        assertEquals(Favorite(0, "com.example.foo", "Foo"), prefs.favorites[0])
        assertEquals(Favorite(1, "com.example.bar", "Bar"), prefs.favorites[1])
    }

    @Test
    fun incomplete_favorite_slot_excluded() {
        val p = mutablePreferencesOf(
            intPreferencesKey("favorite_count")        to 1,
            stringPreferencesKey("favorite_0_package") to "com.example.foo",
            // label missing
        )
        assertEquals(emptyList<Favorite>(), PrefsRepository.mapPreferences(p).favorites)
    }

    @Test
    fun maps_tap_and_swipe_packages() {
        val p = mutablePreferencesOf(
            stringPreferencesKey("time_tap_package")    to "com.clock",
            stringPreferencesKey("date_tap_package")    to "com.cal",
            stringPreferencesKey("swipe_left_package")  to "com.left",
            stringPreferencesKey("swipe_right_package") to "com.right",
        )
        val prefs = PrefsRepository.mapPreferences(p)
        assertEquals("com.clock", prefs.timeTapPackage)
        assertEquals("com.cal",   prefs.dateTapPackage)
        assertEquals("com.left",  prefs.swipeLeftPackage)
        assertEquals("com.right", prefs.swipeRightPackage)
    }
}
