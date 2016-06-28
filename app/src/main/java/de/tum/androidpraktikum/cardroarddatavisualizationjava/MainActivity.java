package de.tum.androidpraktikum.cardroarddatavisualizationjava;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import de.tum.androidpraktikum.cardroarddatavisualizationjava.models.Unit;

public class MainActivity extends GvrActivity {
    private static final String TAG = "MainActivity";
    // TODO: change to 10000
    /**
     * Determines how often the data is fetched by the {@link DataRetriever};
     */
    public static final int DATA_FETCH_INTERVAL = 5000;
    /**
     * StereoRenderer used in the main activity.
     */
    private CardboardRenderer cardboardRenderer;
    /**
     * This timer is used to schedule JSON data retrieval.
     */
    private Timer timer;

    //TODO: remove title bar
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup GvrView
        setContentView(R.layout.common_ui);
        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setTransitionViewEnabled(true);
        gvrView.setOnCardboardBackButtonListener(
                new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                });

        // check if system supports OpenGL ES 2.0
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            gvrView.setEGLContextClientVersion(2);

            // Set the renderer to our renderer.
            cardboardRenderer = new CardboardRenderer(this);
            gvrView.setRenderer(cardboardRenderer);
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
        }

        setGvrView(gvrView);

        // Retrieve data from server
        //
        timer = new Timer();
        final Context appContext = this.getApplicationContext();
        // Schedule a TimerTask with 0 delay every 10 seconds.
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String stringUrl = appContext.getResources().getString(R.string.get_last_row);
                ConnectivityManager connMgr = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    new DataRetriever().execute(stringUrl);
                } else {
                    Log.i(TAG,"No network connection available.");
                }
            }
        }, 0, DATA_FETCH_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private class DataRetriever extends AsyncTask<String, Void, String> {

        private static final String DEBUG_TAG = "DEBUG";
        private Unit[] modelData = new Unit[CardboardRenderer.NUM_OF_UNITS];

        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                //Log.d(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();

                int available = is.available();
                // Convert the InputStream into a string
                String contentAsString = readIt(is, 3000);
                return contentAsString;

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } catch (Exception e) {
                // TODO: change; and check all the exceptions in this class
                System.err.println(e.getMessage());
                e.printStackTrace();
                return "T__T";
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        /**
         * Reads an InputStream and converts it to a String.
         */
        public String readIt(InputStream stream, int len) {
            char[] buffer = null;
            try {
                Reader reader = null;
                reader = new InputStreamReader(stream, "UTF-8");
                buffer = new char[len];
                reader.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new String(buffer);
        }

        @Override
        protected String doInBackground(String... urls) {
            if (urls.length < 1) {
                return "";
            }

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String json) {
            super.onPostExecute(json);

            // Data expected in as [ { 'id' : ..., 'Unit1' : { ... }, ..., 'UnitN' : { ... } } ]
            JSONArray ja = null;
            try {
                ja = new JSONArray(json);
                modelData[0] = new Gson().fromJson(ja.getJSONObject(0).getJSONObject("Unit1").toString(), Unit.class);
                modelData[1] = new Gson().fromJson(ja.getJSONObject(0).getJSONObject("Unit2").toString(), Unit.class);
                modelData[2] = new Gson().fromJson(ja.getJSONObject(0).getJSONObject("Unit3").toString(), Unit.class);
                modelData[3] = new Gson().fromJson(ja.getJSONObject(0).getJSONObject("Unit4").toString(), Unit.class);
                modelData[4] = new Gson().fromJson(ja.getJSONObject(0).getJSONObject("Unit5").toString(), Unit.class);
                modelData[5] = new Gson().fromJson(ja.getJSONObject(0).getJSONObject("Unit6").toString(), Unit.class);
                cardboardRenderer.updateModelData(modelData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //throw new RuntimeException(newUnit.toString());
        }
    }
}


