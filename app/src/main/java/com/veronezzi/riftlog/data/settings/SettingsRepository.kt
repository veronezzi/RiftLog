package com.veronezzi.riftlog.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veronezzi.riftlog.data.remote.RegionMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")
private const val MAX_SEARCH_HISTORY = 25

/**
 * Holds user preferences: preferred platform region and last-search/last-profile bookkeeping.
 * The Riot API key is a developer setup concern (BuildConfig.RIOT_API_KEY from local.properties),
 * not a user preference, so it does not live here.
 */
class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val searchHistorySerializer = ListSerializer(RecentSearch.serializer())

    private object Keys {
        val PLATFORM_REGION = stringPreferencesKey("platform_region")
        val LAST_GAME_NAME = stringPreferencesKey("last_game_name")
        val LAST_TAG_LINE = stringPreferencesKey("last_tag_line")
        val LAST_PROFILE_PUUID = stringPreferencesKey("last_profile_puuid")
        val LAST_PROFILE_REGION = stringPreferencesKey("last_profile_region")
        val SEARCH_HISTORY = stringPreferencesKey("search_history_json")
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

    /** Every Riot ID this device has successfully resolved before, most-recent-first. Powers the
     * "type a name, see who you've looked up before" suggestions on Home - there's no Riot API to
     * search the whole player base by partial name, so this is necessarily scoped to this app's
     * own local history, not every League player. */
    val searchHistory: Flow<List<RecentSearch>> = context.dataStore.data.map { prefs ->
        prefs[Keys.SEARCH_HISTORY]?.let {
            runCatching { json.decodeFromString(searchHistorySerializer, it) }.getOrDefault(emptyList())
        } ?: emptyList()
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

    /** Records a successfully-resolved Riot ID into the local suggestion history: moves it to the
     * front if already present (case-insensitive on name+tag+region), caps the list size. Call
     * this only after a profile actually resolves - a typo'd search that 404s shouldn't pollute
     * suggestions with names that don't exist. */
    suspend fun addToSearchHistory(gameName: String, tagLine: String, platformRegion: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SEARCH_HISTORY]?.let {
                runCatching { json.decodeFromString(searchHistorySerializer, it) }.getOrDefault(emptyList())
            } ?: emptyList()
            val entry = RecentSearch(gameName, tagLine, platformRegion)
            val deduped = current.filterNot {
                it.gameName.equals(gameName, ignoreCase = true) &&
                    it.tagLine.equals(tagLine, ignoreCase = true) &&
                    it.platformRegion == platformRegion
            }
            val updated = (listOf(entry) + deduped).take(MAX_SEARCH_HISTORY)
            prefs[Keys.SEARCH_HISTORY] = json.encodeToString(searchHistorySerializer, updated)
        }
    }

    /** Just drops the Home pinned card. Deliberately keeps SEARCH_HISTORY: unpinning is "I don't
     * want this card right now", not "forget everyone I've looked up". */
    suspend fun clearPinnedProfile() {
        context.dataStore.edit {
            it.remove(Keys.LAST_GAME_NAME)
            it.remove(Keys.LAST_TAG_LINE)
            it.remove(Keys.LAST_PROFILE_PUUID)
            it.remove(Keys.LAST_PROFILE_REGION)
        }
    }

    suspend fun clearCachedData() {
        clearPinnedProfile()
        context.dataStore.edit { it.remove(Keys.SEARCH_HISTORY) }
    }

    companion object {
        const val DEFAULT_PLATFORM_REGION = "na1"
    }
}

@Serializable
data class RecentSearch(val gameName: String, val tagLine: String, val platformRegion: String)
data class LastProfile(val puuid: String, val platformRegion: String)
