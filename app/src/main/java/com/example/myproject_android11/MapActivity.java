package com.example.myproject_android11;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private FirebaseFirestore db;

    private String testNameCommune;

    private String groupId; // Identifiant du groupe (passé depuis l'intention)
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String TAG = "MapActivity";

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

        // Initialisation de Firestore
        db = FirebaseFirestore.getInstance();

        // Récupérer l'ID du groupe passé par l'intention
        groupId = getIntent().getStringExtra("group_id");
        if (groupId == null) {
            Toast.makeText(this, "Erreur : Aucun groupe sélectionné", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Chargement des données
        fetchData();
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

    private void drawPolygone(ArrayList<CommunePolygon> allPolygons, HashMap<String, Float> absenceRatioByCommune) {
        for (CommunePolygon communePolygon  : allPolygons) {
            if (communePolygon.geoPoints.size() < 3) continue; // Vérifier qu'il y a au moins 3 points pour un polygone

            Polygon polygon = new Polygon();
            polygon.setPoints(communePolygon.geoPoints);

            // Déterminer la couleur en fonction du ratio d'absences
            String communeName = communePolygon.name.toLowerCase();// Méthode à implémenter
            float absenceRatio = absenceRatioByCommune.getOrDefault(communeName, 0f);


            int color = getColorForAbsenceRatio(absenceRatio); // Méthode à implémenter

            // Log pour vérifier la couleur appliquée
            Log.d(TAG, "Commune: " + communeName + ", Ratio: " + absenceRatio + ", Couleur appliquée: " + Integer.toHexString(color));

            // Définir le style du polygone
            polygon.getFillPaint().setColor(color); // Couleur en fonction du ratio d'absences
            polygon.getOutlinePaint().setColor(Color.BLACK); // Bordure noire
            polygon.getOutlinePaint().setStrokeWidth(2f);

            // Ajouter le polygone sur la carte
            mapView.getOverlays().add(polygon);
        }

        mapView.invalidate(); // Rafraîchir la carte
    }

    private void fetchData() {
        new Thread(() -> {
            String apiUrl = "https://opendata.brussels.be/api/explore/v2.1/catalog/datasets/limites-administratives-des-communes-en-region-de-bruxelles-capitale/records?limit=20";
            String jsonResponse = getJsonFromUrl(apiUrl);

            if (jsonResponse != null) {
                // Récupérer la liste des polygones
                ArrayList<CommunePolygon> polygons = parseJson(jsonResponse);

                // Récupérer les données de présence et mettre à jour les couleurs
                fetchPresenceData(polygons);
            } else {
                Log.e(TAG, "Failed to fetch data from URL");
            }
        }).start();
    }

    private void fetchPresenceData(ArrayList<CommunePolygon> polygons) {
        Log.d(TAG, "Début fetchPresenceData");

        db.collection("presence")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalDocs = queryDocumentSnapshots.size();
                    Log.d(TAG, "Documents présence trouvés: " + totalDocs);

                    if (totalDocs == 0) {
                        Log.w(TAG, "Aucune donnée pour ce groupe");
                        drawPolygone(polygons, new HashMap<>());
                        return;
                    }

                    // Compteur pour suivre les requêtes terminées
                    AtomicInteger processedDocs = new AtomicInteger(0);
                    HashMap<String, Integer> presenceCount = new HashMap<>();
                    HashMap<String, Integer> absenceCount = new HashMap<>();

                    for (CommunePolygon commune : polygons) {
                        String normalizedName = commune.name.toLowerCase();
                        presenceCount.put(normalizedName, 0);
                        absenceCount.put(normalizedName, 0);
                    }


                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString("userId");
                        Boolean isPresent = doc.getBoolean("isPresent");

                        if (userId == null || isPresent == null) {
                            Log.e(TAG, "Données invalides dans le document " + doc.getId());
                            processedDocs.incrementAndGet();
                            continue;
                        }

                        db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String commune = userDoc.getString("commune");
                                    // Normaliser la casse
                                    String normalizedCommune = commune.trim().toLowerCase();
                                    if (normalizedCommune != null) {
                                        synchronized (this) {
                                            if (isPresent) {
                                                presenceCount.put(normalizedCommune, presenceCount.getOrDefault(normalizedCommune, 0) + 1);
                                            } else {
                                                absenceCount.put(normalizedCommune, absenceCount.getOrDefault(normalizedCommune, 0) + 1);
                                            }
                                        }
                                        Log.d(TAG, "Mise à jour - " + normalizedCommune +
                                                " | Présences: " + presenceCount.get(normalizedCommune) +
                                                " | Absences: " + absenceCount.get(normalizedCommune));
                                    }

                                    if (processedDocs.incrementAndGet() == totalDocs) {
                                        calculateAndDrawRatios(polygons, presenceCount, absenceCount);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Erreur user doc", e);
                                    if (processedDocs.incrementAndGet() == totalDocs) {
                                        calculateAndDrawRatios(polygons, presenceCount, absenceCount);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erreur fetch présence", e));
    }

    private void calculateAndDrawRatios(ArrayList<CommunePolygon> polygons,
                                        HashMap<String, Integer> presenceCount,
                                        HashMap<String, Integer> absenceCount) {
        HashMap<String, Float> ratios = new HashMap<>();

        for (String commune : presenceCount.keySet()) {
            int total = presenceCount.get(commune) + absenceCount.getOrDefault(commune, 0);
            float ratio = (float) absenceCount.getOrDefault(commune, 0) / total;
            ratios.put(commune, ratio);

            // CE LOG S'AFFICHERA MAINTENANT
            Log.d(TAG, "Ratio final - " + commune +
                    " | Présences: " + presenceCount.get(commune) +
                    " | Absences: " + absenceCount.getOrDefault(commune, 0) +
                    " | Ratio: " + ratio);
        }

        drawPolygone(polygons, ratios);
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

    private ArrayList<CommunePolygon> parseJson(String jsonString) {
        ArrayList<CommunePolygon> allPolygons = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray resultsArray = jsonObject.getJSONArray("results");

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject resultObject = resultsArray.getJSONObject(i);
                String communeName = resultObject.getString("name_fr");
                JSONObject geoShape = resultObject.getJSONObject("geo_shape");
                JSONObject geometry = geoShape.getJSONObject("geometry");
                JSONArray coordinatesArray = geometry.getJSONArray("coordinates");

                if (geometry.getString("type").equals("MultiPolygon")) {
                    // Si c'est un MultiPolygon, on boucle sur tous les polygones
                    for (int j = 0; j < coordinatesArray.length(); j++) {
                        JSONArray polygonCoordinates = coordinatesArray.getJSONArray(j);
                        allPolygons.add(new CommunePolygon(communeName, convertToGeoPoints(polygonCoordinates)));
                    }
                } else if (geometry.getString("type").equals("Polygon")) {
                    // Si c'est un Polygon, on le convertit directement
                    allPolygons.add(new CommunePolygon(communeName, convertToGeoPoints(coordinatesArray)));
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


    private int getColorForAbsenceRatio(float absenceRatio) {

        if (Float.isNaN(absenceRatio)) {
            return Color.argb(100, 150, 150, 150); // Gris pour aucune donnée
        }
        // Si le ratio d'absences est supérieur à 0.5, la couleur est rouge
        else if (absenceRatio > 0.5f) {
            return Color.argb(100, 255, 0, 0); // Rouge
        }
        else {
            return Color.argb(100, 0, 255, 0); // Vert
        }
    }

    private static class CommunePolygon {
        String name;
        ArrayList<GeoPoint> geoPoints;

        CommunePolygon(String name, ArrayList<GeoPoint> geoPoints) {
            this.name = name;
            this.geoPoints = geoPoints;
        }
    }
}