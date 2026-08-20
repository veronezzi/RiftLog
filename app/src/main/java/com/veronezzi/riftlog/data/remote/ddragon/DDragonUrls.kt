package com.veronezzi.riftlog.data.remote.ddragon

/** Used when the live `/api/versions.json` fetch fails and there's no cached version either.
 * Only a fallback - exact correctness doesn't matter, just keep it roughly current. */
const val FALLBACK_DDRAGON_VERSION = "15.1.1"

/** Builds Data Dragon CDN image URLs. All static, no API key needed. */
object DDragonUrls {
    private const val CDN_BASE = "https://ddragon.leagueoflegends.com"

    fun profileIcon(version: String, profileIconId: Int): String =
        "$CDN_BASE/cdn/$version/img/profileicon/$profileIconId.png"

    fun championSquare(version: String, championImageFull: String): String =
        "$CDN_BASE/cdn/$version/img/champion/$championImageFull"

    fun championSplash(championId: String): String =
        "$CDN_BASE/cdn/img/champion/splash/${championId}_0.jpg"

    fun itemIcon(version: String, itemId: Int): String =
        "$CDN_BASE/cdn/$version/img/item/$itemId.png"

    /** Rune icons aren't versioned under /cdn/{version}/ like other assets - runesReforged.json's
     * `icon` field is already a full relative path (e.g. "perk-images/Styles/Precision/..."),
     * served straight off /cdn/img/. */
    fun runeIcon(iconPath: String): String = "$CDN_BASE/cdn/img/$iconPath"
}
