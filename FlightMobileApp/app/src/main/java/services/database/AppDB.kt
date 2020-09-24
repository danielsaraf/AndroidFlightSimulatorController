package services.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [(UrlEntity::class)], version = 1)
abstract class AppDB : RoomDatabase() {
    abstract fun urlDAO(): UrlDAO
}