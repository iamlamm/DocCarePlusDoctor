package com.healthtech.doccareplusdoctor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
//import com.healthtech.doccareplusdoctor.data.local.converter.TimePeriodConverter
import com.healthtech.doccareplusdoctor.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1
)

//@TypeConverters(TimePeriodConverter::class)
abstract class AppDataBase : RoomDatabase()