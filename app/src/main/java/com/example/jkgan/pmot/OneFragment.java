package com.example.jkgan.pmot;

/**
 * Created by JKGan on 11/11/2015.
 */
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.jkgan.pmot.Http.HttpRequest;
import com.example.jkgan.pmot.Shops.Promotion;
import com.example.jkgan.pmot.Shops.Shop;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class OneFragment extends Fragment{
    SwipeRefreshLayout mSwipeRefreshLayout;
    FloatingActionButton scanButton;

    public OneFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_one,container, false);
        scanButton = (FloatingActionButton) rootView.findViewById(R.id.fab);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_promotion_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange, R.color.green, R.color.blue);

        if(MyApplication.isNetworkAvailable(getActivity())) {

            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshContent();
                }
            });

            scanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                Snackbar.make(rootView.findViewById((R.id.coordinator)), "Something wrong", Snackbar.LENGTH_LONG).show();

                    Intent intent = new Intent(getActivity(), QRcodeScanner.class);
                    startActivity(intent);
                }
            });

            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage("Loading List...");
            dialog.setCancelable(false);
            dialog.setInverseBackgroundForced(false);
            dialog.show();
            PromotionsListASYNC loginTask = new PromotionsListASYNC();
            loginTask.execute(MyApplication.getUser().getToken());
            dialog.dismiss();
        } else {
//            scanButton.setVisibility(View.INVISIBLE);
            scanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Snackbar.make(rootView.findViewById((R.id.coordinator)), "Unable to scan in offline", Snackbar.LENGTH_LONG).show();
                }
            });
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(getActivity().findViewById((R.id.coordinator)), "Unable to refresh", Snackbar.LENGTH_LONG).show();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }, 2200);
                }
            });


            final List<Promotion> allItems = new ArrayList<Promotion>();
            SQLiteDatabase sqLiteDatabase;
            sqLiteDatabase = getActivity().openOrCreateDatabase("db_Pmot", getActivity().MODE_PRIVATE, null);
            Cursor resultSet = sqLiteDatabase.rawQuery("Select * from promotions, shops where promotions.shop_id = shops.id ORDER BY promotions.created_at DESC;", null);
            System.out.println("====AAAA=================");

            for(int i = 0; i < resultSet.getColumnCount(); i++) {
                System.out.println(resultSet.getColumnName(i));
            }

            if (resultSet.moveToFirst()){
                do{

                    String name = resultSet.getString(resultSet.getColumnIndex("name"));
                    System.out.println("====BBBB=================" + name);

                    allItems.add(new Promotion(resultSet.getString(1), resultSet.getString(2), resultSet.getString(0), "", "", resultSet.getString(3), resultSet.getString(9), resultSet.getString(10),resultSet.getString(0), resultSet.getString(resultSet.getColumnIndex("starts_at")), resultSet.getString(resultSet.getColumnIndex("expires_at")), resultSet.getString(12)));


                }while (resultSet.moveToNext());
            }

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    LinearLayoutManager lLayout = new LinearLayoutManager(getActivity());
                    lLayout.setOrientation(LinearLayoutManager.VERTICAL);

                    RecyclerView rView;
                    if(getActivity() == null) {
                        MainActivity main = new MainActivity();
                        rView = (RecyclerView) main.findViewById(R.id.rv);
                    } else {
                        rView = (RecyclerView) getActivity().findViewById(R.id.rv);
                        rView.setLayoutManager(lLayout);
                    }

                    PromotionRecyclerViewAdapter rcAdapter = new PromotionRecyclerViewAdapter(getActivity(), allItems);
                    rView.setAdapter(rcAdapter);
                }
            };

            Thread thr = new Thread(run);
            thr.start();

        }

        return rootView;
    }

    private void refreshContent() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                PromotionsListASYNC loginTask = new PromotionsListASYNC();
                loginTask.execute(MyApplication.getUser().getToken());
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 3000);
    }

    private class PromotionsListASYNC extends AsyncTask<String, Void, JSONObject> {

//        final ProgressDialog dialog = new ProgressDialog(getActivity());

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected JSONObject doInBackground(String... params) {

            final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("token", params[0]));
            String strURL = MyApplication.getApiUrl() + "/promotions";
            HttpRequest request = new HttpRequest();

            return request.makeHttpRequest(strURL, "GET", parameters);
        }

        @Override
        protected void onPostExecute(JSONObject result) {

            JSONArray jsonArr = null;
            PmotDB db = new PmotDB(getActivity().getApplicationContext());
            try {
                List<Promotion> allItems = new ArrayList<Promotion>();
                jsonArr = result.getJSONArray("Shops");
                JSONObject jsnObj2 = null;
                if(jsonArr != null) {

                    int length = jsonArr.length();
                    for(int i = 0; i < length; i++) {
                        jsnObj2 = jsonArr.getJSONObject(i);
                        allItems.add(new Promotion(jsnObj2.optString("pName"), jsnObj2.optString("description"), jsnObj2.optString("id"), jsnObj2.getJSONObject("image").getJSONObject("medium").optString("url"), jsnObj2.getJSONObject("image").getJSONObject("small").optString("url"), jsnObj2.optString("term_and_condition"), jsnObj2.optString("name"),jsnObj2.optString("address"),jsnObj2.optString("sId"), getDate(jsnObj2.optString("starts_at")), getDate(jsnObj2.optString("expires_at")), jsnObj2.optString("phone")));

                        db.fnRunSQL("INSERT INTO promotions (id, name, description, term_and_condition, starts_at, expires_at, shop_id) VALUES ("+jsnObj2.optInt("id")+", \""+jsnObj2.optString("pName")+"\", \""+jsnObj2.optString("description")+"\", \""+jsnObj2.optString("term_and_condition")+"\", \""+getDate(jsnObj2.optString("starts_at"))+"\", \""+getDate(jsnObj2.optString("expires_at"))+"\", "+jsnObj2.optInt("shop_id")+");",
                                getActivity().getApplicationContext());
                    }

                    LinearLayoutManager lLayout = new LinearLayoutManager(getActivity());
                    lLayout.setOrientation(LinearLayoutManager.VERTICAL);

                    RecyclerView rView;
                    if(getActivity() == null) {
                        MainActivity main = new MainActivity();
                        rView = (RecyclerView) main.findViewById(R.id.rv);
                    } else {
                        rView = (RecyclerView) getActivity().findViewById(R.id.rv);
                        rView.setLayoutManager(lLayout);
                    }

                    PromotionRecyclerViewAdapter rcAdapter = new PromotionRecyclerViewAdapter(getActivity(), allItems);
                    rView.setAdapter(rcAdapter);

                } else {
                    Toast toast;
                    toast = Toast.makeText(getContext(), "Empty json array", Toast.LENGTH_SHORT);
                    toast.show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private String getDate(String time) {
        String str = time.substring(0, 10);
        return str;
    }

}
