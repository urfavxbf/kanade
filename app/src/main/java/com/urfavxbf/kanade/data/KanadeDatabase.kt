package com.urfavxbf.kanade.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_tracks")
data class SavedTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val artwork: String?,
    val durationMs: Long,
    val source: String,
    val album: String,
    val folder: String,
)

@Entity(tableName = "favorites")
data class Favorite(@PrimaryKey val trackId: String, val addedAt: Long)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "trackId"], indices = [Index("trackId")])
data class PlaylistItem(val playlistId: Long, val trackId: String, val position: Int)

@Entity(tableName = "plays", indices = [Index("trackId"), Index("playedAt")])
data class PlayEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val playedAt: Long = System.currentTimeMillis(),
    val listenMs: Long = 0,
)

data class TopTrack(val trackId: String, val plays: Int)

@Dao
interface MusicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrack(track: SavedTrack)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE trackId = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT trackId FROM favorites")
    fun favorites(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert
    suspend fun createPlaylist(playlist: Playlist): Long

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun playlists(): Flow<List<Playlist>>

    @Query("DELETE FROM playlist_items WHERE playlistId = :id")
    suspend fun clearPlaylistItems(id: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(item: PlaylistItem)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_items WHERE playlistId = :id")
    suspend fun nextPosition(id: Long): Int

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeItem(playlistId: Long, trackId: String)

    @Query("SELECT saved_tracks.* FROM playlist_items JOIN saved_tracks ON saved_tracks.id = playlist_items.trackId WHERE playlistId = :id ORDER BY playlist_items.position")
    fun playlistTracks(id: Long): Flow<List<SavedTrack>>

    @Insert
    suspend fun addPlay(entry: PlayEntry): Long

    @Query("UPDATE plays SET listenMs = :elapsed WHERE id = :id")
    suspend fun updatePlay(id: Long, elapsed: Long)

    @Query("SELECT trackId, COUNT(*) AS plays FROM plays GROUP BY trackId ORDER BY plays DESC LIMIT :limit")
    fun topTracks(limit: Int): Flow<List<TopTrack>>

    @Query("SELECT saved_tracks.* FROM plays JOIN saved_tracks ON plays.trackId = saved_tracks.id GROUP BY plays.trackId ORDER BY MAX(plays.playedAt) DESC LIMIT 50")
    fun recentTracks(): Flow<List<SavedTrack>>

    @Query("SELECT COUNT(*) FROM plays")
    fun totalPlays(): Flow<Int>

    @Query("SELECT COALESCE(SUM(listenMs), 0) FROM plays")
    fun totalListenMs(): Flow<Long>

    @Query("SELECT saved_tracks.* FROM plays JOIN saved_tracks ON plays.trackId = saved_tracks.id GROUP BY plays.trackId ORDER BY COUNT(*) DESC LIMIT 50")
    fun topPlayedTracks(): Flow<List<SavedTrack>>

    @Query("SELECT saved_tracks.* FROM saved_tracks JOIN favorites ON favorites.trackId = saved_tracks.id ORDER BY favorites.addedAt DESC")
    fun favoriteTracks(): Flow<List<SavedTrack>>
}

@Database(
    entities = [SavedTrack::class, Favorite::class, Playlist::class, PlaylistItem::class, PlayEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class KanadeDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile private var instance: KanadeDatabase? = null
        fun open(context: Context): KanadeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, KanadeDatabase::class.java, "kanade-library.db"
                ).build().also { instance = it }
            }
    }
}
