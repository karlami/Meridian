package com.arubanetworks.meridiansamples

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.webkit.URLUtil
import com.arubanetworks.meridian.Meridian
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.internal.util.Dev
import com.arubanetworks.meridian.internal.util.Strings
import com.arubanetworks.meridian.location.LocationRequest
import com.arubanetworks.meridian.location.MeridianLocation
import com.arubanetworks.meridian.locationsharing.Friend
import com.arubanetworks.meridian.locationsharing.LocationSharing
import com.arubanetworks.meridian.locationsharing.LocationSharingException
import com.arubanetworks.meridian.maps.*
import com.arubanetworks.meridian.maps.directions.Directions
import com.arubanetworks.meridian.maps.directions.DirectionsDestination
import com.arubanetworks.meridian.maps.directions.DirectionsSource
import com.arubanetworks.meridian.maps.directions.Route
import com.arubanetworks.meridiansamples.utils.CropCircleTransformation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList

class LocationSharingMapFragment : MapFragment(), MapView.DirectionsEventListener, MapView.MapEventListener, MapView.MarkerEventListener {

    //private var mapView: MapView? = null
    //private val mapOptions: MapOptions? = null

    private val appKey: EditorKey? = null
    private val mapKey: EditorKey? = null

    private val savedRoute: Route? = null
    private val savedRouteStepIndex = 0
    private val isRerouting = false

    private val directions: Directions? = null
    private val fromItem: DirectionsSource? = null
    private val pendingDestination: DirectionsDestination? = null
    private val locationRequest: LocationRequest? = null

    /**
     * Friends
     */
    private var friendsList: ArrayList<Friend> = ArrayList()

