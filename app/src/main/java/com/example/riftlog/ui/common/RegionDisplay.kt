package com.example.riftlog.ui.common

/** Short display labels for platform region chips. RegionMapper only exposes the raw ids. */
object RegionDisplay {
    private val labels = mapOf(
        "na1" to "NA", "euw1" to "EUW", "eun1" to "EUNE", "kr" to "KR",
        "br1" to "BR", "la1" to "LAN", "la2" to "LAS", "oc1" to "OCE",
        "tr1" to "TR", "ru" to "RU", "jp1" to "JP",
    )

    fun labelFor(platformId: String): String = labels[platformId] ?: platformId.uppercase()
}
