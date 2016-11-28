package net.ddns.antopaoletti.apandroid;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private ListView lv;
    private static GraphView graph;
    ArrayList<HashMap<String, String>> tempList;
    //HashMap<String, String> values = null;

    public class MyOnClickListener implements View.OnClickListener
    {

        MainActivity activity;
        public MyOnClickListener(MainActivity a) {
            this.activity = a;
        }

        @Override
        public void onClick(View v)
        {
            GetTemps sensors = new GetTemps(this.activity);
            final Button button_refresh = (Button) findViewById(R.id.button_refresh);
            button_refresh.setEnabled(false);
            sensors.execute();
        }

    };

    private class GetTemps extends AsyncTask<Void, Void, Void> {
        public MainActivity activity;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this,"Json Data is downloading",Toast.LENGTH_LONG).show();

        }


        public GetTemps(MainActivity a)
        {
            this.activity = a;
        }


        public String decompress(byte[] compressed) throws IOException {
            final int BUFFER_SIZE = 32;
            ByteArrayInputStream is = new ByteArrayInputStream(compressed);
            GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
            StringBuilder string = new StringBuilder();
            byte[] data = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gis.read(data)) != -1) {
                string.append(new String(data, 0, bytesRead));
            }
            gis.close();
            is.close();
            return string.toString();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            //values = new HashMap<String, String>();
            HttpHandler sh = new HttpHandler();
            // Making a request to url and getting response
            String url = "http://antopaoletti.ddns.net:10080/temp/export.php?numSamples=20";
            String jsonStr = sh.makeServiceCall(url, "anto", "resistore");

            Log.e(TAG, "Response from url: " + jsonStr);
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray data = jsonObj.getJSONArray("data");

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject c = data.getJSONObject(i);
                        String timestamp = c.getString("timestamp");
                        String value = c.getString("value");

                        // tmp hash map for single contact
                        //HashMap<String, String> values = new HashMap<>();

                        // adding each child node to HashMap key => value
                        HashMap<String, String> values = new HashMap<String, String>();
                        values.put("timestamp", timestamp);
                        values.put("value", value);
                        //values.put(timestamp, value);
                        Log.d(TAG, timestamp+" "+value);

                        // adding contact to contact list
                        tempList.add(values);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                }

            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            /*
            ListAdapter adapter = new SimpleAdapter(MainActivity.this, tempList,
                    R.layout.list_item_temp, new String[]{ "timestamp","value"},
                    new int[]{R.id.timestamp, R.id.value});
            lv.setAdapter(adapter);
            */
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            //Iterator it = values.entrySet().iterator();
            //while (it.hasNext()) {
            for (int i = 0; i < tempList.size(); i++) {
                //Map.Entry pair = (Map.Entry)it.next();
                HashMap<String, String> values = tempList.get(i);
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                //String timestamp = (String)pair.getKey();
                //String value = (String)pair.getValue();
                String timestamp = values.get("timestamp");
                String value = values.get("value");
                Double doubleValue = new Double(value);

                try {
                    Date date = sdf.parse(timestamp);
                    Log.d(TAG, timestamp+" "+date.toString()+" "+doubleValue);
                    DataPoint datapoint = new DataPoint(date, doubleValue.doubleValue());
                    series.appendData(datapoint, true, 100);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                //it.remove(); // avoids a ConcurrentModificationException
            }
            this.activity.setGraphData(series);
            this.activity.enableButtonRefresh();


        }
    }

    private class GetContacts extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this,"Json Data is downloading",Toast.LENGTH_LONG).show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();
            // Making a request to url and getting response
            String url = "http://api.androidhive.info/contacts/";
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray contacts = jsonObj.getJSONArray("contacts");

                    // looping through All Contacts
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);
                        String id = c.getString("id");
                        String name = c.getString("name");
                        String email = c.getString("email");
                        String address = c.getString("address");
                        String gender = c.getString("gender");

                        // Phone node is JSON Object
                        JSONObject phone = c.getJSONObject("phone");
                        String mobile = phone.getString("mobile");
                        String home = phone.getString("home");
                        String office = phone.getString("office");

                        // tmp hash map for single contact
                        HashMap<String, String> contact = new HashMap<>();

                        // adding each child node to HashMap key => value
                        contact.put("id", id);
                        contact.put("name", name);
                        contact.put("email", email);
                        contact.put("mobile", mobile);

                        // adding contact to contact list
                        tempList.add(contact);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                }

            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            ListAdapter adapter = new SimpleAdapter(MainActivity.this, tempList,
                    R.layout.list_item, new String[]{ "email","mobile"},
                    new int[]{R.id.email, R.id.mobile});
            lv.setAdapter(adapter);
        }
    }

    class MyAsyncTask extends AsyncTask<String, String, Void> {
        private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        private InputStream inputStream = null;
        private String result = "";

        protected void onPreExecute() {
            progressDialog.setMessage("Downloading your data...");
            progressDialog.show();
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface arg0) {
                    MyAsyncTask.this.cancel(true);
                }
            });
        }
        @Override
        protected Void doInBackground(String... params) {

            String url_select = "http://anto:resistore@antopaoletti.ddns.net:10080/temp/export.php?source=TEMP_SALOTTO";
            progressDialog.setMessage("Downloading from "+url_select);

            ContentValues param = new ContentValues();
            URL url = null;
            URLConnection urlConnection = null;

            try {
                // Set up HTTP post
                url = new URL(url_select);
                urlConnection = url.openConnection();
                urlConnection.setConnectTimeout(1000);
            // Convert response to string using String Builder
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //progressDialog.setMessage("Connection done: "+urlConnection.getContentLength()+ " B");

            try {
                BufferedReader bReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"), 8);
                StringBuilder sBuilder = new StringBuilder();

                String line = null;
                while ((line = bReader.readLine()) != null) {
                    sBuilder.append(line + "\n");
                    progressDialog.setMessage("Got "+sBuilder.toString().length()+" B");
                }

                inputStream.close();
                result = sBuilder.toString();
                progressDialog.setMessage("Downloaded "+result.length()+" B !");

            } catch (Exception e) {
                Log.e("StringBuilding", "Error converting result " + e.toString());
            }
            //return null;
            return null;
        } // protected Void doInBackground(String... params)
        protected void onPostExecute(Void v) {
            //parse JSON data
            try {
                JSONArray jArray = new JSONArray(result);
                /*
                for(int i=0; i < jArray.length(); i++) {

                    JSONObject jObject = jArray.getJSONObject(i);

                    String name = jObject.getString("name");
                    String tab1_text = jObject.getString("tab1_text");
                    int active = jObject.getInt("active");

                } // End Loop
                */
                this.progressDialog.dismiss();
            } catch (JSONException e) {
                Log.e("JSONException", "Error: " + e.toString());
            } // catch (JSONException e)
        } // protected void onPostExecute(Void v)
    } //class MyAsyncTask extends AsyncTask<String, String, Void>


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private InputStream getStream(String url) {
        try {
            URL myurl = new URL(url);
            URLConnection urlConnection = myurl.openConnection();
            urlConnection.setConnectTimeout(1000);
            return urlConnection.getInputStream();
        } catch (Exception ex) {
            return null;
        }
    }

    public static void setGraphData(LineGraphSeries<DataPoint> series) {
        graph.removeAllSeries();
        graph.addSeries(series);
        //graph.getViewport().setMinX(series.getLowestValueX());
        //graph.getViewport().setMaxX(series.getHighestValueX());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_graph);
        //new MyAsyncTask().execute();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        tempList = new ArrayList<>();
        lv = (ListView) findViewById(R.id.list);

        graph = (GraphView) findViewById(R.id.graph);

        /*
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3)
        });
        */

        //graph.addSeries(series);

        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);
        //graph.getViewport().setXAxisBoundsManual(true);

// as we use dates as labels, the human rounding to nice readable numbers
// is not necessary
        graph.getGridLabelRenderer().setHumanRounding(false);

        //setGraphData(series);


        //new GetContacts().execute();
        //new GetTemps().execute();

        final GetTemps sensors = new GetTemps(this);
        final Button button_refresh = (Button) findViewById(R.id.button_refresh);
        button_refresh.setOnClickListener(new MyOnClickListener(this));
    }

    public void enableButtonRefresh() {
        final Button button_refresh = (Button) findViewById(R.id.button_refresh);
        button_refresh.setEnabled(true);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client.connect();
        //AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //AppIndex.AppIndexApi.end(client, getIndexApiAction());
        //client.disconnect();
    }
}
