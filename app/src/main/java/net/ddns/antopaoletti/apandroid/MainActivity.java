package net.ddns.antopaoletti.apandroid;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class MainActivity extends AppCompatActivity {

    private static GraphView graph;
    public String lastTimestamp = "";
    public String lastTimestampMin = "";
    public double mobAvg = 0;
    public int mobAvgNum = 100;
    public String doCmd = "";
    ArrayList<HashMap<String, String>> tempList;
    ArrayList<HashMap<String, String>> jobList;
    HashMap<Integer, String> buttons_cmd = new HashMap<Integer, String>();

    //HashMap<String, String> values = null;
    LineGraphSeries<DataPoint> series;
    private String TAG = MainActivity.class.getSimpleName();
    private ListView lv;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_graph);
        //new MyAsyncTask().execute();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        tempList = new ArrayList<>();
        jobList = new ArrayList<>();
        lv = (ListView) findViewById(R.id.list);

        graph = (GraphView) findViewById(R.id.graph);
        //graph.getViewport().setXAxisBoundsManual(true);
        // activate horizontal zooming and scrolling
        graph.getViewport().setScalable(true);
// activate horizontal scrolling
        graph.getViewport().setScrollable(true);
// activate horizontal and vertical zooming and scrolling
        graph.getViewport().setScalableY(true);
// activate vertical scrolling
        graph.getViewport().setScrollableY(true);
        series = new LineGraphSeries<>();
        graph.addSeries(this.series);
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this, new SimpleDateFormat("dd-MM HH:mm:ss")));
        //graph.getGridLabelRenderer().setNumHorizontalLabels(3);
        graph.getGridLabelRenderer().setTextSize(16);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getGridLabelRenderer().setHumanRounding(false);

        final GetTemps sensors = new GetTemps(this);
        final Button button_refresh_temps = (Button) findViewById(R.id.button_refresh_temps);
        final Button button_refresh_jobs = (Button) findViewById(R.id.button_refresh_jobs);
        button_refresh_temps.setOnClickListener(new RefreshOnClickListener(this));
        button_refresh_jobs.setOnClickListener(new CmdOnClickListener(this));


        GetJobs task = new GetJobs(this);
        task.execute();

        buttons_cmd.put(new Integer(R.id.button_on), "RELAY:0");
        buttons_cmd.put(new Integer(R.id.button_off), "RELAY:1");

        Iterator it = buttons_cmd.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = (Map.Entry) it.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            Integer id = pair.getKey();
            final Button button = (Button) findViewById(id);
            button.setOnClickListener(new CmdOnClickListener(this));
        }
