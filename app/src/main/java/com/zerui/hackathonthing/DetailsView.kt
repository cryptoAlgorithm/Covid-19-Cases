package com.zerui.hackathonthing

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.details_view.*

class DetailsView : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var ref: DatabaseReference
    private lateinit var flagURL: String
    private lateinit var learnMoreURL: String
    private lateinit var country: String
    private lateinit var totalCases: String
    private lateinit var deaths: String
    private lateinit var recoveries: String
    private lateinit var thumbnailURL: String

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    fun updateElements() {
        if (country == "Singapore") {
            openMap.visibility = View.VISIBLE
            val openMapButton = findViewById<Button>(R.id.openMap)
            openMapButton.setOnClickListener {
                startActivity(Intent(this@DetailsView, maps::class.java))
            }
        }
        findViewById<TextView>(R.id.country).text = country
        findViewById<TextView>(R.id.cases).text = totalCases
        findViewById<TextView>(R.id.deaths).text = deaths
        findViewById<TextView>(R.id.recoveries).text = recoveries
        supportActionBar?.title = "Cases in " + country;

        val flagView = findViewById<ImageView>(R.id.flag)
        val radius = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("radius", "14")!!.toInt()

        // Log.i("radius", PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("radius", "14").toString())

        // Update image
        // Load a thumbnail first for speed, then load high res flag if present
        Picasso.get().load(thumbnailURL) // Thumbnail URL for faster loading
            .placeholder(R.drawable.ic_round_refresh_30)
            .transform(RoundedTransformation(radius, 0))
            .into(flagView, object : Callback {
                override fun onSuccess() {
                    Picasso.get()
                        .load(flagURL) // image url goes here
                        .placeholder(flagView.drawable)
                        .transform(RoundedTransformation(radius, 0))
                        .into(flagView)
                }
                override fun onError(e: Exception?) {
                    // Do nothing here
                }
            })

        val openWorldButton = findViewById<Button>(R.id.openWorld)
        openWorldButton.setOnClickListener {
            startActivity(Intent(this@DetailsView, ScrollingActivity::class.java))
        }
    }

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
        setContentView(R.layout.details_view)
        setSupportActionBar(findViewById(R.id.detailToolbar))

        val learnMoreButton = findViewById<Button>(R.id.moreInfoButton)

        if (intent.getStringExtra("cases") == null) {
            if (!this::database.isInitialized) {
                try {
                    // Prevents app from randomly crashing
                    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                } catch (ex: Exception) {
                    // Nothing here!!!
                }
                database = FirebaseDatabase.getInstance().reference
            }
            openWorld.visibility = View.VISIBLE
            country = "Singapore" // Hmm hardcoding eh
            ref = database.child("nush-hackathon/Singapore")
            val postListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var i = 0
                    dataSnapshot.children.forEach {
                        Log.i(i.toString(), it.value.toString())
                        if (i == 0) {
                            totalCases = "Total cases: " + it.value
                        }
                        else if (i == 1) {
                            deaths = "Deaths: " + it.value
                        }
                        else if (i == 2) {
                            recoveries = "Recoveries: " + it.value
                        }
                        else if (i == 3) {
                            thumbnailURL = it.value.toString()
                            flagURL = it.value.toString().replace("23px", ((232 *
                            Resources.getSystem().displayMetrics.density).toInt().toString() + "px"))
                        }
                        else {
                            learnMoreURL = it.value.toString()
                        }
                        i += 1
                    }
                    updateElements()
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    Snackbar.make(
                        findViewById<View>(R.id.content),
                        "Could not refresh data",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null).show()
                }
            }
            ref.addValueEventListener(postListener)
        }
        else {
            // Activity was launched from ScrollingActivity
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            // Update elements

            flagURL = intent.getStringExtra("flagURL").toString() // Make the String non-nullable
            learnMoreURL = intent.getStringExtra("learnMoreURL").toString() // Same here
            country = intent.getStringExtra("country").toString() // Same here
            totalCases = "Total cases: " + intent.getStringExtra("cases")
            deaths = "Deaths: " + intent.getStringExtra("deaths")
            recoveries = "Recoveries: " + intent.getStringExtra("recoveries")
            thumbnailURL = intent.getStringExtra("thumbnailURL").toString()
            updateElements()
        }
        // Picasso.get().load(flagURL).placeholder(R.drawable.ic_round_error).into(flagView)

        learnMoreButton.setOnClickListener {
            val intent = Intent(this@DetailsView, WikipediaWebview::class.java)
            intent.putExtra("url", learnMoreURL)
            startActivity(intent)
        }

        // Init AdMob SDK
        MobileAds.initialize(this@DetailsView) {}

        adView.loadAd(AdRequest.Builder().build())
    }
}