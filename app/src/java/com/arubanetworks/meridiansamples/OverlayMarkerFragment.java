package com.arubanetworks.meridiansamples;

import android.content.Context;

import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.editor.Placemark;
import com.arubanetworks.meridian.maps.MapFragment;
import com.arubanetworks.meridian.maps.MapView;
import com.arubanetworks.meridian.maps.Marker;
import com.arubanetworks.meridian.maps.OverlayMarker;
import com.arubanetworks.meridian.maps.OverlayMarkerOptions;
import com.arubanetworks.meridian.maps.Transaction;
import com.arubanetworks.meridian.maprender.TextureProvider;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


/**
 * Fragment to demonstrate how to use the different flavors of OverlayMarker
 */


public class OverlayMarkerFragment extends MapFragment implements MapView.MarkerEventListener{

    // Cache the 'unselected' overlayMarker's colors
    //   so they can be toggled when the OverlayMarker is selected
    //
    Map<Long, Integer> markerEdgeColors;
    Map<Long, Integer> markerFillColors;

    // Cache a group of OverlayMarkers whose colors can toggled as if they are one selectable item
    //   implemented as circular linked list within a map
    Map<Long, Marker> markerGroups;

    // ------------------------------------------------------------------------

    private void toggleColor(Marker marker){
        OverlayMarkerOptions options = ((OverlayMarker)marker).getOverlayMarkerOptions();

        {
            // when selected, toggle Fill Color between original color, and 0xFFFF3333
            int currentFillColor = options.getOverlayFillColor();
            int updatedFillColor = 0xFFFF3333;
            if (markerFillColors == null) {
                markerFillColors = new HashMap<Long, Integer>();
                markerFillColors.put(marker.getId(), currentFillColor);
            } else if (!markerFillColors.containsKey(marker.getId())) {
                markerFillColors.put(marker.getId(), currentFillColor);
            } else if (currentFillColor == updatedFillColor){
                updatedFillColor = markerFillColors.get(marker.getId());
            }
            options.setOverlayFillColor(updatedFillColor);
        }

        {
            // when selected, toggle Edge Color between original color, and 0x22AA2277
            int currentEdgeColor = options.getOverlayColor();
            int updatedEdgeColor = 0x22AA22AA;
            if (markerEdgeColors == null) {
                markerEdgeColors = new HashMap<Long, Integer>();
                markerEdgeColors.put(marker.getId(), currentEdgeColor);
            } else if (!markerEdgeColors.containsKey(marker.getId())) {
                markerEdgeColors.put(marker.getId(), currentEdgeColor);
            } else if(currentEdgeColor == updatedEdgeColor) {
                updatedEdgeColor = markerEdgeColors.get(marker.getId());
            }
            options.setOverlayColor(updatedEdgeColor);
        }

    }

    // MapView.MarkerEventListener methods
    //
    @Override
    public boolean onMarkerSelect(Marker marker) {

        // When an OverlayMarker is selected, toggle the colors
        if (marker instanceof OverlayMarker) {

            // trigger a redraw
            toggleColor(marker);
            marker.invalidate(true);

            // if this marker is part of a group... toggle the rest of the group as well
            if(markerGroups != null){
                Marker nextMarkerInGroup = markerGroups.get(marker.getId());
                while((nextMarkerInGroup != null) &&  (nextMarkerInGroup != marker)){
                    toggleColor(nextMarkerInGroup);
                    nextMarkerInGroup.invalidate(true);
                    nextMarkerInGroup = markerGroups.get(nextMarkerInGroup.getId());
                }
            }

            // let the calling function know it's been handled by returning true.
            return true;
        }
        return false;
    }

    public static OverlayMarkerFragment newInstance(EditorKey mapKey){
        OverlayMarkerFragment f = new OverlayMarkerFragment();
        MapFragment mapFragment = new MapFragment.Builder()
                .setMapKey(mapKey)
                .build();
        f.setArguments(mapFragment.getArguments());
        return f;
    }

    /**
     * Substitute a OverlayMarker for the default FlatPlacemarkMarker for select placemarks (labeled "EventSpace")
     *
     * @param placemark The placemark that is requesting a {@link Marker} for displaying on the map
     * @return A new instance of a {@link Marker} subclass, or null to request that MapView create a default {@link Marker}.
     */
    @Override
    public Marker markerForPlacemark(Placemark placemark) {
        Context c = getActivity();
        if(c == null)
            return null;

        // Substitute a OverlayMarker for the default FlatPlacemarkMarker
        if(placemark.getName().contains("EventSpace")){
            OverlayMarker gm = new OverlayMarker.Builder(c,
                    OverlayMarkerOptions.fromPlacemark(placemark)).build();

            // Override the default colors
            gm.getOverlayMarkerOptions().setOverlayColor(0x22AA2255);
            gm.getOverlayMarkerOptions().setOverlayFillColor(0x22AA2233);

            return gm;

        }
        return null;
    }

