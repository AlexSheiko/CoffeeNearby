package com.alexsheiko.coffeenearby

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import com.alexsheiko.coffeenearby.R.string
import com.google.android.gms.location.*
import com.google.android.gms.location.places.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers.io
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

private const val QUERY_STARBUCKS = "Starbucks"

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private lateinit var geoDataClient: GeoDataClient
    private lateinit var placeDetectionClient: PlaceDetectionClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val placesAdapter by lazy { PlacesAdapter() }
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()

        // Init user location provider.
        initMyLocationProviderWithPermissionCheck()
        // (The long method name above is caused by required "WithPermissionCheck" suffix
        // added by permission dispatcher library. Long names are not the best coding style)

        // Init nearby place loader that uses Google Places API
        initPlaceFinder()

        // Add async operation to disposables to be able to stop and release resources onDestroy
        disposables.add(
                // Load last known user location without requesting location updates, to save battery
                loadMyLocation()
                        // Load all places that match "Starbucks" query
                        .flatMap { myLocation -> loadPlacesNearby(myLocation, QUERY_STARBUCKS) }
                        // Convert response to a list of places
                        .flatMapIterable { response -> response.asIterable() }
                        // For each place, load additional info to show in a preview
                        .flatMap { loadPlaceInfo(it.placeId) }
                        // Combine all places with details back to list
                        .toList()
                        // Execute all these operations on the background IO thread
                        .subscribeOn(io())
                        // Get notified about the results on the main UI thread
                        .observeOn(mainThread())
                        // Afterwards, hide progress indicator
                        .doAfterSuccess {
                            statusTextView.text = null
                            buttonRetry.visibility = GONE
                        }
                        // Subscribe to observable chain to get results
                        .subscribe(
                                {
                                    // On success, show loaded places in the recycler view
                                    placeList ->
                                    placesAdapter.addAll(placeList)
                                },
                                {
                                    // On error, notify user and log error
                                    handleError(it)
                                }
                        )
        )
    }

    override fun onDestroy() {
        // Stop observable chains and free up resources
        disposables.clear()
        // Let Android system shutdown other activity services
        super.onDestroy()
    }

    private fun handleError(error: Throwable) {
        // On error, notify user
        statusTextView.text = getString(string.error_loading_failed)
        // Provide a hint about what the problem is, if available
        hintTextView.text = error.message
        // Enable Retry button
        buttonRetry.visibility = VISIBLE
        buttonRetry.setOnClickListener { restartApp() }
        // Log exception with more details
        error.printStackTrace()
    }

    private fun restartApp() {
        // Restart current activity
        val intent = intent
        finish()
        startActivity(intent)
        // Make transition smoother by removing screen re-opening animation
        overridePendingTransition(0, 0)
    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    fun initMyLocationProvider() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // Permission request is handled in initMyLocationProvider(), suppressing the warning here
    @SuppressLint("MissingPermission")
    // Load last known user location to find coffee shops nearby
    private fun loadMyLocation(): Observable<Location> {
        return Observable.create { emitter ->
            val locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult?) {
                    if (result != null) {
                        // Publish current user location to query places
                        val location = result.lastLocation
                        emitter.onNext(location)
                        // It's enough for this app to get a location update only once
                        emitter.onComplete()
                        fusedLocationClient.removeLocationUpdates(this)
                    } else {
                        emitter.onError(IllegalStateException("Current location is unknown"))
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, null /* Looper */)
        }
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            setHasFixedSize(true)
            adapter = placesAdapter
        }
    }

    private fun initPlaceFinder() {
        // Construct a GeoDataClient.
        geoDataClient = Places.getGeoDataClient(this)

        // Construct a PlaceDetectionClient.
        placeDetectionClient = Places.getPlaceDetectionClient(this)
    }

    private fun loadPlacesNearby(nearLocation: Location, query: String)
            : Observable<AutocompletePredictionBufferResponse> {
        return Observable.create { emitter ->
            val request = geoDataClient.getAutocompletePredictions(query, null, null)
            request.addOnSuccessListener {
                emitter.onNext(it)
                emitter.onComplete()
            }
            request.addOnFailureListener { emitter.onError(it) }
        }
    }

    private fun loadPlaceInfo(placeId: String?): Observable<Place> {
        // If no Id provided, return nothing and log error
        if (placeId == null) {
            Log.e(javaClass.name, "Encountered a place with no Id")
            return Observable.empty()
        }
        // Get place info like name, address, images
        return Observable.create { emitter ->
            val request = geoDataClient.getPlaceById(placeId)
            request.addOnSuccessListener {
                // Use most likely match for place Id
                emitter.onNext(it.single())
                emitter.onComplete()
            }
            request.addOnFailureListener { emitter.onError(it) }
        }
    }
}