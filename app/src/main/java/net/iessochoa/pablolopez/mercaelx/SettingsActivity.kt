package net.iessochoa.pablolopez.mercaelx

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerLanguage: Spinner
    private lateinit var buttonConfirm: Button
    private lateinit var buttonCancel: Button

    private val languageList = listOf(
        LanguageItem("Español", "es", R.drawable.flag_es),
        LanguageItem("English", "en", R.drawable.flag_en),
        LanguageItem("中文", "zh", R.drawable.flag_zh),
        LanguageItem("Русский", "ru", R.drawable.flag_ru),
        LanguageItem("Deutsch", "de", R.drawable.flag_de),
        LanguageItem("Français", "fr", R.drawable.flag_fr)
    )

    private var selectedLangCode = "es"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        buttonConfirm = findViewById(R.id.buttonConfirm)
        buttonCancel = findViewById(R.id.buttonCancel)

        val adapter = LanguageAdapter(this, languageList)
        spinnerLanguage.adapter = adapter

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val savedLang = prefs.getString("language", "es") ?: "es"
        val savedIndex = languageList.indexOfFirst { it.code == savedLang }
        if (savedIndex >= 0) {
            spinnerLanguage.setSelection(savedIndex)
            selectedLangCode = savedLang
        }

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedLangCode = languageList[position].code
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        buttonConfirm.setOnClickListener {
            val editor = getSharedPreferences("settings", MODE_PRIVATE).edit()
            editor.putString("language", selectedLangCode)
            editor.apply()
            setLocaleAndRestartApp(selectedLangCode)
        }

        buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun setLocaleAndRestartApp(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
