/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.codelabs.paging.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.android.codelabs.paging.data.db.remotekeys.RemoteKeysDao
import com.example.android.codelabs.paging.domain.localdatasource.RemoteKeysLocalDataSource
import com.example.android.codelabs.paging.data.db.repo.RepoDao
import com.example.android.codelabs.paging.domain.localdatasource.RepoLocalDataSource
import com.example.android.codelabs.paging.data.db.searchkeywords.SearchKeywordDao
import com.example.android.codelabs.paging.domain.localdatasource.SearchKeywordLocalDataSource

@Database(
    entities = [RepoLocalDataSource::class, RemoteKeysLocalDataSource::class, SearchKeywordLocalDataSource::class],
    version = 1,
    exportSchema = false
)
abstract class RepoDatabase : RoomDatabase() {

    abstract fun reposDao(): RepoDao
    abstract fun remoteKeysDao(): RemoteKeysDao
    abstract fun searchKeywordDao(): SearchKeywordDao

    companion object {

        @Volatile
        private var INSTANCE: RepoDatabase? = null

        fun getInstance(context: Context): RepoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                RepoDatabase::class.java, "Github.db"
            )
                .build()
    }
}
