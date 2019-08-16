package com.arubanetworks.meridiansamples;

import android.graphics.Matrix;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.arubanetworks.meridian.editor.Placemark;
import com.arubanetworks.meridian.location.MeridianLocation;
import com.arubanetworks.meridian.location.MeridianOrientation;
import com.arubanetworks.meridian.maps.ClusteredMarker;
import com.arubanetworks.meridian.maps.HighlightedMarkers;
import com.arubanetworks.meridian.maps.MapOptions;
import com.arubanetworks.meridian.maps.MapView;
import com.arubanetworks.meridian.maps.Marker;
import com.arubanetworks.meridian.maps.Transaction;

import java.util.ArrayList;

public class MapViewFragment extends Fragment implements MapView.DirectionsEventListener, MapView.MapEventListener, MapView.MarkerEventListener {

    /**
     * Demonstrates the use of the MapView.  It is recommended to use the SDK map fragment instead of the mapview
     */

    private MapView mapView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_mapview, container, false);

        mapView = layout.findViewById(R.id.demo_mapview);

        mapView.setAppKey(Application.APP_KEY);

        // If you want to handle MapView events
        mapView.setMapEventListener(this);

        // If you want to handle directions events
        mapView.setDirectionsEventListener(this);

        // If you want to handle marker events
        mapView.setMarkerEventListener(this);

        // Set map options if desired
        MapOptions mapOptions = mapView.getOptions();
        mapOptions.HIDE_MAP_LABEL = true;
        mapOptions.HIDE_DIRECTIONS_CONTROLS = true;
        mapView.setOptions(mapOptions);

        // If you want to load a map other than the default one
        // It is recommended to do this after setting the map options
        mapView.setMapKey(Application.MAP_KEY);

        // Demonstration of how to customize the mapView's locationMarker:
        //    change default color for Bluetooth to orange
        //    modify the name
        //    modify the details
        //    alternatively... hide the call-out entirely
        //
        // LocationMarker lm = mapView.getLocationMarker();
        // lm.setCustomColor(LocationMarker.State.BLUETOOTH, 0xffff7700);
        // lm.setName("Current Location Label");
        // lm.setDetails("Details");
        // // lm.setShowsCallout(false);

        return layout;
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up memory.
        mapView.onDestroy();
    }

    //
    // MapViewListener methods
    //
    @Override public void onMapLoadStart() { }
    @Override public void onMapLoadFinish() { }
    @Override public void onPlacemarksLoadFinish() {
        // example: highlight the first four placemarks
        /*
        ArrayList<Marker> markerList = new ArrayList<>();
        int index = 0;
        if (mapView != null && mapView.getAllMarkers() != null) {
            for (Marker marker : mapView.getAllMarkers()) {
                markerList.add(marker);
                index++;
                if (index >= 4) {
                    HighlightedMarkers highlightedMarkers = new HighlightedMarkers.Builder(markerList).build();
                    mapView.commitTransaction(new Transaction.Builder().setAnimationDuration(500).addMarker(highlightedMarkers).build());
                    break;
                }
            }
        }*/
    }
    @Override public void onMapLoadFail(Throwable tr) { }
    @Override public void onMapRenderFinish() { }
    @Override public void onMapTransformChange(Matrix transform) { }
    @Override public void onLocationUpdated(MeridianLocation location) { }
    @Override public void onOrientationUpdated(MeridianOrientation orientation) { }
    @Override public boolean onLocationButtonClick() { return false; }
    //
    // DirectionsEventListener methods
    //
    @Override public void onDirectionsReroute() { }
    @Override public boolean onDirectionsClick(Marker marker) {
        if (getActivity() != null) {
            new AlertDialog.Builder(getActivity())
                    .setMessage("Directions not implemented here.")
                    .setPositiveButton("OK", null)
                    .show();
        }
        return true;
    }
    @Override public boolean onDirectionsStart() { return false; }
    @Override public boolean onRouteStepIndexChange(int index) { return false; }
    @Override public boolean onDirectionsClosed() {return false; }
    @Override public boolean onDirectionsError(Throwable tr) { return false; }
    @Override public void onUseAccessiblePathsChange() { }
    //
    // MarkerEventListener methods
    //
    @Override
    public boolean onMarkerSelect(Marker marker) {
        // prevent clustered markers from being selected
        return (marker instanceof ClusteredMarker);
    }
    @Override
    public boolean onMarkerDeselect(Marker marker) {
        return false;
    }

    @Override
    public Marker markerForPlacemark(Placemark placemark) {
        return null;
    }

    @Override
    public Marker markerForSelectedMarker(Marker markerToSelect) {
        return null;
    }
    @Override
    public boolean onCalloutClick(Marker marker) {
        return false;
    }
}
