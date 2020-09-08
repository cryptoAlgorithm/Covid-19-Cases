package com.zerui.hackathonthing

import android.content.res.Resources
import android.os.Bundle
import android.util.Base64
import android.view.KeyEvent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_settings_dump.*
import net.glxn.qrgen.android.QRCode
import com.zerui.hackathonthing.Crypto as cryptography

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

        encryptPassword.setOnKeyListener { view, i, keyEvent ->
            if ((keyEvent.action == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                val enteredText = encryptPassword.text.toString()
                if (enteredText.length >= 8) {
                    // Encrypt preferences
                    val encryptedMap = cryptography.encrypt(
                        PreferenceManager.getDefaultSharedPreferences(applicationContext).all.toString()
                            .replace(
                                "=",
                                ":"
                            ), enteredText
                    )
                    val base64IV = Base64.encodeToString(encryptedMap["iv"], Base64.NO_WRAP)
                    val base64Salt = Base64.encodeToString(encryptedMap["salt"], Base64.NO_WRAP)
                    val base64Encrypted =
                        Base64.encodeToString(encryptedMap["encrypted"], Base64.NO_WRAP)
                    val concatQRText = "$base64Encrypted,$base64IV,$base64Salt"

                    val bitmap = QRCode.from(concatQRText).withSize(WIDTH, HEIGHT).bitmap()

                    val radius = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                        .getString("radius", "14")!!.toInt()
                    val imageView: ImageView = findViewById(R.id.QRImg)
                    imageView.setImageBitmap(RoundedTransformation(radius, 0).transform(bitmap))
                } else {
                    Snackbar.make(
                        view,
                        "Your password does not fulfill the length requirement",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                true
            }
            false
        }
    }
}