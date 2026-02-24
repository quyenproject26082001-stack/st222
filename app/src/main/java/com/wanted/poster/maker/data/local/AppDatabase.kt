package com.wanted.poster.maker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wanted.poster.maker.data.local.dao.UserDao
import com.wanted.poster.maker.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}