/*
        final Button button_on = (Button) findViewById(R.id.button_on);
        button_on.setOnClickListener(new CmdOnClickListener(this));
        final Button button_off = (Button) findViewById(R.id.button_off);
        button_off.setOnClickListener(new CmdOnClickListener(this));
*/
    }

    public void showText(String text, int id) {
        final TextView textView = (TextView) findViewById(id);
        textView.setText(text);
    }

    public void setButtonEnabled(int id, boolean enabled) {
        Button button = (Button) findViewById(id);
        button.setEnabled(enabled);
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

    public class RefreshOnClickListener implements View.OnClickListener
    {

        MainActivity activity;

        public RefreshOnClickListener(MainActivity a) {
            this.activity = a;
        }

        @Override
        public void onClick(View v)
        {
            GetTemps task = new GetTemps(this.activity);
            this.activity.setButtonEnabled(R.id.button_refresh_temps, false);
            task.execute();
        }

    }

    public class CmdOnClickListener implements View.OnClickListener {

        MainActivity activity;

        public CmdOnClickListener(MainActivity a) {
            this.activity = a;
        }

        @Override
        public void onClick(View v) {
            this.activity.setButtonEnabled(R.id.button_refresh_jobs, false);
            this.activity.doCmd = buttons_cmd.get(v.getId());

            if (this.activity.doCmd != null && !this.activity.doCmd.equals("")) {
                Toast.makeText(MainActivity.this, this.activity.doCmd, Toast.LENGTH_LONG).show();
            }
            GetJobs task = new GetJobs(this.activity);
            task.execute();

            //String msg = "";


            /*
            switch(v.getId()) {
                case R.id.button_on:
                    msg = "HEATERS ON";
                    break;
                case R.id.button_off:
                    msg = "HEATERS OFF";
                    break;
            }
            */


/*
            Iterator it = buttons_cmd.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, String> pair = (Map.Entry)it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                Integer id = pair.getKey();
                //Button button = (Button) findViewById(id);
                //button.setEnabled(false);
                this.activity.setButtonEnabled(id, false);
                //it.remove(); // avoids a ConcurrentModificationException
            }
*/
/*
            msg = buttons_cmd.get(v.getId());
            Toast.makeText(this.activity,msg,Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
*/
/*
            it = buttons_cmd.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, String> pair = (Map.Entry)it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                Integer id = pair.getKey();
                this.activity.setButtonEnabled(id, true);
                //Button button = (Button) findViewById(id);
                //button.setEnabled(true);
                //it.remove(); // avoids a ConcurrentModificationException
            }
*/


            //GetJobs task = new GetJobs(this.activity);
            //final Button button_refresh = (Button) findViewById(R.id.button_refresh);
            //button_refresh.setEnabled(false);
            //task.execute();
        }

    }

    private class GetTemps extends AsyncTask<Void, Void, Void> {
        private MainActivity activity;

        public GetTemps(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this, "Json Data for Temps...", Toast.LENGTH_LONG).show();

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
            String url = "http://antopaoletti.ddns.net:10080/temp/export.php?numSamples=20000";
            if (!this.activity.lastTimestamp.equals("")) {
                url += "&from=" + this.activity.lastTimestamp.replace(" ", "%20");
            }
            Log.d(TAG, "Url: " + url);
            String jsonStr = sh.makeServiceCall(url, "anto", "resistore");

            Log.d(TAG, "Response from url: " + jsonStr);
            tempList = new ArrayList<>();
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
                        String unit = c.getString("unit");

                        // tmp hash map for single contact
                        //HashMap<String, String> values = new HashMap<>();

                        // adding each child node to HashMap key => value
                        HashMap<String, String> values = new HashMap<String, String>();


                        int mobAvgStart = i - this.activity.mobAvgNum;
                        if (mobAvgStart < 0) {
                            mobAvgStart = 0;
                        }
                        this.activity.mobAvg = 0;
                        for (int j = mobAvgStart; j <= i; j++) {
                            JSONObject c1 = data.getJSONObject(j);
                            String value1 = c.getString("value");
                            this.activity.mobAvg += new Double(value1).doubleValue();
                        }
                        this.activity.mobAvg /= i - mobAvgStart + 1;

                        if (!timestamp.substring(0, 16).equals(this.activity.lastTimestampMin)) {
                            values.put("unit", unit);
                            values.put("timestamp", timestamp);
                            this.activity.lastTimestamp = timestamp;
                            this.activity.lastTimestampMin = timestamp.substring(0, 16);
                            //values.put("value", value);
                            values.put("value", new Double(this.activity.mobAvg).toString());
                            //values.put(timestamp, value);
                            Log.d(TAG, timestamp + " " + this.activity.lastTimestampMin + " " + value + " " + this.activity.mobAvg + "[" + mobAvgStart + ", " + i + "]");

                            // adding contact to contact list
                            tempList.add(values);
                        }

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
                    if (timestamp != null) {
                        Date date = sdf.parse(timestamp);
                        Log.d(TAG, timestamp + " " + date.toString() + " " + doubleValue);
                        DataPoint datapoint = new DataPoint(date, doubleValue.doubleValue());
                        this.activity.series.appendData(datapoint, true, 10000);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                //it.remove(); // avoids a ConcurrentModificationException
            }

            if (tempList.size() > 0) {
                HashMap<String, String> lastValues = tempList.get(tempList.size() - 1);
                this.activity.showText(lastValues.get("timestamp") + ": " + lastValues.get("value") + Html.fromHtml(lastValues.get("unit")), R.id.text_last_temp);
            }
            this.activity.setButtonEnabled(R.id.button_refresh_temps, true);


        }
    }

    private class GetJobs extends AsyncTask<Void, Void, Void> {
        private MainActivity activity;

        public GetJobs(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this, "Json Data for Jobs...", Toast.LENGTH_LONG).show();

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
            String url = "http://antopaoletti.ddns.net:10080/temp/jobs.php";
            if (this.activity.doCmd != null && !this.activity.doCmd.equals("")) {
                url += "?cmd=" + this.activity.doCmd.replace(":", "%3A");
            }
            //if (!this.activity.lastTimestamp.equals("")) {
            //    url += "&from=" + this.activity.lastTimestamp.replace(" ", "%20");
            //}
            Log.d(TAG, "Url: " + url);
            String jsonStr = sh.makeServiceCall(url, "anto", "resistore");

            Log.d(TAG, "Response from url: " + jsonStr);
            jobList = new ArrayList<>();
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray data = jsonObj.getJSONArray("data");

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject c = data.getJSONObject(i);
                        String timestamp = c.getString("timestamp");
                        String cmd = c.getString("cmd");
                        String status = c.getString("status");
                        String ended = c.getString("ended");

                        // tmp hash map for single contact
                        //HashMap<String, String> values = new HashMap<>();

                        // adding each child node to HashMap key => value
                        HashMap<String, String> values = new HashMap<String, String>();


                        values.put("timestamp", timestamp);
                        //this.activity.lastTimestamp = timestamp;
                        values.put("cmd", cmd);
                        values.put("status", status);
                        values.put("ended", status);
                        Log.d(TAG, timestamp + ": " + cmd + " " + status + "[" + ended + "]");

                        // adding contact to contact list
                        jobList.add(values);
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

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            //Iterator it = values.entrySet().iterator();
            //while (it.hasNext()) {
            TableLayout table = (TableLayout) findViewById(R.id.table_jobs);

            table.removeAllViews();
            for (int i = 0; i < jobList.size() && i < 4; i++) {

                // create a new TableRow
                TableRow row = new TableRow(this.activity);
                // create a new TextView for showing xml data
                HashMap<String, String> values = jobList.get(i);
                String timestamp = values.get("timestamp");
                String cmd = values.get("cmd");

                TextView t = new TextView(this.activity);
                t.setText(timestamp + ": ");
                row.addView(t);
                t = new TextView(this.activity);
                t.setText(Html.fromHtml("<strong>" + cmd + "</strong>"));
                row.addView(t);
                // add the TableRow to the TableLayout
                table.addView(row, new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
            }
            this.activity.setButtonEnabled(R.id.button_refresh_jobs, true);
            this.activity.doCmd = "";


        }
    }

}
