package com.zerui.hackathonthing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


const val PLAY = "com.zerui.hackathonthing.action.PLAY"
const val PAUSE = "com.zerui.hackathonthing.action.PAUSE"
const val CHANGEMUSIC = "com.zerui.hackathonthing.action.CHANGEMUSIC"
const val STOP = "com.zerui.hackathonthing.action.STOP"

class bgMusicPlayer : Service() {
    private fun getMyActivityNotification(songName: String): NotificationCompat.Builder{
        // The PendingIntent to launch our activity if the user selects
        // this notification
        val notificationIntent = Intent(this, MusicPlayer::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("COVID-19")
            .setContentText("$songName")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()

        when (intent.action) {
            PLAY -> {
                if (isPaused) { // Safety check
                    startForeground(1, getMyActivityNotification("Click for more options").build())
                    mediaPlayer.start()
                    isPaused = false
                }
            }
            PAUSE -> {
                if (isPlaying && !isPaused) { // Check if the player was actually playing
                    mediaPlayer.pause()
                    isPaused = true
                }
            }
            STOP -> {
                mediaPlayer.release()
                isPlaying = false // Must reset var
                isPaused = false
                stopForeground(true) // Kill itself
                stopSelf() // Close immediately
            }
            CHANGEMUSIC -> {
                if (isPlaying) {
                    mediaPlayer.release() // Destory the MediaPlayer instance
                }
                val url = intent.getStringExtra("url").toString()
                Log.w("url", url)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(url)
                    setOnCompletionListener {
                        if (repeat) {
                            isPaused = true
                            it.start()
                            isPaused = false
                        }
                        else {
                            // Return everything to default
                            it.release() // Don't hog up system resources
                            returnDefault()
                        }
                    }
                    isBuffering = true
                    prepare() // might take long! (for buffering, etc)
                    isBuffering = false
                }
                isPlaying = true
                isPaused = true

//                with(NotificationManagerCompat.from(this)) {
//                    // notificationId is a unique int for each notification that you must define
//                    notify(
//                        1,
//                        getMyActivityNotification(intent.getStringExtra("songName")!!).build()
//                    )
//                }
                // val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // mNotificationManager.notify(1, getMyActivityNotification("Hey!").build())
                songName = intent.getStringExtra("songName").toString()
            }
        }

        return START_STICKY // Restart service if killed
    }

    private fun returnDefault() {
        isPlaying = false
        isPaused = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Music Player Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            serviceChannel.setSound(null, null)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        var isPlaying: Boolean = false
        var isPaused: Boolean = false
        var songName: String = "Select a song from the list"
        var repeat: Boolean = false
        var isBuffering: Boolean = false
        lateinit var mediaPlayer: MediaPlayer
        var progress: Int
            get() = currentProgress()
            set(value) = if (this::mediaPlayer.isInitialized) { mediaPlayer.seekTo((value/100.0* mediaPlayer.duration).toInt()) } else {}
        var getElapsed: Long
            get() = mediaPlayer.currentPosition.toLong()
            set(_) = TODO()
        var getRemaining: Long
            get() = (mediaPlayer.duration - mediaPlayer.currentPosition).toLong()
            set(_) = TODO()

        private fun currentProgress(): Int {
            if (isPlaying) { // MediaPlayer has been init
                return ((mediaPlayer.currentPosition / mediaPlayer.duration.toDouble()) * 100.0).toInt()
            }
            else {
                return 0 // Playing has not even started
            }
        }

        const val CHANNEL_ID = "MusicPlayerService"
    }
}