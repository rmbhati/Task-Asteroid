package com.ez.asteroid.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.ez.asteroid.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends Activity {
    private RecyclerView recyclerView;
    private WebView webView;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        TextView from = findViewById(R.id.from);
        TextView to = findViewById(R.id.to);
        TextView submit = findViewById(R.id.submit);
        webView = findViewById(R.id.webView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        from.setText(today);
        to.setText(today);

        //from.setOnClickListener(view -> datePicker(from));
        //to.setOnClickListener(view -> datePicker(to));
        from.setOnClickListener(view -> {
            //startActivity(new Intent(this, com.ez.asteroid.ui.match.MainActivity.class));
            startActivity(new Intent(this, BallonActivity.class));
        });
        to.setOnClickListener(view -> {
            startActivity(new Intent(this, com.ez.asteroid.ui.match3.Match3.class));
        });

        submit.setOnClickListener(view -> {
            /*if (from.getText().toString().isEmpty()) {
                Toast.makeText(MainActivity.this, "Select from Date", Toast.LENGTH_SHORT).show();
            } else if (to.getText().toString().isEmpty()) {
                Toast.makeText(MainActivity.this, "Select to Date", Toast.LENGTH_SHORT).show();
            } else {
                callAPI(from.getText().toString(), to.getText().toString());
            }*/
            startActivity(new Intent(this, com.ez.asteroid.ui.match2.Match2Activity.class));
        });
    }

    private void datePicker(TextView date) {
        final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this, (view, year, monthOfYear, dayOfMonth) -> {
            date.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
        }, mYear, mMonth, mDay);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    public void callAPI(String from, String to) {
        showDialog();
        String url = "https://api.nasa.gov/neo/rest/v1/feed?start_date=" + from + "&end_date=" + to + "&api_key=DEMO_KEY";
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    JSONObject object = new JSONObject(response.getString("near_earth_objects"));
                    final ArrayList<HashMap<String, String>> listData = new ArrayList<>();
                    final ArrayList<HashMap<String, String>> listSubData = new ArrayList<>();
                    ArrayList<HashMap<String, String>> listChart = new ArrayList<>();
                    for (Iterator<String> iter = object.keys(); iter.hasNext(); ) {
                        HashMap<String, String> map = new HashMap<>();
                        String key = iter.next();
                        //map.put("key", key);
                        map.put("key", "data");
                        map.put("value", object.getString(key));
                        listData.add(map);
                    }

                    for (int i = 0; i < listData.size(); i++) {
                        JSONArray jsonArray = new JSONArray(listData.get(i).get("value"));
                        for (int j = 0; j < jsonArray.length(); j++) {
                            HashMap<String, String> map = new HashMap<>();
                            HashMap<String, String> mapChart = new HashMap<>();
                            Double min_avg = 0.0;
                            Double max_avg = 0.0;
                            Integer avg_count = 0;
                            Double maxKMperHR = -1.0;
                            Double minKMperHR = -1.0;
                            JSONObject objectdata = jsonArray.getJSONObject(j);
                            map.put("astID", objectdata.getString("id"));
                            //Third Point
                            JSONObject objEstDiameter = objectdata.getJSONObject("estimated_diameter").getJSONObject("kilometers");
                            min_avg = min_avg + objEstDiameter.getDouble("estimated_diameter_min");
                            max_avg = max_avg + objEstDiameter.getDouble("estimated_diameter_max");
                            avg_count = avg_count + 1;

                            JSONArray approchArray = new JSONArray(objectdata.get("close_approach_data").toString());
                            if (approchArray.length() > 0) {
                                JSONObject objCloseApproachData = approchArray.getJSONObject(0);
                                map.put("date", objCloseApproachData.getString("close_approach_date"));
                                mapChart.put("label", objCloseApproachData.getString("close_approach_date"));
                                mapChart.put("value", jsonArray.length() + "");
                                //Fastest speed
                                JSONObject objRelVelocity = objCloseApproachData.getJSONObject("relative_velocity");
                                if (objRelVelocity.getDouble("kilometers_per_hour") > maxKMperHR)
                                    maxKMperHR = objRelVelocity.getDouble("kilometers_per_hour");

                                //Closest Distance(min Distance)
                                JSONObject objMissDistance = objCloseApproachData.getJSONObject("miss_distance");
                                if (objMissDistance.getDouble("kilometers") > minKMperHR)
                                    minKMperHR = objMissDistance.getDouble("kilometers");
                            }
                            Double minAvg = min_avg / avg_count;
                            Double maxAvg = max_avg / avg_count;
                            map.put("min_avg", minAvg + "");
                            map.put("max_avg", maxAvg + "");
                            map.put("fast", maxKMperHR + "");
                            map.put("min", minKMperHR + "");
                            listSubData.add(map);
                            listChart.add(mapChart);
                        }
                    }


                    if (listSubData.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "No data Found", Toast.LENGTH_SHORT).show();
                    } else {
                        //Chart set data
                        Set<HashMap<String, String>> s = new HashSet<>();
                        s.addAll(listChart);
                        listChart = new ArrayList<>();
                        listChart.addAll(s);

                        JSONArray jsonArray = new JSONArray(listChart);

                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.setWebChromeClient(new WebChromeClient());
                        webView.loadUrl("file:///android_asset/bar_chart.html");


                        webView.setWebViewClient(new WebViewClient() {

                            @Override
                            public void onPageFinished(WebView view, String url) {
                                super.onPageFinished(view, url);
                                webView.loadUrl("javascript:myJavaScriptFunc(" + jsonArray + ",'100')");
                            }
                        });

                        //set to adapter
                        recyclerView.setAdapter(new HomeAdapter(getApplicationContext(), listSubData));
                    }

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
                hideDialog();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(getApplicationContext(), "We got an error", Toast.LENGTH_SHORT).show();
                hideDialog();
            }
        });
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit!")
                .setMessage("Are you sure you want to exit from app?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> finish())
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                })
                .show();
    }

    private void showDialog() {
        if (!pDialog.isShowing()) {
            pDialog.show();
            pDialog.setContentView(R.layout.progress_bar);
            pDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}
