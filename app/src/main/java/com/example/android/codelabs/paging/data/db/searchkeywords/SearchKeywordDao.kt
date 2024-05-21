package com.example.android.codelabs.paging.data.db.searchkeywords

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.android.codelabs.paging.domain.localdatasource.SearchKeywordLocalDataSource

@Dao
interface SearchKeywordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKeyword(searchKeywordLocalDataSource: SearchKeywordLocalDataSource)

    @Query("SELECT * FROM search_keywords ORDER BY keyword ASC")
    suspend fun getDistinctKeywords(): List<SearchKeywordLocalDataSource>
}