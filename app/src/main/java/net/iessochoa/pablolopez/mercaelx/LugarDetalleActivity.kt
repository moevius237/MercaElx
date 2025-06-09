package net.iessochoa.pablolopez.mercaelx

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
class LugarDetalleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lugar_detalle)

        val nombre = intent.getStringExtra("nombre") ?: "Nombre no disponible"
        val descripcion = intent.getStringExtra("descripcion") ?: "Descripción no disponible"
        val direccion = intent.getStringExtra("direccion") ?: "Dirección no disponible"
        val horario = intent.getStringExtra("horario") ?: "Horario no disponible"
        val precio = intent.getStringExtra("precio") ?: "Precio no disponible"
        val categoria = intent.getStringExtra("categoria") ?: "Categoría no disponible"
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""

        findViewById<TextView>(R.id.tvNombre).text = nombre
        findViewById<TextView>(R.id.tvDescripcion).text = descripcion
        findViewById<TextView>(R.id.tvDireccion).text = direccion
        findViewById<TextView>(R.id.tvHorario).text = horario
        findViewById<TextView>(R.id.tvPrecio).text = precio
        findViewById<TextView>(R.id.tvCategoria).text = categoria
        val btnSalir = findViewById<Button>(R.id.btnSalir)
        btnSalir.setOnClickListener {
            finish()
        }

        val imageView = findViewById<ImageView>(R.id.ivLugar)
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error_image)
            .into(imageView)
    }
}
