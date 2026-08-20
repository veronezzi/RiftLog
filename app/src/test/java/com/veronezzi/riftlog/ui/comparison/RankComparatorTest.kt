package com.veronezzi.riftlog.ui.comparison

import com.veronezzi.riftlog.domain.model.RankEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankComparatorTest {

    private fun rank(tier: String, division: String, lp: Int) =
        RankEntry(queueType = "RANKED_SOLO_5x5", tier = tier, rank = division, leaguePoints = lp, wins = 0, losses = 0)

    @Test
    fun `both unranked ties`() {
        assertEquals(0, RankComparator.compare(null, null))
    }

    @Test
    fun `ranked beats unranked regardless of side`() {
        val gold = rank("GOLD", "IV", 0)
        assertTrue(RankComparator.compare(gold, null) > 0)
        assertTrue(RankComparator.compare(null, gold) < 0)
    }

    @Test
    fun `higher tier wins even with lower LP`() {
        val diamond = rank("DIAMOND", "IV", 0)
        val gold = rank("GOLD", "I", 99)
        assertTrue(RankComparator.compare(diamond, gold) > 0)
    }

    @Test
    fun `same tier compares by division then LP`() {
        val goldTwo = rank("GOLD", "II", 10)
        val goldThree = rank("GOLD", "III", 90)
        assertTrue(RankComparator.compare(goldTwo, goldThree) > 0)

        val goldTwoLowLp = rank("GOLD", "II", 10)
        val goldTwoHighLp = rank("GOLD", "II", 80)
        assertTrue(RankComparator.compare(goldTwoLowLp, goldTwoHighLp) < 0)
    }

    @Test
    fun `identical rank ties`() {
        val a = rank("PLATINUM", "I", 50)
        val b = rank("PLATINUM", "I", 50)
        assertEquals(0, RankComparator.compare(a, b))
    }
}
