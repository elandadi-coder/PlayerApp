package com.foot22.samedi.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.FeeMode
import com.foot22.samedi.data.model.MatchStatus
import com.foot22.samedi.data.model.PaymentMethod
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.data.model.TeamSide
import com.foot22.samedi.data.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nickname: String = "",
    val phone: String = "",
    val position: PlayerPosition = PlayerPosition.POLYVALENT,
    val skillRating: Double = 6.0,
    val technique: Int = 6,
    val stamina: Int = 6,
    val defense: Int = 6,
    val finishing: Int = 6,
    val useDetailedRating: Boolean = false,
    val status: PlayerStatus = PlayerStatus.ACTIF,
    val injuryNote: String = "",
    val injuryDateEpoch: Long? = null,
    val expectedReturnText: String = "",
    val isGuest: Boolean = false,
    val createdAtEpoch: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = nickname.trim().ifEmpty { name.trim() }

    /** Effective rating used by the team balancer, accounting for post-injury recovery. */
    fun effectiveRating(recoveryFactor: Double = 0.85): Double {
        val base = if (useDetailedRating) {
            (technique + stamina + defense + finishing) / 4.0
        } else {
            skillRating
        }
        return if (status == PlayerStatus.EN_REPRISE) {
            (base * recoveryFactor).coerceAtLeast(1.0)
        } else {
            base
        }
    }
}

@Serializable
@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTimeIso: String,
    val location: String = "Terrain habituel",
    val status: MatchStatus = MatchStatus.OPEN,
    val maxPlayers: Int = 12,
    val pitchCost: Double = 360.0,
    val feeMode: FeeMode = FeeMode.SPLIT_PITCH,
    val fixedFeePerPlayer: Double = 30.0,
    val blueScore: Int? = null,
    val orangeScore: Int? = null,
    val mvpPlayerId: Long? = null,
    val notes: String = ""
)

@Serializable
@Entity(
    tableName = "match_participations",
    primaryKeys = ["matchId", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["matchId"]),
        Index(value = ["playerId"])
    ]
)
data class MatchParticipationEntity(
    val matchId: Long,
    val playerId: Long,
    val attendance: AttendanceStatus = AttendanceStatus.ABSENT,
    val teamSide: TeamSide? = null,
    val snapshotSkill: Double? = null,
    val goals: Int = 0,
    val assists: Int = 0,
    val injuredInMatch: Boolean = false,
    val feeDue: Double = 0.0,
    val feePaid: Double = 0.0,
    val isPaid: Boolean = false,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val feeExempt: Boolean = false
)

@Serializable
@Entity(tableName = "cash_transactions")
data class CashTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpoch: Long = System.currentTimeMillis(),
    val type: TransactionType,
    val amount: Double,
    val label: String,
    val matchId: Long? = null,
    val playerId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH
)
