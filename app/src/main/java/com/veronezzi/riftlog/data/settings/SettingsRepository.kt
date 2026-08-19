package com.veronezzi.riftlog.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veronezzi.riftlog.data.remote.RegionMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Holds user preferences: preferred platform region and last-search/last-profile bookkeeping.
 * The Riot API key is a developer setup concern (BuildConfig.RIOT_API_KEY from local.properties),
 * not a user preference, so it does not live here.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val PLATFORM_REGION = stringPreferencesKey("platform_region")
        val LAST_GAME_NAME = stringPreferencesKey("last_game_name")
        val LAST_TAG_LINE = stringPreferencesKey("last_tag_line")
        val LAST_PROFILE_PUUID = stringPreferencesKey("last_profile_puuid")
        val LAST_PROFILE_REGION = stringPreferencesKey("last_profile_region")
    }

    val platformRegion: Flow<String> = context.dataStore.data.map {
        it[Keys.PLATFORM_REGION] ?: DEFAULT_PLATFORM_REGION
    }

    /** Last Riot ID typed into Home, offered back as a "recent" shortcut. Null if never searched. */
    val lastSearch: Flow<RecentSearch?> = context.dataStore.data.map { prefs ->
        val gameName = prefs[Keys.LAST_GAME_NAME]
        val tagLine = prefs[Keys.LAST_TAG_LINE]
        if (gameName.isNullOrBlank() || tagLine.isNullOrBlank()) null
        else RecentSearch(gameName, tagLine, prefs[Keys.PLATFORM_REGION] ?: DEFAULT_PLATFORM_REGION)
    }

    /** puuid+region of the last successfully-resolved profile, used by the Match History and
     * Champion Stats tabs (which have no way to take nav args since they're bottom-nav roots). */
    val lastProfile: Flow<LastProfile?> = context.dataStore.data.map { prefs ->
        val puuid = prefs[Keys.LAST_PROFILE_PUUID]
        val region = prefs[Keys.LAST_PROFILE_REGION]
        if (puuid.isNullOrBlank() || region.isNullOrBlank()) null else LastProfile(puuid, region)
    }

    suspend fun setPlatformRegion(platformRegion: String) {
        require(platformRegion in RegionMapper.platformIds) {
            "Unknown platform region: $platformRegion"
        }
        context.dataStore.edit { it[Keys.PLATFORM_REGION] = platformRegion }
    }

    suspend fun setLastSearch(gameName: String, tagLine: String, platformRegion: String) {
        context.dataStore.edit {
            it[Keys.LAST_GAME_NAME] = gameName
            it[Keys.LAST_TAG_LINE] = tagLine
            it[Keys.PLATFORM_REGION] = platformRegion
        }
    }

    suspend fun setLastProfile(puuid: String, platformRegion: String) {
        context.dataStore.edit {
            it[Keys.LAST_PROFILE_PUUID] = puuid
            it[Keys.LAST_PROFILE_REGION] = platformRegion
        }
    }

    suspend fun clearCachedData() {
        context.dataStore.edit {
            it.remove(Keys.LAST_GAME_NAME)
            it.remove(Keys.LAST_TAG_LINE)
            it.remove(Keys.LAST_PROFILE_PUUID)
            it.remove(Keys.LAST_PROFILE_REGION)
        }
    }

    companion object {
        const val DEFAULT_PLATFORM_REGION = "na1"
    }
}

data class RecentSearch(val gameName: String, val tagLine: String, val platformRegion: String)
data class LastProfile(val puuid: String, val platformRegion: String)
