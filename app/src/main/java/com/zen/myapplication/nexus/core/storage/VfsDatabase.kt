package com.zen.myapplication.nexus.core.storage

import androidx.room.*
import android.content.Context

@Entity(
    tableName = "game_assets",
    primaryKeys = ["gameId", "relativePath"]
)
data class AssetEntry(
    val gameId: String,       // Root URI of the game
    val relativePath: String, // Normalized lowercase relative path
    val assetUri: String,     // Persistent SAF URI for the file
    val lastModified: Long    // Timestamp for validation
)

@Dao
interface AssetDao {
    @Query("SELECT * FROM game_assets WHERE gameId = :gameId AND relativePath = :path LIMIT 1")
    suspend fun getAsset(gameId: String, path: String): AssetEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntry>)

    @Query("DELETE FROM game_assets WHERE gameId = :gameId")
    suspend fun clearGameAssets(gameId: String)

    @Query("SELECT COUNT(*) FROM game_assets WHERE gameId = :gameId")
    suspend fun getAssetCount(gameId: String): Int
}

@Database(entities = [AssetEntry::class], version = 1, exportSchema = false)
abstract class VfsDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: VfsDatabase? = null

        fun getDatabase(context: Context): VfsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VfsDatabase::class.java,
                    "nexus_vfs_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
