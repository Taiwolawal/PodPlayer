package com.raywenderlich.podplay.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.services.ItunesService
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_podcast.*


class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener {

    companion object {
        private val TAG = "javaClass.simpleName"
    }

    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)

    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        val searchMenuItem = menu?.findItem(R.id.search_item)
        val searchView = searchMenuItem?.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        return true
    }

    fun performSearch(term: String) {
//        val itunesService = ItunesService.instance
//        val itunesRepo = ItunesRepo(itunesService)
//        itunesRepo.searchByTerm(term) {
//            Log.i(TAG, "Results = $it")
//        }

        showProgressBar()
        searchViewModel.searchPodcasts(term) { results ->
            hideProgressBar()
            toolbar.title = term
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
    }

    private fun updateControls() {
        podcastRecyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(podcastRecyclerView.context, layoutManager.orientation)
        podcastRecyclerView.addItemDecoration(dividerItemDecoration)
        podcastListAdapter = PodcastListAdapter(null, this, this)
        podcastRecyclerView.adapter = podcastListAdapter
    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        TODO("Not yet implemented")
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }
    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }




}