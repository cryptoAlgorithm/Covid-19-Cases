package com.zerui.hackathonthing

import android.content.res.Resources
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import net.glxn.qrgen.android.QRCode

class SettingsDump : AppCompatActivity() {
    private val WIDTH = (250 * Resources.getSystem().displayMetrics.density).toInt()
    private val HEIGHT = WIDTH

    override fun onCreate(savedInstanceState: Bundle?) {
        when (PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            "theme",
            "light"
        )) {
            "black" -> {
                setTheme(R.style.settingsDark)
            }
            "dark" -> {
                setTheme(R.style.settingsGrey)
            }
            else -> {
                setTheme(R.style.settingsLight)
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_dump)
        setSupportActionBar(findViewById(R.id.import_export_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val imageView: ImageView = findViewById(R.id.QRImg)

        val bitmap = QRCode.from(
            PreferenceManager.getDefaultSharedPreferences(applicationContext).all.toString()
                .replace(
                    "=",
                    ":"
                )
        ).withSize(WIDTH, HEIGHT).bitmap()

        val radius = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .getString("radius", "14")!!.toInt()
        imageView.setImageBitmap(RoundedTransformation(radius, 0).transform(bitmap))
    }
}