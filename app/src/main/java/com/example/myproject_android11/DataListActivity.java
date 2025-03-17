package com.example.myproject_android11;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DataListActivity extends AppCompatActivity {

    private static final String TAG = "DataListActivity";
    private ListView listView;
    private List<String> dataList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list); // Ensure you have this layout

        // Enable StrictMode for network operations on the main thread (for simplicity in this example)
        // In a real app, use background threads (e.g., AsyncTask, Executor, or coroutines)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        listView = findViewById(R.id.dataListView); // Ensure you have a ListView with this ID in your layout
        dataList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        fetchData();
    }

    private void fetchData() {
        //String apiUrl = "https://opendata.brussels.be/api/explore/v2.1/catalog/datasets/limites-administratives-des-communes-en-region-de-bruxelles-capitale/records?limit=20";
        String apiUrl = "https://opendata.brussels.be/api/explore/v2.1/catalog/datasets/limites-administratives-des-communes-en-region-de-bruxelles-capitale/records?limit=2";
        String jsonResponse = getJsonFromUrl(apiUrl);

        if (jsonResponse != null) {
            parseJson(jsonResponse);
        } else {
            Log.e(TAG, "Failed to fetch data from URL");
            dataList.add("Failed to fetch data");
            adapter.notifyDataSetChanged();
        }
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

    private void parseJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray resultsArray = jsonObject.getJSONArray("results");

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject resultObject = resultsArray.getJSONObject(i);
                String nameFr = resultObject.getString("name_fr");
                String nameNl = resultObject.getString("name_nl");
                dataList.add("Name FR: " + nameFr + ", Name NL: " + nameNl);
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
            dataList.add("Error parsing JSON");
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        }
    }
}