package com.zerui.hackathonthing

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlinx.android.synthetic.main.activity_wikipedia_webview.*

class WikipediaWebview : AppCompatActivity() {
    override fun onBackPressed() {
        if (webview.copyBackForwardList().getCurrentIndex() > 0) {
            webview.goBack()
        } else {
            // Your exit alert code, or alternatively line below to finish
            super.onBackPressed() // finishes activity
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        var darkOn = false
        when (PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            "theme",
            "light"
        )) {
            "black" -> {
                setTheme(R.style.DarkTheme); darkOn = true
            }
            "dark" -> {
                setTheme(R.style.GreyTheme); darkOn = true
            }
            else -> {
                setTheme(R.style.LightTheme)
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wikipedia_webview)
        setSupportActionBar(findViewById(R.id.infoToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webview.settings.javaScriptEnabled = true
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) && darkOn) WebSettingsCompat.setForceDark(
            webview.settings,
            WebSettingsCompat.FORCE_DARK_ON
        )

        webview.loadUrl(intent.getStringExtra("url").toString())
        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return false
            }
        }
    }
}