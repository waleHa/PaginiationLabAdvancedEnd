package com.example.android.codelabs.paging.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.example.android.codelabs.paging.data.GithubRemoteMediator
import com.example.android.codelabs.paging.data.network.GithubService
import com.example.android.codelabs.paging.data.db.RepoDatabase
import com.example.android.codelabs.paging.domain.localdatasource.RepoLocalDataSource
import com.example.android.codelabs.paging.domain.localdatasource.SearchKeywordLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Repository class that works with local and remote data sources.
 */
class GithubRepository @Inject constructor(
    private val service: GithubService,
    private val repoDatabase: RepoDatabase
) {

    /**
     * Search repositories whose names match the query, exposed as a stream of data that will emit
     * every time we get more data from the network.
     * getSearchResultStream is a function that takes a search query and returns a flow of PagingData<Repo>.
     *
     */
    suspend fun getSearchResultStream(query: String): Flow<PagingData<RepoLocalDataSource>> {

        val dbQuery = "%${query.replace(' ', '%')}%"
        val pagingSourceFactory = { repoDatabase.reposDao().reposByName(dbQuery) }
        val cachedDataExists = repoDatabase.reposDao().hasReposForQuery(dbQuery) > 0
        // Save the keyword to the database
        repoDatabase.searchKeywordDao().insertKeyword(SearchKeywordLocalDataSource(query))

        return if (cachedDataExists) {
            pagerFromDatabase(pagingSourceFactory)
        } else {
            @OptIn(ExperimentalPagingApi::class)
            Pager(
                config = PagingConfig(
                    pageSize = NETWORK_PAGE_SIZE,
                    enablePlaceholders = false
                ),
                remoteMediator = GithubRemoteMediator(query, service, repoDatabase),
                pagingSourceFactory = pagingSourceFactory
            ).flow
        }
    }

    suspend fun getSearchResultStreamFromDataBase(query: String): Flow<PagingData<RepoLocalDataSource>> {
        val dbQuery = "%${query.replace(' ', '%')}%"
        val pagingSourceFactory = { repoDatabase.reposDao().reposByName(dbQuery) }
        val cachedDataExists = repoDatabase.reposDao().hasReposForQuery(dbQuery) > 0
        return if (cachedDataExists) {
            pagerFromDatabase(pagingSourceFactory)
        } else {
            flow {
            }
        }
    }

    private fun pagerFromDatabase(pagingSourceFactory: () -> PagingSource<Int, RepoLocalDataSource>): Flow<PagingData<RepoLocalDataSource>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    suspend fun getOfflineKeywords() = repoDatabase.searchKeywordDao().getDistinctKeywords()

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }


}