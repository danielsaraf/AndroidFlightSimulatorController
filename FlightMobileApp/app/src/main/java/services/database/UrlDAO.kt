package services.database

import androidx.room.*


@Dao
interface UrlDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUrl(url: UrlEntity): Long

    @Query("SELECT * FROM UrlEntity ORDER BY lastTimeUsed DESC LIMIT 5")
    suspend fun readTop5Urls(): List<UrlEntity>
}