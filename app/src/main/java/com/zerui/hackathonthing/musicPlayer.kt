package com.zerui.hackathonthing

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_music_player.*
import kotlinx.android.synthetic.main.one_row.view.*
import java.util.*

class MusicPlayer : AppCompatActivity() {
    @ExperimentalStdlibApi
    private fun String.capitalizeWords(): String = // Addon function for String class
        split(" ").joinToString(" ") { it.toLowerCase(Locale.ROOT).capitalize(Locale.ROOT) }

    private lateinit var ref: StorageReference

    @ExperimentalStdlibApi
    private fun listRaw() {
        val inflater = layoutInflater
        val songTable = songsList

        ref = FirebaseStorage.getInstance("gs://nush-hackathon.appspot.com").reference

        ref.listAll()
            .addOnSuccessListener { listResult ->
                listResult.prefixes.forEach { _ ->
                    /* NOTES:
                    _ = prefix
                    All the prefixes under listRef.
                    You may call listAll() recursively on them.
                    I won't actually need this function
                    */
                }

                listResult.items.forEach { item ->
                    // All the items under listRef.
                    val row = inflater.inflate(R.layout.one_row, songTable, false)
                    val songName = item.name.replace("_", " ").capitalizeWords().substringBeforeLast('.', "")
                    row.country.text = songName
                    val param = row.country.layoutParams as ViewGroup.MarginLayoutParams
                    val margin = (5 *
                            Resources.getSystem().displayMetrics.density).toInt()
                    param.setMargins(margin*3, margin, margin, margin) // Left margin should be 15dp
                    row.country.layoutParams = param
                    row.countryImg.visibility = View.GONE // Allows me to reuse the one row xml without rewriting it
                    row.totalCases.visibility = View.GONE
                    // Replace underscores with spaces and capitalise (Somehow resources cannot have any of those)
                    row.setOnClickListener {
                        if (bgMusicPlayer.isBuffering) {
                            Snackbar.make(
                                it,
                                "Another song is currently buffering. Please wait",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        else {
                            buffering.visibility = View.VISIBLE
                            item.downloadUrl.addOnSuccessListener {
                                // Got the download URL for the music
                                val intent = Intent(this@MusicPlayer, bgMusicPlayer::class.java)
                                    .apply {
                                        action = "com.zerui.hackathonthing.action.CHANGEMUSIC"
                                    }
                                    .putExtra("songName", songName)
                                    .putExtra("url", it.toString())
                                // .putExtra("srcResource", field.getInt(field))

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(intent)
                                } else {
                                    startService(intent)
                                }
                                // Music will stop when source is changed. So start it again
                                val playIntent =
                                    Intent(this@MusicPlayer, bgMusicPlayer::class.java)
                                        .apply {
                                            action = "com.zerui.hackathonthing.action.PLAY"
                                        }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(playIntent)
                                } else {
                                    startService(playIntent)
                                }
                                // PausePlay won't work as it thinks it was not playing
                                pausePlay.setImageDrawable(
                                    ContextCompat.getDrawable(applicationContext, R.drawable.ic_round_pause_24)
                                )

                                songTitle.text = songName
                            }.addOnFailureListener {
                                // Handle any errors
                                Snackbar.make(
                                    findViewById<View>(R.id.content),
                                    "Failed to decode URL",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    songTable.addView(row)
                    seekBar.progress = 0 // Reset it
                }
            }
            .addOnFailureListener {
                // Uh-oh, an error occurred!
                // Smol problem lol
                Snackbar.make(
                    findViewById<View>(R.id.content),
                    "No internet connectivity",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

    private fun playPause() {
        if (!bgMusicPlayer.isPaused && bgMusicPlayer.isPlaying) {
            val intent = Intent(this@MusicPlayer, bgMusicPlayer::class.java)
                .apply {
                    action = "com.zerui.hackathonthing.action.PAUSE"
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            }
            else {
                startService(intent)
            }
            pausePlay.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_round_play_arrow_24))
        }
        else if (!bgMusicPlayer.isPlaying) {
            // No song is queued
            Snackbar.make(
                findViewById<View>(R.id.pausePlay),
                "Please choose a song from the list first",
                Snackbar.LENGTH_SHORT
            ).show()
        }
        else {
            val intent = Intent(this@MusicPlayer, bgMusicPlayer::class.java)
                .apply {
                    action = "com.zerui.hackathonthing.action.PLAY"
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            }
            else {
                startService(intent)
            }
            pausePlay.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_round_pause_24))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateElements() {
        // Updates progress bar and text
        if (bgMusicPlayer.isPlaying) {
            seekBar.isEnabled = true
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                seekBar.setProgress(bgMusicPlayer.progress, true)
            }
            else {
                seekBar.progress = bgMusicPlayer.progress
            }
            val millisecondsElapsed = bgMusicPlayer.getElapsed
            val millisecondsRemaining = bgMusicPlayer.getRemaining
            elapsed.text = "${(millisecondsElapsed/1000/60).toString().padStart(2, '0')}:${(millisecondsElapsed/1000%60).toString().padStart(2, '0')}" // Minutes:Seconds
            remaining.text = "${(millisecondsRemaining/1000/60).toString().padStart(2, '0')}:${(millisecondsRemaining/1000%60).toString().padStart(2, '0')}"
        }
        else {
            seekBar.progress = 0
            seekBar.isEnabled = false
            pausePlay.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_round_play_arrow_24))
        }

        if (!bgMusicPlayer.isBuffering) {
            buffering.visibility = View.INVISIBLE
        }
    }

    @kotlin.ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        when (PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            "theme",
            "light"
        )) {
            "black" -> {
                setTheme(R.style.DarkTheme)
            }
            "dark" -> {
                setTheme(R.style.GreyTheme)
            }
            else -> {
                setTheme(R.style.LightTheme)
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        setSupportActionBar(findViewById(R.id.musicToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        listRaw()

        updateElements() // Need to update once first
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                updateElements()
                handler.postDelayed(this, 500)
            }
        }, 500)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, pos: Int, fromUser: Boolean) {
                if (fromUser) {
                    bgMusicPlayer.progress = pos
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {} // Nothing here

            override fun onStopTrackingTouch(seekBar: SeekBar) {} // Nothing here too
        })

        if (!bgMusicPlayer.isPaused && bgMusicPlayer.isPlaying) {
            // The music service was playing
            pausePlay.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_round_pause_24))
        }
        songTitle.text = bgMusicPlayer.songName

        pausePlay.setOnClickListener {
            playPause()
        }
        stopFAB.setOnClickListener {
            seekBar.progress = 0
            elapsed.text = "--:--"; remaining.text = "--:--" // Clear both TextViews
            songTitle.text = "Select a song from the list" // Reset to default song
            val intent = Intent(this@MusicPlayer, bgMusicPlayer::class.java)
                .apply {
                    action = "com.zerui.hackathonthing.action.STOP"
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            }
            else {
                startService(intent)
            }
            pausePlay.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_round_play_arrow_24))
        }
        repeatToggle.setOnCheckedChangeListener { _, isChecked ->
            bgMusicPlayer.repeat = isChecked
        }
    }
}