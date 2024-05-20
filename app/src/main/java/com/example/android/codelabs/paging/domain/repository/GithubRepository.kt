package com.example.android.codelabs.paging.domain.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.android.codelabs.paging.data.GithubRemoteMediator
import com.example.android.codelabs.paging.data.api.GithubService
import com.example.android.codelabs.paging.data.db.RepoDatabase
import com.example.android.codelabs.paging.domain.model.Repo
import kotlinx.coroutines.flow.Flow
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
    suspend fun getSearchResultStream(query: String): Flow<PagingData<Repo>> {

        // appending '%' so we can allow other characters to be before and after the query string
        val dbQuery = "%${query.replace(' ', '%')}%"
        // pagingSourceFactory is a lambda that creates a PagingSource using the reposByName method from RepoDao.
        // This queries the local Room database for repositories matching the search query.
        val pagingSourceFactory = { repoDatabase.reposDao().reposByName(dbQuery) }
        //Pager is a component of the Paging 3 library that handles loading data from a data source and providing it to the UI in pages.
        //PagingConfig specifies the configuration for the paging, like the page size.
        // GithubRemoteMediator which is responsible for fetching data from the network and caching it in the local database if needed.
        val cachedDataExists = repoDatabase.reposDao().hasReposForQuery(dbQuery) > 0
        return if (cachedDataExists) {
            Pager(
                config = PagingConfig(
                    pageSize = NETWORK_PAGE_SIZE,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = pagingSourceFactory
            ).flow
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

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}