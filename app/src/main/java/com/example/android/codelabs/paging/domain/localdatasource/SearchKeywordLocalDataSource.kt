package com.example.android.codelabs.paging.domain.localdatasource

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_keywords")
data class SearchKeywordLocalDataSource(
    @PrimaryKey @ColumnInfo(name = "keyword") val keyword: String
)