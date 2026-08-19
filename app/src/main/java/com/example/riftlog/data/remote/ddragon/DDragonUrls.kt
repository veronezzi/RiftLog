package com.example.riftlog.data.remote.ddragon

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
}
