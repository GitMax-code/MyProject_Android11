package com.example.myproject_android11;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Configuration d'osmdroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        // Initialisation de la MapView
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(50.8503, 4.3517)); // Centre sur Bruxelles

        // Initialisation de Volley
        requestQueue = Volley.newRequestQueue(this);

        // Chargement des données et ajout des marqueurs
        //loadAndAddMarkers();
        //drawRedMarker();
        //drawPolyline();
        drawPolygone();
    }

    private void drawRedMarker() {
        GeoPoint centerPoint = new GeoPoint(50.8503, 4.3517);
        Marker marker = new Marker(mapView);
        marker.setPosition(centerPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_red_marker));
        mapView.getOverlays().add(marker);
    }

    private void drawPolyline() {
        ArrayList<GeoPoint> geoPoints = new ArrayList<>();
        GeoPoint geoPoint1 = new GeoPoint(50.8503, 4.3517);
        GeoPoint geoPoint2 = new GeoPoint(50.8504, 4.3518);
        GeoPoint geoPoint3 = new GeoPoint(50.8502, 4.3516);
        geoPoints.add(geoPoint1);
        geoPoints.add(geoPoint2);
        geoPoints.add(geoPoint3);
        Polyline polyline = new Polyline();
        polyline.setPoints(geoPoints);
        polyline.setOnClickListener(new Polyline.OnClickListener() {

            @Override
            public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                Toast.makeText(MapActivity.this, "Polyline clicked", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        mapView.getOverlays().add(polyline);
    }



    private void drawPolygone(){
        ArrayList<GeoPoint> geoPoints = new ArrayList<>();
        GeoPoint geoPoint1 = new GeoPoint(50.8503, 4.3517);
        GeoPoint geoPoint2 = new GeoPoint(50.8503, 4.3527);
        GeoPoint geoPoint3 = new GeoPoint(50.8493, 4.3517);
        geoPoints.add(geoPoint1);
        geoPoints.add(geoPoint2);
        geoPoints.add(geoPoint3);
        geoPoints.add(geoPoints.get(0));
        Polygon polygone = new Polygon();

        //FillPaint = contenu du polygone
        polygone.getFillPaint().setColor(Color.parseColor("#0000FF"));
        //OutlinePaint = bord du polygone
        polygone.getOutlinePaint().setColor(Color.parseColor("#FF0000"));
        polygone.getOutlinePaint().setStrokeWidth(0f);
        polygone.setPoints(geoPoints);
        polygone.setTitle("A polygon");
        mapView.getOverlays().add(polygone);

    }

    private void loadAndAddMarkers() {
        String url = "https://opendata.brussels.be/api/explore/v2.1/catalog/datasets/limites-administratives-des-communes-en-region-de-bruxelles-capitale/records?limit=20";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray records = response.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            JSONObject fields = record.getJSONObject("fields");
                            JSONArray geomCoords = fields.getJSONObject("geom").getJSONArray("coordinates").getJSONArray(0);

                            // Calculer le centre du polygone
                            double sumLat = 0;
                            double sumLon = 0;
                            int numPoints = geomCoords.length();
                            List<GeoPoint> polygonPoints = new ArrayList<>();

                            for (int j = 0; j < numPoints; j++) {
                                JSONArray coord = geomCoords.getJSONArray(j);
                                double lon = coord.getDouble(0);
                                double lat = coord.getDouble(1);
                                sumLon += lon;
                                sumLat += lat;
                                polygonPoints.add(new GeoPoint(lat, lon)); // Ajouter les points du polygone
                            }

                            double centerLat = sumLat / numPoints;
                            double centerLon = sumLon / numPoints;
                            GeoPoint centerPoint = new GeoPoint(centerLat, centerLon);

                            // Ajouter un marqueur au centre du polygone
                            Marker marker = new Marker(mapView);
                            marker.setPosition(centerPoint);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_red_marker));
                            mapView.getOverlays().add(marker);

                            // Ajouter le polygone des limites communales
                            Polygon polygon = new Polygon(mapView);
                            polygon.setPoints(polygonPoints);
                            polygon.setStrokeColor(0xFFFF0000); // Rouge
                            polygon.setStrokeWidth(5);
                            polygon.setFillColor(0x30FF0000); // Rouge semi-transparent
                            mapView.getOverlays().add(polygon);
                        }
                        mapView.invalidate(); // Rafraîchir la carte
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur JSON", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Erreur de chargement des données", Toast.LENGTH_SHORT).show();
                    Log.e("API_ERROR", "Erreur lors de la requête API", error);
                }
        );

        requestQueue.add(jsonObjectRequest);
    }
}
