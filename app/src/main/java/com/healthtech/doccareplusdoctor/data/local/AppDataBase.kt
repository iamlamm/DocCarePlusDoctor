package com.healthtech.doccareplusdoctor.data.local

//import com.health tech.doccareplusdoctor.data.local.converter.TimePeriodConverter
import androidx.room.Database
import androidx.room.RoomDatabase
import com.healthtech.doccareplusdoctor.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1
)

//@TypeConverters(TimePeriodConverter::class)
abstract class AppDataBase : RoomDatabase()