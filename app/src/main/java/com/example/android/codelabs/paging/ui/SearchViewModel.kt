package com.example.android.codelabs.paging.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.android.codelabs.paging.MyApplication
import com.example.android.codelabs.paging.data.repository.GithubRepository
import com.example.android.codelabs.paging.domain.localdatasource.RepoLocalDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SearchRepositoriesViewModel @Inject constructor(
    private val repository: GithubRepository,
    private val savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {

    val state: StateFlow<UiState>
    val pagingDataFlow: Flow<PagingData<UiModel>>
    val accept: (UiAction) -> Unit

    init {
        val initialQuery: String = savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY
        val lastQueryScrolled: String = savedStateHandle.get(LAST_QUERY_SCROLLED) ?: DEFAULT_QUERY
        val actionStateFlow = MutableSharedFlow<UiAction>()
        val searches = actionStateFlow
            .filterIsInstance<UiAction.Search>()
            .distinctUntilChanged()
            .onStart { emit(UiAction.Search(query = initialQuery)) }
        val queriesScrolled = actionStateFlow
            .filterIsInstance<UiAction.Scroll>()
            .distinctUntilChanged()
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
            .onStart { emit(UiAction.Scroll(currentQuery = lastQueryScrolled)) }

        pagingDataFlow = searches
            .flatMapLatest { searchRepo(it.query) }
            .cachedIn(viewModelScope)

        state = combine(
            searches,
            queriesScrolled,
            ::Pair
        ).map { (search, scroll) ->
            UiState(
                query = search.query,
                lastQueryScrolled = scroll.currentQuery,
                hasNotScrolledForCurrentSearch = search.query != scroll.currentQuery
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UiState()
            )

        accept = { action -> viewModelScope.launch { actionStateFlow.emit(action) } }
    }

    override fun onCleared() {
        savedStateHandle[LAST_SEARCH_QUERY] = state.value.query
        savedStateHandle[LAST_QUERY_SCROLLED] = state.value.lastQueryScrolled
        super.onCleared()
    }

    suspend fun getOfflineKeywords(): List<String> {
        return repository.getOfflineKeywords().map { it.keyword }
    }

    private suspend fun searchRepo(queryString: String): Flow<PagingData<UiModel>> {
        return if (isConnected()) {
            repository.getSearchResultStream(queryString)
                .map { pagingData -> pagingData.map { UiModel.RepoItem(it) } }
                .map {
                    it.insertSeparators { before, after ->
                        when {
                            after == null -> null
                            before == null -> UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                            before.roundedStarCount > after.roundedStarCount -> {
                                if (after.roundedStarCount >= 1) UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                                else UiModel.SeparatorItem("< 10.000+ stars")
                            }

                            else -> null
                        }
                    }
                }
        } else {
            repository.getSearchResultStreamFromDataBase(queryString)
                .map { pagingData -> pagingData.map { UiModel.RepoItem(it) } }
                .map {
                    it.insertSeparators { before, after ->
                        when {
                            after == null -> null
                            before == null -> UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                            before.roundedStarCount > after.roundedStarCount -> {
                                if (after.roundedStarCount >= 1) UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                                else UiModel.SeparatorItem("< 10.000+ stars")
                            }

                            else -> null
                        }
                    }
                }
        }
    }

    private fun isConnected(): Boolean {
        val connectivityManager =
            getApplication<MyApplication>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnectedOrConnecting == true
        }
    }
}


sealed class UiAction {
    data class Search(val query: String) : UiAction()
    data class Scroll(val currentQuery: String) : UiAction()
}

data class UiState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    val hasNotScrolledForCurrentSearch: Boolean = false
)

sealed class UiModel {
    data class RepoItem(val repoLocalDataSource: RepoLocalDataSource) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}

private val UiModel.RepoItem.roundedStarCount: Int
    get() = this.repoLocalDataSource.stars / 10_000

private const val LAST_QUERY_SCROLLED: String = "last_query_scrolled"
private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val DEFAULT_QUERY = "Android"
