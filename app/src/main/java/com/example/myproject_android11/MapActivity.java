package com.example.myproject_android11;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private RequestQueue requestQueue;

    private String testNameCommune;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String TAG = "DataListActivity";
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
        //requestQueue = Volley.newRequestQueue(this);

        // Chargement des données et ajout des marqueurs
        //loadAndAddMarkers();
        drawRedMarker();
        //drawPolyline();
        testNameCommune = "rien";
        //drawPolygone();
        fetchData();
        Toast.makeText(MapActivity.this, testNameCommune, Toast.LENGTH_SHORT).show();

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
        Toast.makeText(this, "name" + polygone.getTitle(), Toast.LENGTH_SHORT).show();
        //FillPaint = contenu du polygone
        polygone.getFillPaint().setColor(Color.parseColor("#0000FF"));
        //OutlinePaint = bord du polygone
        polygone.getOutlinePaint().setColor(Color.parseColor("#FF0000"));
        polygone.getOutlinePaint().setStrokeWidth(0f);
        polygone.setPoints(geoPoints);
        polygone.setTitle("A polygon");
        mapView.getOverlays().add(polygone);
    }

    private void drawPolygone(ArrayList<ArrayList<GeoPoint>> allPolygons) {
        for (ArrayList<GeoPoint> geoPoints : allPolygons) {
            if (geoPoints.size() < 3) continue; // Vérifier qu'il y a au moins 3 points pour un polygone

            Polygon polygone = new Polygon();
            polygone.setPoints(geoPoints);

            // Définir le style du polygone
            polygone.getFillPaint().setColor(Color.argb(100, 0, 0, 255)); // Bleu semi-transparent
            polygone.getOutlinePaint().setColor(Color.RED); // Bordure rouge
            polygone.getOutlinePaint().setStrokeWidth(3f);

            // Ajouter le polygone sur la carte
            mapView.getOverlays().add(polygone);
        }

        mapView.invalidate(); // Rafraîchir la carte
    }


    private void fetchData() {
        new Thread(() -> {
            String apiUrl = "https://opendata.brussels.be/api/explore/v2.1/catalog/datasets/limites-administratives-des-communes-en-region-de-bruxelles-capitale/records?limit=20";
            //String apiUrl = "https://opendata.brussels.be/api/explore/v2.1/catalog/datasets/limites-administratives-des-communes-en-region-de-bruxelles-capitale/records";
            String jsonResponse = getJsonFromUrl(apiUrl);

            if (jsonResponse != null) {
                // Récupérer la liste des polygones
                ArrayList<ArrayList<GeoPoint>> polygons = parseJson(jsonResponse);

                drawPolygone(polygons);
            } else {
                Log.e(TAG, "Failed to fetch data from URL");
            }
        }).start();
    }

    private String getJsonFromUrl(String urlString) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            jsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error ", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
        return jsonStr;
    }

    private ArrayList<ArrayList<GeoPoint>> parseJson(String jsonString) {
        ArrayList<ArrayList<GeoPoint>> allPolygons = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray resultsArray = jsonObject.getJSONArray("results");

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject resultObject = resultsArray.getJSONObject(i);
                JSONObject geoShape = resultObject.getJSONObject("geo_shape");
                JSONObject geometry = geoShape.getJSONObject("geometry");
                JSONArray coordinatesArray = geometry.getJSONArray("coordinates");

                if (geometry.getString("type").equals("MultiPolygon")) {
                    // Si c'est un MultiPolygon, on boucle sur tous les polygones
                    for (int j = 0; j < coordinatesArray.length(); j++) {
                        JSONArray polygonCoordinates = coordinatesArray.getJSONArray(j);
                        allPolygons.add(convertToGeoPoints(polygonCoordinates));
                    }
                } else if (geometry.getString("type").equals("Polygon")) {
                    // Si c'est un Polygon, on le convertit directement
                    allPolygons.add(convertToGeoPoints(coordinatesArray));
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "Erreur lors du parsing du JSON", e);
        }

        return allPolygons;
    }

    private ArrayList<GeoPoint> convertToGeoPoints(JSONArray polygonCoordinates) throws JSONException {
        ArrayList<GeoPoint> geoPoints = new ArrayList<>();

        JSONArray outerRing = polygonCoordinates.getJSONArray(0); // L'anneau principal
        for (int i = 0; i < outerRing.length(); i++) {
            JSONArray point = outerRing.getJSONArray(i);
            double lon = point.getDouble(0);
            double lat = point.getDouble(1);
            geoPoints.add(new GeoPoint(lat, lon));
        }

        // Fermer le polygone
        geoPoints.add(geoPoints.get(0));

        return geoPoints;
    }






}
