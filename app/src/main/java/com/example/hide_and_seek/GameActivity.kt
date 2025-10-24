/*package com.example.hide_and_seek

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var hintButton: Button

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private var hasStartedTimer = false
    private var countdownTimer: CountDownTimer? = null
    private var remainingTime = 120_000L // 120 seconds in milliseconds

    private var hintPressCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        latitudeTextView = findViewById(R.id.latitudeText)
        longitudeTextView = findViewById(R.id.longitudeText)
        timerTextView = findViewById(R.id.timerText)
        hintButton = findViewById(R.id.hintButton)
        val backButton = findViewById<Button>(R.id.backButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Back button returns to main activity
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Hint button logic
        hintButton.setOnClickListener {
            lifecycleScope.launch {
                val prefs = applicationContext.locationDataStore.data.first()
                val name = prefs[LocationKeys.LOCATION_NAME] ?: "No name saved"
                val lat = prefs[LocationKeys.LATITUDE]?.toString() ?: "No latitude"
                val lon = prefs[LocationKeys.LONGITUDE]?.toString() ?: "No longitude"

                when (hintPressCount) {
                    0 -> {
                        Toast.makeText(this@GameActivity, "Hint: $name", Toast.LENGTH_SHORT).show()
                        subtractTime(25_000)
                    }
                    1 -> {
                        Toast.makeText(this@GameActivity, "Latitude: $lat", Toast.LENGTH_SHORT).show()
                        subtractTime(25_000)
                    }
                    2 -> {
                        Toast.makeText(this@GameActivity, "Longitude: $lon", Toast.LENGTH_SHORT).show()
                        subtractTime(25_000)
                    }
                    else -> {
                        Toast.makeText(this@GameActivity, "No more hints available!", Toast.LENGTH_SHORT).show()
                    }
                }
                hintPressCount++
            }
        }

        // Create the location request for continuous updates
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000 // update every 2 seconds
        ).setMinUpdateDistanceMeters(1f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocationUI(location)
                }
            }
        }

        // Check permissions and start location updates
        if (checkLocationPermissions()) {
            requestFreshLocation()
            startLocationUpdates()
        } else {
            requestLocationPermissions()
        }
    }

    private fun checkLocationPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED && coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestFreshLocation()
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    updateLocationUI(location)
                    Log.d("LocationUpdate", "Fresh GPS fix: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.w("LocationUpdate", "Fresh GPS location is null")
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (!checkLocationPermissions()) {
            requestLocationPermissions()
            return
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        } catch (e: SecurityException) {
            Log.e("LocationError", "Permission not granted", e)
            Toast.makeText(this, "Cannot start location updates: permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLocationUI(location: Location) {
        latitudeTextView.text = "Latitude: ${location.latitude}"
        longitudeTextView.text = "Longitude: ${location.longitude}"
        Log.d("LocationUpdate", "Lat: ${location.latitude}, Long: ${location.longitude}")

        // Start the timer the first time we get a valid location
        if (!hasStartedTimer) {
            hasStartedTimer = true
            startCountdownTimer()
        }
    }

    private fun startCountdownTimer(startTime: Long = 120_000L) {
        remainingTime = startTime
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                val secondsLeft = millisUntilFinished / 1000
                timerTextView.text = "Time: $secondsLeft"
            }

            override fun onFinish() {
                timerTextView.text = "Time: 0"
                val intent = Intent(this@GameActivity, GameOverActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.start()
    }

    private fun subtractTime(amount: Long) {
        remainingTime = (remainingTime - amount).coerceAtLeast(0)
        countdownTimer?.cancel()
        startCountdownTimer(remainingTime)
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermissions()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        countdownTimer?.cancel()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: SecurityException) {
            Log.e("LocationError", "Cannot remove updates: permission denied", e)
        }
    }
}
*/
package com.example.hide_and_seek

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class GameActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var hintButton: Button
    private lateinit var backButton: Button

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var hasStartedTimer = false
    private var countdownTimer: CountDownTimer? = null
    private var remainingTime = 120_000L
    private var hintPressCount = 0

    private var targetLat: Double? = null
    private var targetLon: Double? = null
    private var hasWon = false

    private val LAT_LON_TOLERANCE = 0.0001  // ~10 meters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        latitudeTextView = findViewById(R.id.latitudeText)
        longitudeTextView = findViewById(R.id.longitudeText)
        timerTextView = findViewById(R.id.timerText)
        hintButton = findViewById(R.id.hintButton)
        backButton = findViewById(R.id.backButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        backButton.setOnClickListener { finish() }

        // Hint button logic
        hintButton.setOnClickListener {
            lifecycleScope.launch {
                val prefs = applicationContext.locationDataStore.data.first()
                val name = prefs[LocationKeys.LOCATION_NAME] ?: "No name saved"
                val lat = prefs[LocationKeys.LATITUDE]?.toString() ?: "No latitude"
                val lon = prefs[LocationKeys.LONGITUDE]?.toString() ?: "No longitude"

                when (hintPressCount) {
                    0 -> {
                        Toast.makeText(this@GameActivity, "Hint: $name", Toast.LENGTH_SHORT).show()
                        subtractTime(25_000)
                    }
                    1 -> {
                        Toast.makeText(this@GameActivity, "Latitude: $lat", Toast.LENGTH_SHORT).show()
                        subtractTime(25_000)
                    }
                    2 -> {
                        Toast.makeText(this@GameActivity, "Longitude: $lon", Toast.LENGTH_SHORT).show()
                        subtractTime(25_000)
                    }
                    else -> {
                        Toast.makeText(this@GameActivity, "No more hints!", Toast.LENGTH_SHORT).show()
                    }
                }
                hintPressCount++
            }
        }

        // Load target coordinates
        lifecycleScope.launch {
            val prefs = applicationContext.locationDataStore.data.first()
            targetLat = prefs[LocationKeys.LATITUDE]
            targetLon = prefs[LocationKeys.LONGITUDE]
        }

        // Location request every 0.5 seconds
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            500
        ).setMinUpdateDistanceMeters(1f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocationUI(location)
                }
            }
        }

        if (checkLocationPermissions()) {
            startLocationUpdates()
        } else {
            requestLocationPermissions()
        }
    }

    private fun checkLocationPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED && coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLocationUpdates() {
        try {
            if (checkLocationPermissions()) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
            }
        } catch (e: SecurityException) {
            Log.e("LocationError", "Permission denied", e)
        }
    }

    private fun updateLocationUI(location: Location) {
        latitudeTextView.text = "Latitude: ${location.latitude}"
        longitudeTextView.text = "Longitude: ${location.longitude}"

        if (!hasStartedTimer) {
            hasStartedTimer = true
            startCountdownTimer()
        }

        checkWinCondition(location)
    }

    private fun checkWinCondition(location: Location) {
        if (hasWon) return
        val lat = targetLat
        val lon = targetLon
        if (lat != null && lon != null) {
            if (abs(location.latitude - lat) <= LAT_LON_TOLERANCE &&
                abs(location.longitude - lon) <= LAT_LON_TOLERANCE
            ) {
                hasWon = true
                countdownTimer?.cancel()
                startActivity(Intent(this, YouWinActivity::class.java))
                finish()
            }
        }
    }

    private fun startCountdownTimer(startTime: Long = 120_000L) {
        remainingTime = startTime
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                timerTextView.text = "Time: ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                timerTextView.text = "Time: 0"
                startActivity(Intent(this@GameActivity, GameOverActivity::class.java))
                finish()
            }
        }.start()
    }

    private fun subtractTime(amount: Long) {
        remainingTime = (remainingTime - amount).coerceAtLeast(0)
        countdownTimer?.cancel()
        startCountdownTimer(remainingTime)
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermissions()) startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        countdownTimer?.cancel()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: SecurityException) {
            Log.e("LocationError", "Cannot remove updates", e)
        }
    }
}
