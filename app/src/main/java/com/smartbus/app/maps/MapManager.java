package com.smartbus.app.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.LiveLocation;
import com.smartbus.app.models.Stop;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.annotations.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapManager {

    // Map style URL - configurable, defaults to demotiles
    public static final String DEFAULT_STYLE = "https://demotiles.maplibre.org/style.json";

    private MapLibreMap mapLibreMap;
    private Context context;
    private Map<String, org.maplibre.android.annotations.Marker> busMarkers = new HashMap<>();

    public MapManager(Context context, MapLibreMap map) {
        this.context = context;
        this.mapLibreMap = map;
    }

    // Move camera to a location
    public void moveCameraTo(double lat, double lng, double zoom) {
        mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
            new CameraPosition.Builder()
                .target(new LatLng(lat, lng))
                .zoom(zoom)
                .build()
        ), 1000);
    }

    // Add or update a bus marker
    public void updateBusMarker(String busId, double lat, double lng, String busNumber) {
        if (busMarkers.containsKey(busId)) {
            busMarkers.get(busId).setPosition(new LatLng(lat, lng));
        } else {
            org.maplibre.android.annotations.MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title("Bus " + busNumber)
                .snippet("Tap for details");
            org.maplibre.android.annotations.Marker marker = mapLibreMap.addMarker(markerOptions);
            busMarkers.put(busId, marker);
        }
    }

    // Remove a bus marker
    public void removeBusMarker(String busId) {
        if (busMarkers.containsKey(busId)) {
            mapLibreMap.removeMarker(busMarkers.get(busId));
            busMarkers.remove(busId);
        }
    }

    // Draw route line from stops list
    public void drawRoute(List<Stop> stops, Style style) {
        if (stops == null || stops.size() < 2) return;

        StringBuilder coordsBuilder = new StringBuilder("[");
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            coordsBuilder.append("[").append(stop.getLongitude()).append(",").append(stop.getLatitude()).append("]");
            if (i < stops.size() - 1) coordsBuilder.append(",");
        }
        coordsBuilder.append("]");

        String geoJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":"
                + coordsBuilder + "},\"properties\":{}}";

        GeoJsonSource routeSource = new GeoJsonSource("route-source", geoJson);
        style.addSource(routeSource);

        LineLayer routeLayer = new LineLayer("route-layer", "route-source");
        routeLayer.setProperties(
            PropertyFactory.lineColor("#1565C0"),
            PropertyFactory.lineWidth(4f),
            PropertyFactory.lineOpacity(0.8f)
        );
        style.addLayer(routeLayer);
    }

    // Add stop markers from list
    public void addStopMarkers(List<Stop> stops) {
        if (stops == null) return;
        for (Stop stop : stops) {
            mapLibreMap.addMarker(new MarkerOptions()
                .position(new LatLng(stop.getLatitude(), stop.getLongitude()))
                .title(stop.getStopName())
                .snippet("Stop #" + stop.getStopOrder()));
        }
    }

    // Start tracking a single bus from Firebase
    public void startBusTracking(String busId, BusLocationCallback callback) {
        FirebaseHelper.getInstance().getLiveLocationsRef().child(busId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        LiveLocation loc = snapshot.getValue(LiveLocation.class);
                        if (loc != null && callback != null) {
                            callback.onLocationUpdate(loc);
                        }
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {}
            });
    }

    // Start tracking ALL buses from Firebase (for admin)
    public void startAllBusTracking(AllBusLocationCallback callback) {
        FirebaseHelper.getInstance().getLiveLocationsRef()
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        LiveLocation loc = child.getValue(LiveLocation.class);
                        if (loc != null && callback != null) {
                            callback.onLocationUpdate(child.getKey(), loc);
                        }
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {}
            });
    }

    // Callback interfaces
    public interface BusLocationCallback {
        void onLocationUpdate(LiveLocation location);
    }

    public interface AllBusLocationCallback {
        void onLocationUpdate(String busId, LiveLocation location);
    }
}
