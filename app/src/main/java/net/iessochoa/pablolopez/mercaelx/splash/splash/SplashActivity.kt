package net.iessochoa.pablolopez.mercaelx.splash.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import net.iessochoa.pablolopez.mercaelx.MainActivity
import net.iessochoa.pablolopez.mercaelx.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logo)
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val subtitleText = findViewById<TextView>(R.id.subtitleText)

        // Configurar estado inicial de las vistas
        logo.scaleX = 0.8f
        logo.scaleY = 0.8f
        logo.alpha = 0.9f
        welcomeText.alpha = 0f
        subtitleText.alpha = 0f

        // Animación del logo
        logo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(1200)
            .setInterpolator(OvershootInterpolator())
            .start()

        // Animación escalonada de los textos
        welcomeText.postDelayed({
            welcomeText.animate()
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 600)

        subtitleText.postDelayed({
            subtitleText.animate()
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 1200)

        // Redirigir después de 5 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 5000)
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Por favor espera mientras se carga la aplicación", Toast.LENGTH_SHORT).show()
        // No llamar a super para mantener el bloqueo
    }
}