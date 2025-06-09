package net.iessochoa.pablolopez.mercaelx
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline

import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.GeoPoint
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.lang.Exception
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private lateinit var btnRoute: FloatingActionButton
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var selectedDestination: LatLng? = null
    private var currentPolyline: Polyline? = null
    private lateinit var btnShowDetails: Button
    private var selectedMarkerData: Map<*, *>? = null
    private val todosLosMarkers = mutableListOf< Marker>()
    private lateinit var btnFilterCategory: FloatingActionButton

    private val db = FirebaseFirestore.getInstance()

    override fun attachBaseContext(newBase: Context) {
        val context = applyAppLocale(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createMapFragment()
        setupLocationUpdates()

        btnRoute = findViewById(R.id.btnRoute)
        btnShowDetails = findViewById(R.id.btnShowDetails)
        val btnNearby = findViewById<FloatingActionButton>(R.id.btnNearbyPlaces)
        btnFilterCategory = findViewById<FloatingActionButton>(R.id.btnFilterCategory)

        btnFilterCategory.setOnClickListener {
            mostrarDialogoFiltrarPorCategoria()
        }

        btnNearby.setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    val markersCercanos = todosLosMarkers.map { marker ->
                        val distancia = calcularDistancia(userLatLng, marker.position)
                        MarkerConDistancia(marker, distancia)
                    }.sortedBy { it.distancia }
                        .take(5)

                    mostrarMenuLugaresCercanos(markersCercanos)
                } else {
                    Toast.makeText(this, getString(R.string.no_se_pudo_obtener_ubicacion), Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnRoute.visibility = View.GONE
        btnShowDetails.visibility = View.GONE
        findViewById<Button>(R.id.btnClearRoute).visibility = View.GONE

        btnRoute.setOnClickListener {
            selectedDestination?.let {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    drawRouteTo(it)
                } else {
                    Toast.makeText(this, getString(R.string.permiso_ubicacion_no_concedido), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun calcularDistancia(start: LatLng, end: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0].toDouble() // en metros
    }
    private fun mostrarMenuLugaresCercanos(lista: List<MarkerConDistancia>) {
        val nombres = lista.map { "${it.marker.title} (${String.format("%.0f", it.distancia)} m)" }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.selecciona_lugar_cercano))
        builder.setItems(nombres.toTypedArray()) { _, which ->
            val seleccionado = lista[which].marker
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(seleccionado.position, 18f))
            seleccionado.showInfoWindow()
            selectedDestination = seleccionado.position
            selectedMarkerData = seleccionado.tag as? Map<*, *>
            btnRoute.visibility = View.VISIBLE
            btnShowDetails.visibility = View.VISIBLE
            findViewById<Button>(R.id.btnClearRoute).visibility = View.VISIBLE
        }
        builder.show()
    }
    private fun mostrarDialogoFiltrarPorCategoria() {
        val categorias = arrayOf("Todos", "Religioso", "Cultural", "Restaurante")

        AlertDialog.Builder(this)
            .setTitle("Selecciona una categoría")
            .setItems(categorias) { _, which ->
                val seleccionada = categorias[which].lowercase()
                filtrarMarkersPorCategoria(seleccionada)
            }
            .show()
    }
    private fun filtrarMarkersPorCategoria(categoria: String) {
        todosLosMarkers.forEach { it.isVisible = false }

        if (categoria == "todos") {
            todosLosMarkers.forEach { it.isVisible = true }
        } else {
            todosLosMarkers.forEach { marker ->
                val lugar = marker.tag as? Map<*, *>
                val categoriaMap = lugar?.get("categoria") as? Map<*, *>
                val categoriaMarker = categoriaMap?.get(Locale.getDefault().language) as? String
                    ?: categoriaMap?.get("es") as? String

                if (categoriaMarker?.lowercase()?.contains(categoria) == true) {
                    marker.isVisible = true
                }
            }
        }
    }


    private fun createMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomGesturesEnabled = true

        map.setOnMyLocationClickListener(this)
        map.setOnMyLocationButtonClickListener(this)

        limitCameraToElche()
        loadMarkers()

        val btnZoomIn = findViewById<Button>(R.id.btnZoomIn)
        val btnZoomOut = findViewById<Button>(R.id.btnZoomOut)
        val btnClearRoute = findViewById<Button>(R.id.btnClearRoute)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnZoomIn.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomIn())
        }

        btnZoomOut.setOnClickListener {
            val currentZoom = map.cameraPosition.zoom
            if (currentZoom > map.minZoomLevel) {
                map.animateCamera(CameraUpdateFactory.zoomTo(currentZoom - 1))
            }
        }

        btnClearRoute.setOnClickListener {
            currentPolyline?.remove()
            currentPolyline = null
            btnRoute.visibility = View.GONE
            btnShowDetails.visibility = View.GONE
            btnClearRoute.visibility = View.GONE
        }

        map.setOnMarkerClickListener { marker ->
            selectedDestination = marker.position
            selectedMarkerData = marker.tag as? Map<*, *>

            btnRoute.visibility = View.VISIBLE
            btnShowDetails.visibility = View.VISIBLE
            btnClearRoute.visibility = View.VISIBLE

            marker.showInfoWindow()
            map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
            true
        }

        btnShowDetails.setOnClickListener {
            selectedMarkerData?.let { lugar ->
                val lang = Locale.getDefault().language

                fun getLocalized(map: Map<*, *>?, key: String): String {
                    val data = lugar[key] as? Map<*, *>
                    return data?.get(lang) as? String
                        ?: data?.get("es") as? String
                        ?: getString(resources.getIdentifier("${key}_no_disponible", "string", packageName))
                }

                val intent = Intent(this, LugarDetalleActivity::class.java).apply {
                    putExtra("nombre", getLocalized(lugar, "nombre"))
                    putExtra("descripcion", getLocalized(lugar, "descripcion"))
                    putExtra("direccion", getLocalized(lugar, "direccion"))
                    putExtra("horario", lugar["horario"] as? String ?: getString(R.string.horario_no_disponible))
                    putExtra("precio", lugar["precio"] as? String ?: getString(R.string.precio_no_disponible))
                    putExtra("categoria", getLocalized(lugar, "categoria"))
                    putExtra("imageUrl", lugar["imageUrl"] as? String ?: "")
                }
                startActivity(intent)
            } ?: Toast.makeText(this, getString(R.string.no_datos_lugar), Toast.LENGTH_SHORT).show()
        }

        enableMyLocation()
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun drawRouteTo(destination: LatLng) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val origin = "${it.latitude},${it.longitude}"
                val dest = "${destination.latitude},${destination.longitude}"
                val apiKey = "AIzaSyAcFOzrna9ap_vjfyP_g4JYxxcluz-5TFs"

                val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$dest&mode=walking&key=$apiKey"

                Thread {
                    try {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.connect()
                        val response = connection.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)

                        val status = json.getString("status")
                        if (status == "OK") {
                            val points = json.getJSONArray("routes")
                                .getJSONObject(0)
                                .getJSONObject("overview_polyline")
                                .getString("points")

                            val decodedPath = decodePoly(points)
                            runOnUiThread {
                                currentPolyline?.remove()
                                currentPolyline = map.addPolyline(
                                    PolylineOptions()
                                        .addAll(decodedPath)
                                        .color(ContextCompat.getColor(this, R.color.elche_green))
                                        .width(10f)
                                )
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this, "Error en la respuesta de la API: $status", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            Toast.makeText(this, "Error al trazar la ruta: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
        }
    }


    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    private fun loadMarkers() {
        val lang = Locale.getDefault().language

        db.collection("MercaElx")
            .document("Lugares")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val lugares = document.get("lugaresArray") as? List<Map<String, Any>>
                    lugares?.forEach { lugar ->
                        val geoPoint = lugar["latitud"] as? GeoPoint
                        val position = geoPoint?.let { LatLng(it.latitude, it.longitude) }

                        val nombreMap = lugar["nombre"] as? Map<String, String>
                        val nombre = nombreMap?.get(lang) ?: nombreMap?.get("es") ?: getString(R.string.nombre_no_disponible)

                        position?.let {
                            val marker = map.addMarker(
                                MarkerOptions()
                                    .position(it)
                                    .title(nombre)
                            )
                            marker?.tag = lugar
                            marker?.let { todosLosMarkers.add(it) }

                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_cargar_lugares), Toast.LENGTH_LONG).show()
            }
    }

    private fun isPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (isPermissionsGranted()) {
            map.isMyLocationEnabled = true
            centerMapOnMyLocation()
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun centerMapOnMyLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f))
            } else {
                Toast.makeText(this, getString(R.string.no_se_pudo_obtener_ubicacion), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    // saveLocationToFirestore(location)
                }
            }
        }
    }

    private fun limitCameraToElche() {
        val bounds = LatLngBounds(LatLng(38.2300, -0.7400), LatLng(38.3200, -0.6600))
        map.setLatLngBoundsForCameraTarget(bounds)
        map.setMinZoomPreference(12f)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(38.2670, -0.6980), 13f))
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Toast.makeText(this, getString(R.string.ir_a_ajustes), Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, getString(R.string.permiso_denegado), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, getString(R.string.centrado), Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(
            this, getString(R.string.estas_en, location.latitude, location.longitude), Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    override fun onResume() {
        super.onResume()

        applyAppLocale(this)

        for (marker in todosLosMarkers) {
            marker.remove()
        }
        todosLosMarkers.clear()
        loadMarkers()
    }

    fun applyAppLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "es") ?: "es"
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }
}
