package services.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class UrlEntity {
    @PrimaryKey
    var url: String = ""

    @ColumnInfo
    var lastTimeUsed: Long = System.currentTimeMillis()
}