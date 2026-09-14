package com.vigia.ai.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history_table")
data class AnalysisHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // GUARD (Auto) ou VERIFY (Manuel / Partage)
    val contentSummary: String,
    val riskScore: Int,
    val riskLevel: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<AnalysisHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AnalysisHistory)

    @Delete
    suspend fun deleteHistory(history: AnalysisHistory)

    @Query("DELETE FROM history_table")
    suspend fun clearHistory()
}

@Database(entities = [AnalysisHistory::class], version = 1, exportSchema = false)
abstract class VigiaDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: VigiaDatabase? = null

        fun getDatabase(context: Context): VigiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VigiaDatabase::class.java,
                    "vigia_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
