package ai.vigia.app.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Cache local des analyses reelles : permet de consulter l'historique hors ligne. */
@Entity(tableName = "analyses")
data class AnalysisEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val preview: String,
    val score: Int,
    val level: String,
    val summary: String,
    val signalsJson: String,
    val sourcesJson: String,
    val aiUsed: Boolean,
    val createdAt: String,
    val syncedWithServer: Boolean
)

/** Analyses realisees hors ligne, en attente d'envoi au serveur. */
@Entity(tableName = "pending_analyses")
data class PendingAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val content: String,
    val createdAt: Long
)

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analyses ORDER BY createdAt DESC LIMIT :limit")
    fun observeAll(limit: Int = 100): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE id = :id")
    suspend fun byId(id: String): AnalysisEntity?

    @Query("SELECT COUNT(*) FROM analyses")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM analyses WHERE level = :level")
    suspend fun countByLevel(level: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AnalysisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AnalysisEntity>)

    @Query("DELETE FROM analyses WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM analyses")
    suspend fun clear()
}

@Dao
interface PendingDao {
    @Insert
    suspend fun add(item: PendingAnalysisEntity)

    @Query("SELECT * FROM pending_analyses ORDER BY createdAt ASC")
    suspend fun all(): List<PendingAnalysisEntity>

    @Query("DELETE FROM pending_analyses WHERE id = :id")
    suspend fun remove(id: Long)
}

@Database(entities = [AnalysisEntity::class, PendingAnalysisEntity::class], version = 1, exportSchema = false)
abstract class VigiaDatabase : RoomDatabase() {
    abstract fun analyses(): AnalysisDao
    abstract fun pending(): PendingDao

    companion object {
        @Volatile private var instance: VigiaDatabase? = null

        fun get(context: Context): VigiaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VigiaDatabase::class.java,
                "vigia.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
