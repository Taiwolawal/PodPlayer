package com.raywenderlich.podplay.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.services.FeedService
import com.raywenderlich.podplay.ui.PodcastActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class EpisodeUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val EPISODE_CHANNEL_ID = "podplay_episodes_channel"
        const val EXTRA_FEED_URL = "PodcastFeedUrl"
    }

    override suspend fun doWork(): Result = coroutineScope {
        // 2
        val job = async {
// 3
            val db = PodPlayDatabase.getInstance(applicationContext)
            val repo = PodcastRepo(FeedService.instance, db.podcastDao())
// 4
            repo.updatePodcastEpisodes { podcastUpdates ->
// 5
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }
// 6
                for (podcastUpdate in podcastUpdates) {
                    displayNotification(podcastUpdate)
                }
            }
        }
// 7
        job.await()
        Result.success()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if
                (notificationManager.getNotificationChannel(EPISODE_CHANNEL_ID) == null) {
            val channel = NotificationChannel(EPISODE_CHANNEL_ID, "Episodes", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun displayNotification(podcastInfo: PodcastRepo.PodcastUpdateInfo) {

        val contentIntent = Intent(applicationContext,
            PodcastActivity::class.java)
        contentIntent.putExtra(EXTRA_FEED_URL, podcastInfo.feedUrl)
        val pendingContentIntent =
            PendingIntent.getActivity(applicationContext, 0,
                contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
// 2
        val notification =
            NotificationCompat.Builder(applicationContext,
                EPISODE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_episode_icon)
                .setContentTitle(applicationContext.getString(
                    R.string.episode_notification_title))
                .setContentText(applicationContext.getString(
                    R.string.episode_notification_text,
                    podcastInfo.newCount, podcastInfo.name))
                .setNumber(podcastInfo.newCount)
                .setAutoCancel(true)
                .setContentIntent(pendingContentIntent)
                .build()
// 4
        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
// 5
        notificationManager.notify(podcastInfo.name, 0, notification)
    }

}