package com.foot22.samedi.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY status ASC, name ASC")
    fun observeAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY name ASC")
    suspend fun getAllPlayersOnce(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>): List<Long>

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY dateTimeIso DESC")
    fun observeAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches ORDER BY dateTimeIso DESC")
    suspend fun getAllMatchesOnce(): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE status IN ('OPEN', 'TEAMS_READY') ORDER BY dateTimeIso ASC LIMIT 1")
    fun observeUpcomingMatch(): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE status IN ('OPEN', 'TEAMS_READY') ORDER BY dateTimeIso ASC LIMIT 1")
    suspend fun getUpcomingMatchOnce(): MatchEntity?

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Long): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()
}

@Dao
interface ParticipationDao {
    @Query("SELECT * FROM match_participations")
    fun observeAllParticipations(): Flow<List<MatchParticipationEntity>>

    @Query("SELECT * FROM match_participations")
    suspend fun getAllParticipationsOnce(): List<MatchParticipationEntity>

    @Query("SELECT * FROM match_participations WHERE matchId = :matchId")
    fun observeByMatch(matchId: Long): Flow<List<MatchParticipationEntity>>

    @Query("SELECT * FROM match_participations WHERE matchId = :matchId")
    suspend fun getByMatchOnce(matchId: Long): List<MatchParticipationEntity>

    @Query("SELECT * FROM match_participations WHERE matchId = :matchId AND playerId = :playerId")
    suspend fun getOne(matchId: Long, playerId: Long): MatchParticipationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(participation: MatchParticipationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(participations: List<MatchParticipationEntity>)

    @Query("DELETE FROM match_participations WHERE matchId = :matchId AND playerId = :playerId")
    suspend fun deleteOne(matchId: Long, playerId: Long)

    @Query("DELETE FROM match_participations")
    suspend fun deleteAllParticipations()
}

@Dao
interface CashTransactionDao {
    @Query("SELECT * FROM cash_transactions ORDER BY dateEpoch DESC")
    fun observeAllTransactions(): Flow<List<CashTransactionEntity>>

    @Query("SELECT * FROM cash_transactions ORDER BY dateEpoch DESC")
    suspend fun getAllTransactionsOnce(): List<CashTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CashTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<CashTransactionEntity>)

    @Delete
    suspend fun deleteTransaction(transaction: CashTransactionEntity)

    @Query("DELETE FROM cash_transactions WHERE matchId = :matchId AND playerId = :playerId")
    suspend fun deleteByMatchAndPlayer(matchId: Long, playerId: Long)

    @Query("DELETE FROM cash_transactions")
    suspend fun deleteAllTransactions()
}
