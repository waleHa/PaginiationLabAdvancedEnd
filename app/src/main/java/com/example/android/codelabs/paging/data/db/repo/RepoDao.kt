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

package com.example.android.codelabs.paging.data.db.repo

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.android.codelabs.paging.domain.localdatasource.RepoLocalDataSource

@Dao
interface RepoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repoLocalDataSources: List<RepoLocalDataSource>)

    //The reposByName method takes a search query (queryString) and returns a PagingSource.
    //PagingSource is a component in Paging 3 library that loads pages of data from a data source, which in this case is the local Room database.
    //The SQL query searches for repositories whose name or description matches the queryString. It returns the results ordered by the number of stars (in descending order) and then by name (in ascending order).

    @Query(
        "SELECT * FROM repos WHERE " +
                "name LIKE :queryString OR description LIKE :queryString " +
                "ORDER BY stars DESC, name ASC"
    )
    fun reposByName(queryString: String): PagingSource<Int, RepoLocalDataSource>

    // New method to count the number of repos matching the query
    @Query(
        "SELECT COUNT(*) FROM repos WHERE " +
                "name LIKE :queryString OR description LIKE :queryString"
    )
    suspend fun hasReposForQuery(queryString: String): Int

    @Query("DELETE FROM repos")
    suspend fun clearRepos()

//    @Query("SELECT DISTINCT name FROM repos WHERE name IS NOT NULL AND name != '' ORDER BY name")
//    suspend fun getDistinctKeywords(): List<String>
}
