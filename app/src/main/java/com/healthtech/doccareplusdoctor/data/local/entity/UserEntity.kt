package com.healthtech.doccareplusdoctor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val role: String,
    val createdAt: Long,
    val avatar: String?,
    val height: Int?,
    val weight: Int?,
    val age: Int?,
    val bloodType: String?,
    val about: String?,
    val gender: String?
)