    /**
     * Add an additional OverlayMarker for a FlatPlacemarkMarker for select placemarks (labeled "Storage")
     *
     */

    @Override
    public void onPlacemarksLoadFinish() {
        super.onPlacemarksLoadFinish();

        for (Placemark placemark : getMapView().getPlacemarks()) {
            if(placemark.getName().contains("Storage")){
                Context c = getActivity();
                if(c == null)
                    return;

                // Create a secondary OverlayMarker in addition to a FlatPlacemarkMarker for this placemark

                OverlayMarker om = new OverlayMarker.Builder(c, OverlayMarkerOptions.fromPlacemark(placemark)).build();
                om.getOverlayMarkerOptions().setOverlayColor(0x2222AA55);
                om.getOverlayMarkerOptions().setOverlayFillColor(0x2222AA33);

                // Add the marker to the map
                ArrayList<Marker> markerList = new ArrayList<>();
                markerList.add(om);
                getMapView().commitTransaction(new Transaction.Builder().setAnimationDuration(500).addMarkers(markerList).build());

            }
        }
    }

    /**
     * Called after the map and placemarks have been loaded and rendered.
     * programmatically add several OverlayMarkers
     */

    @Override
    public void onMapRenderFinish() {
        super.onMapRenderFinish();

        ArrayList<Marker> markerList = new ArrayList<>();
        Context c = getActivity();
        if (c != null) {

            OverlayMarker openPathMarker, arrowHeadMarker, circleMarker;
            int floatsPerPoint = 2;  // points are 2D

            // Create an interesting path to demonstrate the OverlayMarker with OPEN_PATH

            int numPathPts = 4;
            float openPts[] = new float[numPathPts * floatsPerPoint + 1];
            openPts[0] = (float) numPathPts;
            openPts[1]  =   0.0f; openPts[2]  =   0.0f;
            openPts[3]  =   0.0f; openPts[4]  =  40.0f;
            openPts[5]  = 140.0f; openPts[6]  = 280.0f;
            openPts[7]  =  60.0f; openPts[8]  = 360.0f;

            OverlayMarkerOptions optionOpenPath = new OverlayMarkerOptions(
                    TextureProvider.OverlayType.OPEN_PATH,
                    240.0f, 300.0f,
                    OverlayMarkerOptions.OverlayMarkerCoordinateType.RELATIVE,
                    openPts);

            optionOpenPath.setOverlayWidth(7);
            optionOpenPath.setOverlayColor(0xAA2222BB);
            optionOpenPath.setOverlayFillColor(0x33337733);
            openPathMarker = new OverlayMarker.Builder(c, optionOpenPath).build();
            openPathMarker.setName("Path Marker");
            markerList.add(openPathMarker);


            // Create an interesting arrowhead on the path to demonstrate the OverlayMarker with FILL

            int numArrowPts = 4;
            float points[] = new float[numArrowPts * floatsPerPoint + 1];
            points[0] = (float) numArrowPts;
            points[1]  =   0.0f; points[2]  =   0.0f;
            points[3]  = -20.0f; points[4]  =  20.0f;
            points[5]  =   0.0f; points[6]  = -50.0f;
            points[7]  =  20.0f; points[8]  =  20.0f;

            OverlayMarkerOptions optionsArrow = new OverlayMarkerOptions(
                    TextureProvider.OverlayType.OUTLINE_WITH_FILL,
                    240.0f, 300.0f,
                    OverlayMarkerOptions.OverlayMarkerCoordinateType.RELATIVE,
                    points);

            optionsArrow.setOverlayWidth(3);
            optionsArrow.setOverlayColor(0xAA2222BB);
            optionsArrow.setOverlayFillColor(0xAA222233);

            arrowHeadMarker = new OverlayMarker.Builder(c, optionsArrow).build();
            arrowHeadMarker.setName("Arrow Marker");
            markerList.add(arrowHeadMarker);


            // Create a circle to demonstrate the OverlayMarker fromCircle

            float radius = 10.0f;
            circleMarker = new OverlayMarker.Builder(c, OverlayMarkerOptions.fromCircle(240.0f, 230.0f-radius, radius)).build();
            circleMarker.getOverlayMarkerOptions().setOverlayWidth(3);
            circleMarker.getOverlayMarkerOptions().setOverlayColor(0x555555FF);
            circleMarker.getOverlayMarkerOptions().setOverlayFillColor(0xAA222233);
            circleMarker.setName("Circle Marker");
            markerList.add(circleMarker);

            // This demonstrates how several OverlayMarkers can be grouped to act as one
            if(markerGroups == null) {
                markerGroups = new HashMap<Long, Marker>();
            }
            markerGroups.put(openPathMarker.getId(), arrowHeadMarker);
            markerGroups.put(arrowHeadMarker.getId(), circleMarker);
            markerGroups.put(circleMarker.getId(), openPathMarker);

        }

        // Add the markers to the map
        getMapView().commitTransaction(new Transaction.Builder().setAnimationDuration(500).addMarkers(markerList).build());
    }
}