    private var friendMarkers: MutableMap<String, Marker>? = HashMap()
    private var friendsTimer: Timer? = null
    private var friendsTimerTask: TimerTask? = null
    private var destinationFriend: Friend? = null
    private var currentSelectedFriendMarker: LocationSharingMarker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // we need to initialize Location Sharing first
        if (!Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
            LocationSharing.initWithAppKey(Application.APP_KEY)
        }
    }

    override fun onResume() {
        super.onResume()

        //mapView = getMapView()
        if (Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
            AlertDialog.Builder(activity!!)
                    .setMessage("You need to provide a valid editor token")
                    .setPositiveButton("OK", null)
                    .show()
            return
        }
        if (LocationSharing.shared().isUploadingServiceRunning) {
            friendsTimer = Timer()
            friendsTimerTask = object : TimerTask() {
                override fun run() {
                    LocationSharing.shared().getFriends(object : LocationSharing.Callback<List<Friend>> {
                        override fun onSuccess(result: List<Friend>) {
                            renderFriends(result)
                        }

                        override fun onError(lse: LocationSharingException) {
                            // TODO do something
                        }
                    })
                }
            }
            friendsTimer?.schedule(friendsTimerTask, Date(), 5000)
        }
    }

    override fun onPause() {
        super.onPause()

        if (friendsTimer != null) {
            friendsTimer?.cancel()
            friendsTimer = null
        }

        if (friendsTimerTask != null) {
            friendsTimerTask?.cancel()
            friendsTimerTask = null
        }
    }

    private inner class LocationSharingMarker internal constructor(context: Context?, internal val friend: Friend?, internal val selected: Boolean) :
            Marker(friend?.location?.x!!.toFloat(), friend.location?.y!!.toFloat()) {

        private val context: Context? = context?.applicationContext
        private var bitmap: Bitmap? = null

        val friendId: String?
            get() = friend?.key

        init {
            weight = 1.1f
            friend?.let {
                name = it.fullName
                val ageInMillis = System.currentTimeMillis() - it.location.timestamp.time
                this@LocationSharingMarker.context?.let {
                    details = getString(R.string.last_heard) + ": " + getFormattedAgeString(it.resources, ageInMillis)
                }
            }
        }

        override fun getCollision(): Boolean {
            return false
        }

        override fun canBeSelected(): Boolean {
            return !selected
        }

        override fun getBitmap(): Bitmap? {
            if (bitmap != null) {
                return bitmap
            }
            val SIZE_AVATAR_DP = 40

            if (activity != null) {
                if (URLUtil.isValidUrl(friend?.photoUrl)) {
                    activity!!.runOnUiThread {
                        Glide.with(activity!!.applicationContext)
                                .load(friend?.photoUrl)
                                .asBitmap()
                                .transform(CropCircleTransformation(Glide.get(activity!!.applicationContext).bitmapPool, Dev.get().dpToPix(2f)))
                                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>(Math.round(Dev.get().dpToPix(SIZE_AVATAR_DP.toFloat()).toFloat()), Math.round(Dev.get().dpToPix(SIZE_AVATAR_DP.toFloat()).toFloat())) {
                                    override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
                                        this@LocationSharingMarker.bitmap = resource
                                        this@LocationSharingMarker.invalidate(true)
                                    }
                                })
                    }
                } else {
                    if (context != null) {
                        activity!!.runOnUiThread {
                            val initialsBitmap = friend?.getInitialsBitmap(Dev.get().dpToPix(SIZE_AVATAR_DP.toFloat()), Dev.get().dpToPix(SIZE_AVATAR_DP.toFloat()),
                                    Dev.get().dpToPix(24f), ContextCompat.getColor(context, R.color.mr_callout_blue))

                            // convert to byte array
                            val stream = ByteArrayOutputStream()
                            initialsBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)

                            // draw
                            Glide.with(activity!!.applicationContext)
                                    .fromBytes()
                                    .load(stream.toByteArray())
                                    .bitmapTransform(CropCircleTransformation(Glide.get(activity!!.applicationContext).bitmapPool, Dev.get().dpToPix(2f)))
                                    .into<SimpleTarget<GlideDrawable>>(object : SimpleTarget<GlideDrawable>(Dev.get().dpToPix(SIZE_AVATAR_DP.toFloat()), Dev.get().dpToPix(SIZE_AVATAR_DP.toFloat())) {
                                        override fun onResourceReady(resource: GlideDrawable?, glideAnimation: GlideAnimation<in GlideDrawable>?) {
                                            this@LocationSharingMarker.bitmap = (resource as GlideBitmapDrawable).bitmap
                                            this@LocationSharingMarker.invalidate(true)
                                        }
                                    })
                        }
                    }
                }
            }
            // still loading
            return null

        }
    }

    private fun renderFriends(friends: List<Friend>?) {

        if (mapView?.mapKey == null || friends == null) {
            //if (mapView == null) mapView = getMapView()
            friendMarkers?.clear()
            return
        }

        // If we have an active route update our destinationFriend, this will allow for rerouting to
        // trigger if the friend has moved substantially. If we want to override the automatic
        // rerouting triggers we could call `onDirectionsReroute()` here.
        if (mapView?.route != null) {
            for (friend in friends) {
                if (destinationFriend != null && friend == destinationFriend)
                    destinationFriend?.location = friend.location
            }
        }

        // remove friends who are gone or in another map
        val friendsToRemove =
                friendsList.filter {
                    !friends.contains(it) || !it.isInSameMapAndSharing(mapView?.mapKey)
                }
        val markersToRemove: ArrayList<Marker>? = ArrayList()
        for (friend in friendsToRemove) {
            friendMarkers?.get(friend.key)?.let {
                markersToRemove?.add(it)
                friendMarkers?.remove(friend.key)
            }
        }

        if (markersToRemove?.isEmpty() == false) {
            mapView?.commitTransaction(Transaction.Builder().setType(Transaction.Type.REMOVE).setAnimationDuration(0).addMarkers(markersToRemove).build())
        }

        (friends as ArrayList).removeAll(friendsToRemove)

        // add the new friends
        val friendsToAdd = friends.filter { friendMarkers != null && friendMarkers?.get(it.key) == null && it.isInSameMapAndSharing(mapView?.mapKey) }
        for (friend in friendsToAdd) {
            if (friend.location?.mapKey == mapView?.mapKey) {
                friendMarkers?.set(friend.key, LocationSharingMarker(activity!!, friend, false))
            }
        }

        // update the existing friends with the location
        val friendsToUpdate = friends.filter { friendsList.contains(it) && it.isInSameMapAndSharing(mapView?.mapKey) }
        for (friend in friendsToUpdate) {
            // update current selected friend
            if (currentSelectedFriendMarker?.friend?.key == friend.key) {
                currentSelectedFriendMarker?.setPosition(friend.location?.x!!.toFloat(), friend.location?.y!!.toFloat())
                mapView?.commitTransaction(Transaction.Builder().setAnimationDuration(250).addMarker(currentSelectedFriendMarker!!).build())
            }
            (friendMarkers?.get(friend.getKey()) as LocationSharingMarker).friend?.setLocation(friend.getLocation())
            // update last heard field (in Marker details)
            val ageInMillis = System.currentTimeMillis() - friend.location.timestamp.time
            if (context != null) {
                (friendMarkers?.get(friend.getKey()) as LocationSharingMarker).details = getString(R.string.last_heard) + ": " + getFormattedAgeString(context!!.resources, ageInMillis)
            }
            friendMarkers?.get(friend.key)?.setPosition(friend.location?.x!!.toFloat(), friend.location?.y!!.toFloat())
        }
        if (friendMarkers?.isEmpty() == false) {
            mapView.commitTransaction(Transaction.Builder().setAnimationDuration(250).addMarkers(friendMarkers!!.values).build())
        }

        friendsList = friends
    }

    //
    // MapViewListener methods
    //
    override fun onMapLoadStart() {
        super.onMapLoadStart()

        // time to remove our markers
        friendMarkers?.clear()
    }

    override fun onMapLoadFinish() {
        super.onMapLoadFinish()
    }

    override fun onPlacemarksLoadFinish() {
        super.onPlacemarksLoadFinish()
    }

    override fun onMapRenderFinish() {
        super.onMapRenderFinish()
    }

    override fun onMapLoadFail(tr: Throwable) {
        super.onMapLoadFail(tr)
    }

    override fun onMapTransformChange(transform: Matrix) {
        super.onMapTransformChange(transform)
    }

    override fun onDirectionsReroute() {
        super.onDirectionsReroute()
    }

    override fun onDirectionsClick(marker: Marker): Boolean {
        if (marker is PlacemarkMarker) {
            return super.onDirectionsClick(marker)
        } else if (marker is LocationSharingMarker) {
            val friend = marker.friend
            if (friend != null && friend.location != null) {
                startDirections(DirectionsDestination.forFriend(friend))
                destinationFriend = friend
            }
            return false
        }
        return super.onDirectionsClick(marker)
    }

    override fun onDirectionsStart(): Boolean {
        return super.onDirectionsStart()
    }

    override fun onRouteStepIndexChange(index: Int): Boolean {
        return super.onRouteStepIndexChange(index)
    }

    override fun onDirectionsClosed(): Boolean {
        return super.onDirectionsClosed()
    }

    override fun onDirectionsError(tr: Throwable): Boolean {
        return super.onDirectionsError(tr)
    }

    override fun onLocationUpdated(meridianLocation: MeridianLocation) {
        super.onLocationUpdated(meridianLocation)
    }

    override fun onUseAccessiblePathsChange() {
        super.onUseAccessiblePathsChange()
    }
    //
    // MapView.MarkerEventListener methods
    //

    override fun onMarkerSelect(marker: Marker): Boolean {
        currentSelectedFriendMarker = null
        if (marker is LocationSharingMarker && friendMarkers?.containsValue(marker) == true) {
            currentSelectedFriendMarker = marker
        }
        return false
    }

    override fun onMarkerDeselect(marker: Marker): Boolean {
        if (marker is LocationSharingMarker) {
            currentSelectedFriendMarker = null
        }
        return false
    }

    override fun markerForSelectedMarker(marker: Marker): Marker? {
        if (context != null) {
            if (marker is LocationSharingMarker) {
                val newMarker = LocationSharingMarker(context!!, marker.friend, true)
                newMarker.setPosition(marker.getPosition()[0], marker.getPosition()[1])
                currentSelectedFriendMarker = newMarker
                return newMarker
            }
        }
        return null
    }

    companion object {

        private const val APP_KEY = "meridian.AppKey"
        private const val MAP_KEY = "meridian.MapKey"
        private const val ROUTE_KEY = "meridian.RouteKey"
        private const val ROUTE_STEP_KEY = "meridian.RouteStepKey"
        private const val ROUTE_REROUTE_KEY = "meridian.RouteReroute"
        private const val FROM_KEY = "meridian.FromKey"
        private const val PENDING_DESTINATION_KEY = "meridian.PendingDestinationKey"
        private const val CONTROLS_OPTIONS_KEY = "meridian.mapOptions"
        private val SOURCE_REQUEST_CODE = "meridian.source_request".hashCode() and 0xFF

        fun newInstance(mapKey: EditorKey): LocationSharingMapFragment {
            val f = LocationSharingMapFragment()
            val args = MapFragment.Builder().setMapKey(mapKey).build().arguments
            f.arguments = args
            return f
        }
    }

    private fun getFormattedAgeString(res: Resources, ageMilli: Long): String {
        var ageMillis = ageMilli
        if (ageMillis < 0) {
            return res.getString(R.string.last_heard_now)
        }

        val secondsInMilli: Long = 1000
        val minutesInMilli = secondsInMilli * 60
        val hoursInMilli = minutesInMilli * 60
        val daysInMilli = hoursInMilli * 24

        val elapsedDays = ageMillis / daysInMilli
        ageMillis = ageMillis % daysInMilli

        val elapsedHours = ageMillis / hoursInMilli
        ageMillis = ageMillis % hoursInMilli

        val elapsedMinutes = ageMillis / minutesInMilli
        ageMillis = ageMillis % minutesInMilli

        val elapsedSeconds = ageMillis / secondsInMilli

        return if (elapsedDays > 0)
            res.getString(R.string.age_format_days, elapsedDays, elapsedHours, elapsedMinutes)
        else if (elapsedHours > 0)
            res.getString(R.string.age_format_hours, elapsedHours, elapsedMinutes)
        else if (elapsedMinutes > 0)
            res.getString(R.string.age_format_minutes, elapsedMinutes)
        else
            res.getString(R.string.age_format_seconds, elapsedSeconds)
    }
}
