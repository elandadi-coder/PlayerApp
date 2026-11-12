package com.foot22.samedi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.FeeMode
import com.foot22.samedi.data.model.MatchStatus
import com.foot22.samedi.data.model.PaymentMethod
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.data.model.TeamSide
import com.foot22.samedi.data.model.TransactionType

class RoomEnumConverters {
    @TypeConverter fun toPosition(v: String): PlayerPosition = PlayerPosition.valueOf(v)
    @TypeConverter fun fromPosition(v: PlayerPosition): String = v.name

    @TypeConverter fun toPlayerStatus(v: String): PlayerStatus = PlayerStatus.valueOf(v)
    @TypeConverter fun fromPlayerStatus(v: PlayerStatus): String = v.name

    @TypeConverter fun toAttendance(v: String): AttendanceStatus = AttendanceStatus.valueOf(v)
    @TypeConverter fun fromAttendance(v: AttendanceStatus): String = v.name

    @TypeConverter fun toTeamSide(v: String?): TeamSide? = v?.let { TeamSide.valueOf(it) }
    @TypeConverter fun fromTeamSide(v: TeamSide?): String? = v?.name

    @TypeConverter fun toMatchStatus(v: String): MatchStatus = MatchStatus.valueOf(v)
    @TypeConverter fun fromMatchStatus(v: MatchStatus): String = v.name

    @TypeConverter fun toFeeMode(v: String): FeeMode = FeeMode.valueOf(v)
    @TypeConverter fun fromFeeMode(v: FeeMode): String = v.name

    @TypeConverter fun toPaymentMethod(v: String): PaymentMethod = PaymentMethod.valueOf(v)
    @TypeConverter fun fromPaymentMethod(v: PaymentMethod): String = v.name

    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)
    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
}

@Database(
    entities = [
        PlayerEntity::class,
        MatchEntity::class,
        MatchParticipationEntity::class,
        CashTransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomEnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun participationDao(): ParticipationDao
    abstract fun cashTransactionDao(): CashTransactionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "samedi22.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
