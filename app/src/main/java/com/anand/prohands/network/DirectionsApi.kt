package com.anand.prohands.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Directions API interface for fetching route data.
 */
interface DirectionsApi {

    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): Response<DirectionsResponse>
}

/**
 * Response models for Google Directions API
 */
data class DirectionsResponse(
    val routes: List<Route> = emptyList(),
    val status: String = ""
)

data class Route(
    val legs: List<Leg> = emptyList(),
    val overview_polyline: OverviewPolyline? = null
)

data class Leg(
    val distance: TextValue? = null,
    val duration: TextValue? = null,
    val start_address: String = "",
    val end_address: String = "",
    val steps: List<Step> = emptyList()
)

data class Step(
    val distance: TextValue? = null,
    val duration: TextValue? = null,
    val html_instructions: String = "",
    val maneuver: String? = null,
    val polyline: OverviewPolyline? = null,
    val travel_mode: String = ""
)

data class TextValue(
    val text: String = "",
    val value: Int = 0
)

data class OverviewPolyline(
    val points: String = ""
)

/**
 * Utility to decode Google's encoded polyline format
 */
object PolylineDecoder {
    fun decode(encoded: String): List<com.google.android.gms.maps.model.LatLng> {
        val poly = ArrayList<com.google.android.gms.maps.model.LatLng>()
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

            val p = com.google.android.gms.maps.model.LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }

        return poly
    }
}

