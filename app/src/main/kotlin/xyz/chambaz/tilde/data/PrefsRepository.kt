package xyz.chambaz.tilde.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object PrefsRepository {
    private val FAVORITE_COUNT      = intPreferencesKey("favorite_count")
    private val TIME_TAP_PACKAGE    = stringPreferencesKey("time_tap_package")
    private val DATE_TAP_PACKAGE    = stringPreferencesKey("date_tap_package")
    private val SWIPE_LEFT_PACKAGE  = stringPreferencesKey("swipe_left_package")
    private val SWIPE_RIGHT_PACKAGE = stringPreferencesKey("swipe_right_package")

    private lateinit var dataStore: DataStore<Preferences>

    fun init(context: Context) {
        if (!::dataStore.isInitialized)
            dataStore = PreferenceDataStoreFactory.create {
                context.applicationContext.preferencesDataStoreFile("launcher_prefs")
            }
    }

    internal fun initForTest(store: DataStore<Preferences>) {
        dataStore = store
    }

    val prefs: Flow<LauncherPrefs>
        get() = dataStore.data.map { mapPreferences(it) }

    internal fun mapPreferences(p: Preferences): LauncherPrefs {
        val count = p[FAVORITE_COUNT] ?: 4
        val favorites = (0 until count).mapNotNull { n ->
            val pkg = p[stringPreferencesKey("favorite_${n}_package")] ?: return@mapNotNull null
            val lbl = p[stringPreferencesKey("favorite_${n}_label")]   ?: return@mapNotNull null
            Favorite(n, pkg, lbl)
        }
        return LauncherPrefs(
            favoriteCount     = count,
            favorites         = favorites,
            timeTapPackage    = p[TIME_TAP_PACKAGE]    ?: "",
            dateTapPackage    = p[DATE_TAP_PACKAGE]    ?: "",
            swipeLeftPackage  = p[SWIPE_LEFT_PACKAGE]  ?: "",
            swipeRightPackage = p[SWIPE_RIGHT_PACKAGE] ?: "",
        )
    }

    suspend fun updateFavoriteCount(n: Int) { dataStore.edit { it[FAVORITE_COUNT] = n } }

    suspend fun updateFavorite(slot: Int, packageName: String, label: String) {
        dataStore.edit {
            it[stringPreferencesKey("favorite_${slot}_package")] = packageName
            it[stringPreferencesKey("favorite_${slot}_label")]   = label
        }
    }

    suspend fun updateTimeTap(pkg: String)    { dataStore.edit { it[TIME_TAP_PACKAGE]    = pkg } }
    suspend fun updateDateTap(pkg: String)    { dataStore.edit { it[DATE_TAP_PACKAGE]    = pkg } }
    suspend fun updateSwipeLeft(pkg: String)  { dataStore.edit { it[SWIPE_LEFT_PACKAGE]  = pkg } }
    suspend fun updateSwipeRight(pkg: String) { dataStore.edit { it[SWIPE_RIGHT_PACKAGE] = pkg } }
}
