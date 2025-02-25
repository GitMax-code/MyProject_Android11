package com.example.myproject_android11;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuration d'osmdroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);  // Utilise activity_map.xml

        // Initialisation de la carte
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(50.8503, 4.3517)); // Bruxelles

        // Initialiser la requête API
        requestQueue = Volley.newRequestQueue(this);
        fetchCommuneBorders();
    }

    private void fetchCommuneBorders() {
        String url = "https://opendata.brussels.be/api/explore/v2.1/catalog/datasets/limites-administratives-des-communes-en-region-de-bruxelles-capitale/records?limit=20";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray records = response.getJSONArray("results");
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject commune = records.getJSONObject(i);
                                String name = commune.getJSONObject("fields").getString("name_fr");
                                JSONObject geoShape = commune.getJSONObject("fields").getJSONObject("geo_shape");
                                String geoType = geoShape.getString("type"); // Récupérer le type de géométrie
                                JSONArray coordinates = geoShape.getJSONArray("coordinates");

                                // Vérifier le type de géométrie et dessiner en conséquence
                                if ("Polygon".equals(geoType)) {
                                    drawPolygon(coordinates, name); // Dessiner un polygone
                                } else if ("MultiPolygon".equals(geoType)) {
                                    drawMultiPolygon(coordinates, name); // Dessiner plusieurs polygones
                                } else {
                                    Log.e("GeoShapeError", "Type de géométrie non pris en charge : " + geoType);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("API_ERROR", "Erreur de récupération des données: " + error.getMessage());
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void drawPolygon(JSONArray coordinates, String name) {
        List<GeoPoint> geoPoints = new ArrayList<>();

        try {
            JSONArray polygonArray = coordinates.getJSONArray(0); // Récupérer le premier polygone
            for (int j = 0; j < polygonArray.length(); j++) {
                JSONArray point = polygonArray.getJSONArray(j);
                double lon = point.getDouble(0);
                double lat = point.getDouble(1);
                geoPoints.add(new GeoPoint(lat, lon));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Créer et configurer le polygone
        Polygon polygon = new Polygon();
        polygon.setPoints(geoPoints);
        polygon.setFillColor(0x301E90FF); // Bleu semi-transparent
        polygon.setStrokeColor(0xFF1E90FF); // Bleu foncé
        polygon.setStrokeWidth(3f);
        polygon.setTitle(name);

        // Ajouter le polygone à la carte
        mapView.getOverlayManager().add(polygon);
        mapView.invalidate();
    }

    private void drawMultiPolygon(JSONArray coordinates, String name) {
        for (int i = 0; i < coordinates.length(); i++) {
            try {
                // Récupérer chaque polygone dans le multipolygone
                JSONArray polygonCoordinates = coordinates.getJSONArray(i);
                List<GeoPoint> geoPoints = new ArrayList<>();

                // Ajouter les points du polygone
                for (int j = 0; j < polygonCoordinates.length(); j++) {
                    JSONArray point = polygonCoordinates.getJSONArray(j);
                    double lon = point.getDouble(0);
                    double lat = point.getDouble(1);
                    geoPoints.add(new GeoPoint(lat, lon));
                }

                // Dessiner le polygone
                Polygon polygon = new Polygon();
                polygon.setPoints(geoPoints);
                polygon.setFillColor(0x301E90FF); // Bleu semi-transparent
                polygon.setStrokeColor(0xFF1E90FF); // Bleu foncé
                polygon.setStrokeWidth(3f);
                polygon.setTitle(name);

                // Ajouter le polygone à la carte
                mapView.getOverlayManager().add(polygon);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Rafraîchir la carte
        mapView.invalidate();
    }
}
