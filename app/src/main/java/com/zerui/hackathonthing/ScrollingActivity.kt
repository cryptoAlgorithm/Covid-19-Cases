package com.zerui.hackathonthing

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_scrolling.*
import kotlinx.android.synthetic.main.content_scrolling.*

class ScrollingActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var ref: DatabaseReference
    private lateinit var pref: SharedPreferences
    private var activityMenu: Menu? = null
    private var listState: Parcelable? = null
    val data: ArrayList<List<String>> = ArrayList()

    private fun updateData() {
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var useAdd = false
                if (data.isEmpty()) {
                    useAdd = true
                }
                var i = 0 // .add won't work here as it'll just keep on adding and not delete old data
                dataSnapshot.children.forEach {
                    val dataList = it.value.toString().drop(1).dropLast(1)
                        .split(", (?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).toMutableList()
                    dataList.add(it.key.toString()) // toString is safer than !!
                    if (useAdd) {
                        data.add(dataList)
                    }
                    else {
                        data[i] = dataList
                    }
                    i += 1
                }
                indeterminableLoading.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                fab.isEnabled = true
                recyclerTable.adapter?.notifyDataSetChanged()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Snackbar.make(
                    findViewById<View>(R.id.content),
                    "Failed to update data. Please try again later.",
                    Snackbar.LENGTH_LONG
                ).show()
                indeterminableLoading.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                fab.isEnabled = true
            }
        }
        indeterminableLoading.visibility = View.VISIBLE
        ref.addListenerForSingleValueEvent(postListener)
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
        setContentView(R.layout.activity_scrolling)
        if (!this::database.isInitialized) {
            try {
                // Well Firebase has a bad habit of spitting out errors
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            } catch (ex: Exception) {
                // Nothing here!!!
            }
            database = FirebaseDatabase.getInstance().reference
        }
        ref = database.child("nush-hackathon")
        // locations = database.child("locations")
        pref = applicationContext.getSharedPreferences("rickrollPref", Context.MODE_PRIVATE)
        setSupportActionBar(findViewById(R.id.toolbar))
        toolbar.title = title
        fab.setOnClickListener { // Can add view -> for the view var
            // Snackbar.make(view, "Updating Data...", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            fab.isEnabled = false // Don't allow user to double click button
            swipeRefreshLayout.isRefreshing = true
            updateData()
        }

        // Loads data into the ArrayList
        updateData()

        // Creates a vertical Layout Manager
        recyclerTable.apply {
            this.layoutManager = LinearLayoutManager(this@ScrollingActivity)
            this.adapter = DataUpdateAdapter(data, this@ScrollingActivity)
        }

        swipeRefreshLayout.setOnRefreshListener {
            updateData()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        activityMenu = menu
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        val searchView: SearchView = menu.findItem(R.id.searchBar).actionView as SearchView

        // searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        // searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.queryHint = "Country"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String): Boolean {
                var i = 0
                val newDataSet: ArrayList<List<String>> = ArrayList()
                var numVisible = 0
                while (i < data.size) {
                    if (data[i][5].contains(query, ignoreCase = true)) {
                        newDataSet.add(data[i])
                        numVisible += 1
                    }
                    i += 1
                }
                if (numVisible == 0) {
                    // TODO: Show a no results message
                }
                recyclerTable.adapter = DataUpdateAdapter(newDataSet, this@ScrollingActivity)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
        })
        return true
    }

    override fun onBackPressed() {
        val searchView: SearchView = activityMenu?.findItem(R.id.searchBar)?.actionView as SearchView
        if (!searchView.isIconified) {
            searchView.isIconified = true
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this@ScrollingActivity, SettingsActivity::class.java))
                return true
            }
            R.id.toMusic -> {
                startActivity(Intent(this@ScrollingActivity, MusicPlayer::class.java))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(outState)

        // Save list state
        listState = recyclerTable.layoutManager?.onSaveInstanceState()
        outState.putParcelable("listState", listState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        listState = savedInstanceState.getParcelable("listState")
    }
}