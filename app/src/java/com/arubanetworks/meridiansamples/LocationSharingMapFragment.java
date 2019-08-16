package com.arubanetworks.meridiansamples;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.webkit.URLUtil;

import com.arubanetworks.meridian.Meridian;
import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.internal.util.Dev;
import com.arubanetworks.meridian.internal.util.Strings;
import com.arubanetworks.meridian.location.LocationRequest;
import com.arubanetworks.meridian.location.MeridianLocation;
import com.arubanetworks.meridian.locationsharing.Friend;
import com.arubanetworks.meridian.locationsharing.LocationSharing;
import com.arubanetworks.meridian.locationsharing.LocationSharingException;
import com.arubanetworks.meridian.maps.MapFragment;
import com.arubanetworks.meridian.maps.MapOptions;
import com.arubanetworks.meridian.maps.MapView;
import com.arubanetworks.meridian.maps.Marker;
import com.arubanetworks.meridian.maps.PlacemarkMarker;
import com.arubanetworks.meridian.maps.Transaction;
import com.arubanetworks.meridian.maps.directions.Directions;
import com.arubanetworks.meridian.maps.directions.DirectionsDestination;
import com.arubanetworks.meridian.maps.directions.DirectionsSource;
import com.arubanetworks.meridian.maps.directions.Route;
import com.arubanetworks.meridiansamples.utils.CropCircleTransformation;
import com.arubanetworks.meridiansamples.utils.Lists;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings({"UnusedDeclaration", "ConstantConditions"})
public class LocationSharingMapFragment extends MapFragment implements MapView.DirectionsEventListener, MapView.MapEventListener,
        MapView.MarkerEventListener {

    private static final String APP_KEY = "meridian.AppKey";
    private static final String MAP_KEY = "meridian.MapKey";
    private static final String ROUTE_KEY = "meridian.RouteKey";
    private static final String ROUTE_STEP_KEY = "meridian.RouteStepKey";
    private static final String ROUTE_REROUTE_KEY = "meridian.RouteReroute";
    private static final String FROM_KEY = "meridian.FromKey";
    private static final String PENDING_DESTINATION_KEY = "meridian.PendingDestinationKey";
    private static final String CONTROLS_OPTIONS_KEY = "meridian.mapOptions";
    private static final int SOURCE_REQUEST_CODE = "meridian.source_request".hashCode() & 0xFF;

    private MapView mapView;
    private MapOptions mapOptions = null;

    private EditorKey appKey;
    private EditorKey mapKey;

    private Route savedRoute;
    private int savedRouteStepIndex = 0;
    private boolean isRerouting = false;

    private Directions directions;
    private DirectionsSource fromItem;
    private DirectionsDestination pendingDestination;
    private LocationRequest locationRequest;

    /**
     * Friends
     **/
    private List<Friend> friendsList = new ArrayList<>();
    private List<LocationSharingMarker> friendsMarker = new ArrayList<>();

    private Map<String, Marker> friendMarkers = new HashMap<>();
    private Timer friendsTimer;
    private TimerTask friendsTimerTask;
    private Friend destinationFriend;
    private LocationSharingMarker currentSelectedFriendMarker;

    public static LocationSharingMapFragment newInstance(EditorKey mapKey) {
        LocationSharingMapFragment f = new LocationSharingMapFragment();
        Bundle args = new MapFragment.Builder().setMapKey(mapKey).build().getArguments();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // we need to initialize Location Sharing first
        if (!Strings.isNullOrEmpty(Meridian.getShared().getEditorToken())) {
            LocationSharing.initWithAppKey(Application.APP_KEY);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView = getMapView();
        if (Strings.isNullOrEmpty(Meridian.getShared().getEditorToken())) {
            new AlertDialog.Builder(getActivity())
                    .setMessage("You need to provide a valid editor token")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        if (LocationSharing.shared().isUploadingServiceRunning()) {
            friendsTimer = new Timer();
            friendsTimerTask = new TimerTask() {
                @Override
                public void run() {
                    LocationSharing.shared().getFriends(new LocationSharing.Callback<List<Friend>>() {
                        @Override
                        public void onSuccess(List<Friend> result) {
                            renderFriends(result);
                        }

                        @Override
                        public void onError(LocationSharingException lse) {
                            // TODO do something
                        }
                    });
                }
            };
            friendsTimer.schedule(friendsTimerTask, new Date(), 5000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (friendsTimer != null) {
            friendsTimer.cancel();
            friendsTimer = null;
        }

        if (friendsTimerTask != null) {
            friendsTimerTask.cancel();
            friendsTimerTask = null;
        }
    }

    private class LocationSharingMarker extends Marker {

        private static final int SIZE_AVATAR_DP = 40;

        private Context context;
        private Friend friend;
        private Bitmap bitmap;
        private boolean selected;  // if the friend marker is selected, this could be used to change the bitmap size for example

        LocationSharingMarker(Context context, Friend friend, boolean selected) {
            super(friend.getLocation().getX(), friend.getLocation().getY());
            this.friend = friend;
            this.context = context != null ? context.getApplicationContext() : null;
            this.selected = selected;
            setWeight(1.1f);
            setName(friend.getFullName());
            long ageInMillis = System.currentTimeMillis() - friend.getLocation().getTimestamp().getTime();
            if (this.context != null) {
                setDetails(getString(R.string.last_heard) + ": " + getFormattedAgeString(this.context.getResources(), ageInMillis));
            }
        }

        Friend getFriend() {
            return friend;
        }

        @Override
        public boolean getCollision() {
            return false;
        }

        @Override
        public boolean canBeSelected() {
            return !selected;
        }

        public String getFriendId() {
            return friend.getKey();
        }

        @Override
        public Bitmap getBitmap() {
            if (bitmap != null) {
                return bitmap;
            }

            if (getActivity() != null) {
                if (URLUtil.isValidUrl(friend.getPhotoUrl())) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(getActivity().getApplicationContext())
                                    .load(friend.getPhotoUrl())
                                    .asBitmap()
                                    .transform(new CropCircleTransformation(Glide.get(getActivity().getApplicationContext()).getBitmapPool(), Dev.get().dpToPix(2)))
                                    .into(new SimpleTarget<Bitmap>(Math.round(Dev.get().dpToPix(SIZE_AVATAR_DP)), Math.round(Dev.get().dpToPix(SIZE_AVATAR_DP))) {
                                        @Override
                                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                                            LocationSharingMarker.this.bitmap = bitmap;
                                            LocationSharingMarker.this.invalidate(true);
                                        }
                                    });
                        }
                    });
                } else {
                    if (context != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap initialsBitmap = friend.getInitialsBitmap(Dev.get().dpToPix(SIZE_AVATAR_DP), Dev.get().dpToPix(SIZE_AVATAR_DP),
                                        Dev.get().dpToPix(24), ContextCompat.getColor(context, R.color.mr_callout_blue));

                                // convert to byte array
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                initialsBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

                                // draw
                                Glide.with(getActivity().getApplicationContext())
                                        .fromBytes()
                                        .load(stream.toByteArray())
                                        .bitmapTransform(new CropCircleTransformation(Glide.get(getActivity().getApplicationContext()).getBitmapPool(), Dev.get().dpToPix(2)))
                                        .into(new SimpleTarget<GlideDrawable>(Dev.get().dpToPix(SIZE_AVATAR_DP), Dev.get().dpToPix(SIZE_AVATAR_DP)) {
                                            @Override
                                            public void onResourceReady(GlideDrawable drawable, GlideAnimation anim) {
                                                LocationSharingMarker.this.bitmap = ((GlideBitmapDrawable) drawable).getBitmap();
                                                LocationSharingMarker.this.invalidate(true);
                                            }
                                        });
                            }
                        });
                    }
                }
            }
            // still loading
            return null;

        }
    }

    private void renderFriends(final List<Friend> friends) {

        if (mapView == null || mapView.getMapKey() == null || friends == null) {
            if (mapView == null) mapView = getMapView();
            if (friendMarkers != null) friendMarkers.clear();
            return;
        }

        // If we have an active route update our destinationFriend, this will allow for rerouting to
        // trigger if the friend has moved substantially. If we want to override the automatic
        // rerouting triggers we could call `onDirectionsReroute()` here.
        if(mapView!=null && mapView.getRoute()!=null) {
            for (Friend friend : friends) {
                if (destinationFriend != null && friend.equals(destinationFriend))
                    destinationFriend.setLocation(friend.getLocation());
            }
        }

        // remove friends who are gone or in another map
        List<Friend> friendsToRemove = Lists.filter(friendsList, new Lists.IPredicate<Friend>() {
            @Override
            public boolean apply(Friend friend) {
                return !friends.contains(friend) || !friend.isInSameMapAndSharing(mapView.getMapKey());
            }
        });
        List<Marker> markersToRemove = new ArrayList<>();
        for (Friend friend : friendsToRemove) {
            if (friendMarkers.get(friend.getKey()) != null) {
                markersToRemove.add(friendMarkers.get(friend.getKey()));
                friendMarkers.remove(friend.getKey());
            }
        }
        if (!markersToRemove.isEmpty()) {
            mapView.commitTransaction(new Transaction.Builder().setType(Transaction.Type.REMOVE).setAnimationDuration(0).addMarkers(markersToRemove).build());
        }
        friends.removeAll(friendsToRemove);

        // add the new friends
        List<Friend> friendsToAdd = Lists.filter(friends, new Lists.IPredicate<Friend>() {
            @Override
            public boolean apply(Friend friend) {
                return friendMarkers.get(friend.getKey()) == null && friend.isInSameMapAndSharing(mapView.getMapKey());
            }
        });
        for (Friend friend : friendsToAdd) {
            if (friend.getLocation().getMapKey().equals(mapView.getMapKey())) {
                friendMarkers.put(friend.getKey(), new LocationSharingMarker(getActivity(), friend, false));
            }
        }

        // update the existing friends with the location
        List<Friend> friendsToUpdate = Lists.filter(friends, new Lists.IPredicate<Friend>() {
            @Override
            public boolean apply(Friend friend) {
                return friendsList.contains(friend) && friend.isInSameMapAndSharing(mapView.getMapKey());
            }
        });
        for (Friend friend : friendsToUpdate) {
            // update current selected friend
            if (currentSelectedFriendMarker != null && currentSelectedFriendMarker.friend.getKey().equals(friend.getKey())) {
                currentSelectedFriendMarker.setPosition(friend.getLocation().getX(), friend.getLocation().getY());
                mapView.commitTransaction(new Transaction.Builder().setAnimationDuration(250).addMarker(currentSelectedFriendMarker).build());
            }
            ((LocationSharingMarker) friendMarkers.get(friend.getKey())).friend.setLocation(friend.getLocation());
            // update last heard field (in marker details)
            long ageInMillis = System.currentTimeMillis() - friend.getLocation().getTimestamp().getTime();
            if (getContext() != null) {
                friendMarkers.get(friend.getKey()).setDetails(getString(R.string.last_heard) + ": " + getFormattedAgeString(getContext().getResources(), ageInMillis));
            }
            friendMarkers.get(friend.getKey()).setPosition(friend.getLocation().getX(), friend.getLocation().getY());
        }
        if (!friendMarkers.isEmpty()) {
            mapView.commitTransaction(new Transaction.Builder().setAnimationDuration(250).addMarkers(friendMarkers.values()).build());
        }

        friendsList = friends;
    }

    //
    // MapViewListener methods
    //
    @Override
    public void onMapLoadStart() {
        super.onMapLoadStart();

        // time to remove our markers
        if (friendMarkers != null) friendMarkers.clear();
    }

    @Override
    public void onMapLoadFinish() {
        super.onMapLoadFinish();
    }

    @Override
    public void onPlacemarksLoadFinish() {
        super.onPlacemarksLoadFinish();
    }

    @Override
    public void onMapRenderFinish() {
        super.onMapRenderFinish();
    }

    @Override
    public void onMapLoadFail(Throwable tr) {
        super.onMapLoadFail(tr);
    }

    @Override
    public void onMapTransformChange(Matrix transform) {
        super.onMapTransformChange(transform);
    }

    @Override
    public void onDirectionsReroute() {
        super.onDirectionsReroute();
    }

    @Override
    public boolean onDirectionsClick(Marker marker) {
        if (marker instanceof PlacemarkMarker) {
            return super.onDirectionsClick(marker);

        } else if (marker instanceof LocationSharingMarker) {
            Friend friend = ((LocationSharingMarker) marker).getFriend();
            if (friend != null && friend.getLocation() != null) {
                startDirections(DirectionsDestination.forFriend(friend));
                destinationFriend =friend;
            }
            return false;
        }
        return super.onDirectionsClick(marker);
    }

    @Override
    public boolean onDirectionsStart() {
        return super.onDirectionsStart();
    }

    @Override
    public boolean onRouteStepIndexChange(int index) {
        return super.onRouteStepIndexChange(index);
    }

    @Override
    public boolean onDirectionsClosed() {
        return super.onDirectionsClosed();
    }

    @Override
    public boolean onDirectionsError(Throwable tr) {
        return super.onDirectionsError(tr);
    }

    @Override
    public void onLocationUpdated(MeridianLocation meridianLocation) {
        super.onLocationUpdated(meridianLocation);
    }

    @Override
    public void onUseAccessiblePathsChange() {
        super.onUseAccessiblePathsChange();
    }
    //
    // MapView.MarkerEventListener methods
    //

    @Override
    public boolean onMarkerSelect(Marker marker) {
        currentSelectedFriendMarker = null;
        if (marker instanceof LocationSharingMarker && friendMarkers.containsValue(marker)){
            currentSelectedFriendMarker = ((LocationSharingMarker) marker);
        }
        return false;
    }

    @Override
    public boolean onMarkerDeselect(Marker marker) {
        if (marker instanceof LocationSharingMarker){
            currentSelectedFriendMarker = null;
        }
        return false;
    }

    @Override
    public Marker markerForSelectedMarker(Marker marker) {
        if (getContext() != null) {
            if (marker instanceof LocationSharingMarker) {
                LocationSharingMarker newMarker = new LocationSharingMarker(getContext(), ((LocationSharingMarker) marker).friend, true);
                newMarker.setPosition(marker.getPosition()[0],marker.getPosition()[1]);
                currentSelectedFriendMarker = newMarker;
                return newMarker;
            }
        }
        return null;
    }

    private static String getFormattedAgeString(Resources res, long ageMillis) {
        if (ageMillis < 0) {
            return res.getString(R.string.last_heard_now);
        }

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = ageMillis / daysInMilli;
        ageMillis = ageMillis % daysInMilli;

        long elapsedHours = ageMillis / hoursInMilli;
        ageMillis = ageMillis % hoursInMilli;

        long elapsedMinutes = ageMillis / minutesInMilli;
        ageMillis = ageMillis % minutesInMilli;

        long elapsedSeconds = ageMillis / secondsInMilli;

        if (elapsedDays > 0)
            return res.getString(R.string.age_format_days, elapsedDays, elapsedHours, elapsedMinutes);
        else if (elapsedHours > 0)
            return res.getString(R.string.age_format_hours, elapsedHours, elapsedMinutes);
        else if (elapsedMinutes > 0)
            return res.getString(R.string.age_format_minutes, elapsedMinutes);
        else
            return res.getString(R.string.age_format_seconds, elapsedSeconds);
    }
}
