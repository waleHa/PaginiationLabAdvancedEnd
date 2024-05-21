package com.example.android.codelabs.paging.ui

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.example.android.codelabs.paging.data.network.NetworkBroadcastReceiver
import com.example.android.codelabs.paging.databinding.ActivitySearchBinding
import com.example.android.codelabs.paging.ui.adapter.ReposAdapter
import com.example.android.codelabs.paging.ui.adapter.ReposLoadStateAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchRepositoriesViewModel by viewModels()
    private lateinit var binding: ActivitySearchBinding
    private lateinit var networkBroadcastReceiver: NetworkBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val repoAdapter = ReposAdapter()
        networkBroadcastReceiver = NetworkBroadcastReceiver()

        val header = ReposLoadStateAdapter { repoAdapter.retry() }
        binding.list.adapter = repoAdapter.withLoadStateHeaderAndFooter(
            header = header,
            footer = ReposLoadStateAdapter { repoAdapter.retry() }
        )

        lifecycleScope.launch {
            viewModel.pagingDataFlow.collectLatest(repoAdapter::submitData)
        }

        binding.searchRepo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateRepoListFromInput()
                true
            } else {
                false
            }
        }

        binding.searchRepo.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateRepoListFromInput()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            viewModel.state
                .map { it.query }
                .distinctUntilChanged()
                .collect { binding.searchRepo.setText(it) }
        }

        lifecycleScope.launch {
            repoAdapter.loadStateFlow.collect { loadState ->
                val isListEmpty =
                    loadState.refresh is LoadState.NotLoading && repoAdapter.itemCount == 0
                binding.emptyList.isVisible = isListEmpty
                binding.list.isVisible = !isListEmpty
                binding.progressBar.isVisible = loadState.mediator?.refresh is LoadState.Loading
                binding.retryButton.isVisible =
                    loadState.mediator?.refresh is LoadState.Error && repoAdapter.itemCount == 0
                val errorState = loadState.source.append as? LoadState.Error
                    ?: loadState.source.prepend as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error
                errorState?.let {
                    Toast.makeText(
                        this@SearchActivity,
                        "\uD83D\uDE28 Wooops ${it.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.retryButton.setOnClickListener { repoAdapter.retry() }
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) viewModel.accept(UiAction.Scroll(currentQuery = binding.searchRepo.text.toString()))
            }
        })

        networkBroadcastReceiverHandler()
    }

    private fun networkBroadcastReceiverHandler() {
        // Register the broadcast receiver for connectivity changes
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkBroadcastReceiver, intentFilter)

        // Observe network connectivity
        networkBroadcastReceiver.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                Toast.makeText(this, "Internet connected", Toast.LENGTH_LONG).show()
                binding.inputLayout.visibility = View.VISIBLE
                binding.keywordPicker.visibility = View.GONE
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
                binding.inputLayout.visibility = View.GONE
                binding.keywordPicker.visibility = View.VISIBLE
                loadOfflineKeywords()
            }
        }
    }

    private fun loadOfflineKeywords() {
        // Fetch distinct keywords from the local database
        lifecycleScope.launch {
            val keywords = viewModel.getOfflineKeywords()

            val adapter = ArrayAdapter(this@SearchActivity, android.R.layout.simple_spinner_item, keywords)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.keywordPicker.adapter = adapter

            // Set the item selected listener for the spinner
            binding.keywordPicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val selectedKeyword = keywords[position]
                    viewModel.accept(UiAction.Search(query = selectedKeyword))
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver
        unregisterReceiver(networkBroadcastReceiver)
    }

    private fun updateRepoListFromInput() {
        binding.searchRepo.text?.trim().let {
            if (it?.isNotEmpty() == true) {
                binding.list.scrollToPosition(0)
                viewModel.accept(UiAction.Search(query = it.toString()))
            }
        }
    }
}