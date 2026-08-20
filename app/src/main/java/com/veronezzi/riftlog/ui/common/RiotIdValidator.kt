package com.veronezzi.riftlog.ui.common

/** Shared "GameName#Tag" parsing used by every Riot ID entry field (Home, Comparison). */
object RiotIdValidator {
    fun parse(rawRiotId: String): ParsedRiotId? {
        val parts = rawRiotId.trim().split("#")
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return null
        return ParsedRiotId(parts[0].trim(), parts[1].trim())
    }
}

data class ParsedRiotId(val gameName: String, val tagLine: String)
