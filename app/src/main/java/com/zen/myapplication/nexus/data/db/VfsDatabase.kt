package com.zen.myapplication.nexus.data.db

import androidx.room.*
import android.content.Context

/**
 * Metadata for a single game asset stored in the VFS.
 */
@Entity(
    tableName = "game_assets",
    primaryKeys = ["gameId", "relativePath"]
)
data class AssetEntry(
    val gameId: String,       // Root URI of the game (the "authority" for this game session)
    val relativePath: String, // Normalized lowercase relative path (e.g., "img/system/window.png")
    val assetUri: String,     // Persistent SAF URI or local path for the file
    val lastModified: Long,   // Timestamp for cache validation
    val size: Long            // File size for header generation
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

@Database(entities = [AssetEntry::class], version = 2, exportSchema = false)
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
                )
                .fallbackToDestructiveMigration() // Reset during alpha/rewrite phase
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
