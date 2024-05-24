package Activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.surajverma.weatherwise.R
import com.surajverma.weatherwise.databinding.ActivityMainBinding

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val cityName=getCityName()
        binding.progressBar.visibility=View.VISIBLE

        if (cityName != null) {
            fetchWeatherData(cityName)
        }

        searchCity()

        ////////////////////////////////////////////

        binding.locationButton.setOnClickListener {
            vibrator.vibrate(50)
            binding.progressBar.visibility=View.VISIBLE
            requestLocation()
//            Log.e("locationTag", "Show city location here")

        }


    }

    private fun requestLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
      //  Log.d("LocationDebug", "Requesting location...")

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
         //   Log.d("LocationDebug", "Location permission not granted, requesting...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
         //   Log.d("LocationDebug", "Location permission granted, requesting location updates...")
            locationManager?.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null)
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {

           // Log.d("LocationDebug", "Received location update: ${location.latitude}, ${location.longitude}")

            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (addresses != null) {
               // Log.d("LocationDebug", "Number of addresses received: ${addresses.size}")
                if (addresses.isNotEmpty()) {
                    val cityName = addresses[0]?.locality
                   // Log.d("LocationDebug", "Extracted city name: $cityName")

                    binding.searchBar.setQuery("$cityName", false)
                    if (cityName != null) {
                        fetchWeatherData(cityName)
                    }


                } else {
                    Log.e("locationTag", "Unable to get city name from location")
                }
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {
            AlertDialog.Builder(this@MainActivity)
                .setMessage("Location services are disabled. Would you like to enable them?")
                .setPositiveButton("Yes") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
    }



    private fun searchCity() {
        binding.progressBar.visibility=View.VISIBLE
        val searchView=binding.searchBar
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    fetchWeatherData(query)
                    updateCityName(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }

        })
    }

    private fun fetchWeatherData(cityName: String) {
        val retrofit=Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)

        val response= retrofit.getWeatherData(cityName, "c0737edc0312a6019f36aeeca3becbec", "metric")
        response.enqueue(object : Callback<WeatherApp>{
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                val responseBody=response.body()
                if(response.isSuccessful && responseBody!=null){
                    val temperature= responseBody.main.temp
                    val humidity= responseBody.main.humidity
                    val windSpeed=responseBody.wind.speed
                    val sunrise=responseBody.sys.sunrise.toLong()
                    val sunset=responseBody.sys.sunset.toLong()
                    val seaLevel=responseBody.main.pressure
                    val maxTemp=responseBody.main.temp_max
                    val minTemp=responseBody.main.temp_min
                    val condition=responseBody.weather.firstOrNull()?.main?:"unknown"


                    binding.temp.setText("$temperature°C")
                    binding.weather.setText("$condition")
                    binding.maxTemp.setText("Max Temp: $maxTemp °C")
                    binding.minTemp.setText(("Min Temp: $minTemp °C"))
                    binding.humidity.setText("$humidity%")
                    binding.windSpeed.setText("$windSpeed m/s")
                    binding.sunrise.setText("${time(sunrise)}")
                    binding.sunset.setText("${time(sunset)}")
                    binding.sea.setText("$seaLevel hPa")
                    binding.condition.setText("$condition".uppercase())
                    binding.cityName.setText("$cityName")
                    binding.day.setText(dayName(System.currentTimeMillis()))
                    binding.date.setText(date())

                    changeBackgroundAccordingToWeatherConditions(condition)
                    binding.progressBar.visibility=View.GONE
                }
                else{
                    Toast.makeText(this@MainActivity, "Unable to Fetch Data", Toast.LENGTH_SHORT).show()
                    fetchWeatherData("Dhanbad")
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun dayName(timestamp: Long): String{
        val simpleDateFormat=SimpleDateFormat("EEEE", Locale.getDefault())
        return simpleDateFormat.format(Date())

    }

    private fun date(): String{
        val simpleDateFormat=SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return simpleDateFormat.format(Date())

    }

    private fun time(timestamp: Long): String{
        val simpleDateFormat=SimpleDateFormat("HH:mm", Locale.getDefault())
        return simpleDateFormat.format(timestamp*1000)

    }

    private fun changeBackgroundAccordingToWeatherConditions(condition: String){
        when(condition){
            "Clear Sky", "Sunny", "Clear" -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)

            }

            "Haze","Partly Clouds","Clouds","Overcast","Mist", "Foggy" -> {
                binding.root.setBackgroundResource(R.drawable.cloud_background)
                binding.lottieAnimationView.setAnimation(R.raw.cloud)

            }

            "Light Rain","Moderate Rain","Heavy Rain","Drizle","Showers" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.lottieAnimationView.setAnimation(R.raw.rain)

            }

            "Light Snow","Moderate Snow","Heavy Snow","Blizzard" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.lottieAnimationView.setAnimation(R.raw.snow)

            }
            else->{
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)

            }
        }
        binding.lottieAnimationView.playAnimation()
    }

    private fun getCityName(): String?{
        val pref: SharedPreferences = getSharedPreferences("WEATHER_WISE", Context.MODE_PRIVATE)
        return pref.getString("cityName","Dhanbad")

    }

    private fun updateCityName(updatedCityName: String){
        val pref: SharedPreferences=getSharedPreferences("WEATHER_WISE",Context.MODE_PRIVATE)
        val editor=pref.edit()
        editor.putString("cityName", updatedCityName)
        editor.apply()
    }
}