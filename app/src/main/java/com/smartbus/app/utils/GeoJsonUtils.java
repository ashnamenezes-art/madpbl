package com.smartbus.app.utils;

import com.smartbus.app.models.Stop;

import java.util.List;
import java.util.Locale;

public final class GeoJsonUtils {

    private GeoJsonUtils() {}

    /**
     * Build a GeoJSON FeatureCollection containing a single LineString that
     * traces the given list of stops in order.
     *
     * Example output:
     * <pre>
     * {
     *   "type": "FeatureCollection",
     *   "features": [{
     *     "type": "Feature",
     *     "geometry": {
     *       "type": "LineString",
     *       "coordinates": [[lng1,lat1],[lng2,lat2],...]
     *     },
     *     "properties": {}
     *   }]
     * }
     * </pre>
     *
     * @param stops Ordered list of Stop objects; must not be null or empty.
     * @return GeoJSON string.
     */
    public static String createRouteGeoJson(List<Stop> stops) {
        if (stops == null || stops.isEmpty()) {
            return emptyFeatureCollection();
        }

        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < stops.size(); i++) {
            Stop s = stops.get(i);
            coords.append(String.format(Locale.US, "[%s,%s]",
                    formatDouble(s.getLongitude()),
                    formatDouble(s.getLatitude())));
            if (i < stops.size() - 1) {
                coords.append(",");
            }
        }

        return "{"
                + "\"type\":\"FeatureCollection\","
                + "\"features\":[{"
                + "\"type\":\"Feature\","
                + "\"geometry\":{"
                + "\"type\":\"LineString\","
                + "\"coordinates\":[" + coords + "]"
                + "},"
                + "\"properties\":{}"
                + "}]"
                + "}";
    }

    /**
     * Build a GeoJSON Feature for a single map point.
     *
     * @param lat   Latitude of the point.
     * @param lng   Longitude of the point.
     * @param title A human-readable label stored in the "title" property.
     * @return GeoJSON string.
     */
    public static String createPointGeoJson(double lat, double lng, String title) {
        String safeTitle = (title == null) ? "" : title.replace("\"", "\\\"");
        return "{"
                + "\"type\":\"Feature\","
                + "\"geometry\":{"
                + "\"type\":\"Point\","
                + "\"coordinates\":[" + formatDouble(lng) + "," + formatDouble(lat) + "]"
                + "},"
                + "\"properties\":{"
                + "\"title\":\"" + safeTitle + "\""
                + "}"
                + "}";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String emptyFeatureCollection() {
        return "{\"type\":\"FeatureCollection\",\"features\":[]}";
    }

    /** Format a double with up to 6 decimal places, no trailing zeros. */
    private static String formatDouble(double value) {
        // Using Locale.US ensures '.' as decimal separator regardless of device locale.
        return String.format(Locale.US, "%.6f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", ".0");
    }
}
