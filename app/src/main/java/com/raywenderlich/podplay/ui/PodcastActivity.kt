package com.raywenderlich.podplay.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.services.FeedService
import com.raywenderlich.podplay.services.ItunesService
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import com.raywenderlich.podplay.worker.EpisodeUpdateWorker
import kotlinx.android.synthetic.main.activity_podcast.*
import java.util.concurrent.TimeUnit


class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener,
    PodcastDetailsFragment.OnPodcastDetailsListener {

    companion object {
        private val TAG = "javaClass.simpleName"
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_EPISODE_UPDATE_JOB = "com.raywenderlich.podplay.episodes"
    }

    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private val podcastViewModel by viewModels<PodcastViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
        addBackStackListener()
        setupPodcastListView()
        scheduleJobs()

    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        searchMenuItem = menu.findItem(R.id.search_item)
        searchMenuItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }
            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean{
                showSubscribedPodcasts()
                return true
            }
        })


        val searchView = searchMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        if (supportFragmentManager.backStackEntryCount > 0) {
            podcastRecyclerView.visibility = View.INVISIBLE
        }
        if (podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }
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

        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        if (podcastFeedUrl != null) {
            podcastViewModel.setActivePodcast(podcastFeedUrl) {
                it?.let { podcastSummaryView ->
                    onShowDetails(podcastSummaryView) }
            }
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

        val rssService = FeedService.instance
        val db = PodPlayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(rssService, podcastDao)
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
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
// 2
        showProgressBar()
// 3
        podcastViewModel.getPodcast(podcastSummaryViewData) {
// 4
            hideProgressBar()
            if (it != null) {
// 5
                showDetailsFragment()
            } else {
                showError("Error loading feed $feedUrl")
            }
        }
            }
// 6


    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }
    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as
                PodcastDetailsFragment?

        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    private fun showDetailsFragment() {
// 1
        val podcastDetailsFragment = createPodcastDetailsFragment()
// 2
        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer,
            podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment").commit()
// 3
        podcastRecyclerView.visibility = View.INVISIBLE
// 4
        searchMenuItem.isVisible = false
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    private fun showSubscribedPodcasts() {
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null) {
            toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null) {
                showSubscribedPodcasts()
            }
        })
    }

    private fun scheduleJobs() {
// 1
        val constraints: Constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()
// 2
        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
// 3
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TAG_EPISODE_UPDATE_JOB,
            ExistingPeriodicWorkPolicy.REPLACE, request)
    }


}