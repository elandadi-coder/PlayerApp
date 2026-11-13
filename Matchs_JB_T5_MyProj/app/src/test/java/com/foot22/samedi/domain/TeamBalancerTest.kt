package com.foot22.samedi.domain

import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.PlayerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamBalancerTest {

    @Test
    fun `excludes injured and inactive players from generated teams`() {
        val players = listOf(
            PlayerEntity(id = 1, name = "Karim", position = PlayerPosition.GARDIEN, skillRating = 8.0),
            PlayerEntity(id = 2, name = "Youssef", position = PlayerPosition.GARDIEN, skillRating = 7.0),
            PlayerEntity(id = 3, name = "Badr", position = PlayerPosition.DEFENSEUR, skillRating = 8.0),
            PlayerEntity(id = 4, name = "Sami", position = PlayerPosition.ATTAQUANT, skillRating = 9.0),
            PlayerEntity(id = 5, name = "Sofiane", position = PlayerPosition.DEFENSEUR, skillRating = 7.0, status = PlayerStatus.BLESSE),
            PlayerEntity(id = 6, name = "Anas", position = PlayerPosition.MILIEU, skillRating = 6.0, status = PlayerStatus.INACTIF)
        )

        val result = TeamBalancer.generateTeams(players)
        val allAssignedIds = (result.blueTeam + result.orangeTeam).map { it.id }

        assertEquals(4, allAssignedIds.size)
        assertFalse(5L in allAssignedIds)
        assertFalse(6L in allAssignedIds)
        assertEquals(2, result.blueTeam.size)
        assertEquals(2, result.orangeTeam.size)
    }

    @Test
    fun `splits goalkeepers and balances total skill tightly`() {
        val players = listOf(
            PlayerEntity(id = 1, name = "GK1", position = PlayerPosition.GARDIEN, skillRating = 8.0),
            PlayerEntity(id = 2, name = "GK2", position = PlayerPosition.GARDIEN, skillRating = 7.5),
            PlayerEntity(id = 3, name = "D1", position = PlayerPosition.DEFENSEUR, skillRating = 8.5),
            PlayerEntity(id = 4, name = "D2", position = PlayerPosition.DEFENSEUR, skillRating = 6.5),
            PlayerEntity(id = 5, name = "M1", position = PlayerPosition.MILIEU, skillRating = 9.0),
            PlayerEntity(id = 6, name = "M2", position = PlayerPosition.MILIEU, skillRating = 6.0),
            PlayerEntity(id = 7, name = "A1", position = PlayerPosition.ATTAQUANT, skillRating = 8.0),
            PlayerEntity(id = 8, name = "A2", position = PlayerPosition.ATTAQUANT, skillRating = 7.0),
            PlayerEntity(id = 9, name = "P1", position = PlayerPosition.POLYVALENT, skillRating = 7.5),
            PlayerEntity(id = 10, name = "P2", position = PlayerPosition.POLYVALENT, skillRating = 6.0)
        )

        val result = TeamBalancer.generateTeams(players)

        assertEquals(5, result.blueTeam.size)
        assertEquals(5, result.orangeTeam.size)
        assertEquals(1, result.blueTeam.count { it.position == PlayerPosition.GARDIEN })
        assertEquals(1, result.orangeTeam.count { it.position == PlayerPosition.GARDIEN })
        assertTrue("Expected tight skill delta, was ${result.skillDelta}", result.skillDelta <= 1.5)
    }

    @Test
    fun `applies recovery factor when player is recovering`() {
        val p = PlayerEntity(id = 1, name = "Nabil", skillRating = 8.0, status = PlayerStatus.EN_REPRISE)
        assertEquals(6.8, p.effectiveRating(0.85), 0.01)
    }
}
