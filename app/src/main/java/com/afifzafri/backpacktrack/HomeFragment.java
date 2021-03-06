package com.afifzafri.backpacktrack;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    // for swipe to refresh widget
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // initialize adapter and data structure here
    private ListTopContributorsAdapter contributorsAdapter;
    private ListPopularCountriesAdapter countriesAdapter;
    private ListPopularItinerariesAdapter itinerariesAdapter;
    // list Array
    private List<UsersModel> contributorsList;
    private List<CountriesModel> countriesList;
    private List<PopularItinerariesModel> itinerariesList;

    // Recyclerviews
    private RecyclerView contributorsRecycler;
    private RecyclerView countriesRecycler;
    private RecyclerView itinerariesRecycler;
    private LinearLayoutManager mLayoutManager;
    private SpanningLinearLayoutManager spanLayoutManager;


    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_home, container, false);

        // read from SharedPreferences
        final SharedPreferences sharedpreferences = getActivity().getSharedPreferences("logindata", Context.MODE_PRIVATE);
        final String access_token = sharedpreferences.getString("access_token", "");

        // ----- List Top Contributors -----
        contributorsList = new ArrayList<>();
        contributorsAdapter = new ListTopContributorsAdapter(contributorsList);

        contributorsRecycler = (RecyclerView) view.findViewById(R.id.listTopContributors);
        // use a linear layout manager
        spanLayoutManager = new SpanningLinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        contributorsRecycler.setLayoutManager(spanLayoutManager);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        contributorsRecycler.setHasFixedSize(true);
        contributorsRecycler.setNestedScrollingEnabled(false);

        // specify an adapter (see also next example)
        contributorsRecycler.setAdapter(contributorsAdapter);

        // create a function for the load user's popular itineraries list
        loadTopContributors(access_token, view);


        // ----- List Popular Countries -----
        countriesList = new ArrayList<>();
        countriesAdapter = new ListPopularCountriesAdapter(countriesList);

        countriesRecycler = (RecyclerView) view.findViewById(R.id.listPopularCountries);
        // use a linear layout manager
        spanLayoutManager = new SpanningLinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        countriesRecycler.setLayoutManager(spanLayoutManager);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        countriesRecycler.setHasFixedSize(true);
        countriesRecycler.setNestedScrollingEnabled(false);

        // specify an adapter (see also next example)
        countriesRecycler.setAdapter(countriesAdapter);

        // create a function for the load user's popular itineraries list
        loadPopularCountries(access_token, view);


        // ----- List Popular Itineraries -----
        itinerariesList = new ArrayList<>();
        itinerariesAdapter = new ListPopularItinerariesAdapter(itinerariesList);

        itinerariesRecycler = (RecyclerView) view.findViewById(R.id.listPopularItineraries);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        itinerariesRecycler.setLayoutManager(mLayoutManager);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        itinerariesRecycler.setHasFixedSize(true);
        itinerariesRecycler.setNestedScrollingEnabled(false);

        // specify an adapter (see also next example)
        itinerariesRecycler.setAdapter(itinerariesAdapter);

        // create a function for the load user's popular itineraries list
        loadPopularItineraries(access_token, view);

        // ----- Launch webview widgets activity -----
        ImageButton currencyBtn = (ImageButton) view.findViewById(R.id.currencyBtn);
        currencyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAdded()) {
                    Intent intentPage = new Intent(getActivity(), WebviewWidgetActivity.class);
                    intentPage.putExtra("title", "Currency Converter");
                    intentPage.putExtra("widget", "currencyconverter");
                    startActivity(intentPage);
                }
            }
        });

        ImageButton weatherBtn = (ImageButton) view.findViewById(R.id.weatherBtn);
        weatherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAdded()) {
                    Intent intentPage = new Intent(getActivity(), WebviewWidgetActivity.class);
                    intentPage.putExtra("title", "Weather Forecast");
                    intentPage.putExtra("widget", "weatherforecast");
                    startActivity(intentPage);
                }
            }
        });

        // refresh fragment when perform swipe to refresh
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.detach(currentFragment).attach(currentFragment).commit();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }
        );

        return view;
    }

    private void loadTopContributors(final String access_token, View view) {
        // get UI elements
        final FrameLayout loadUserFrame = (FrameLayout) view.findViewById(R.id.loadUserFrame);

        // show loading spinner
        loadUserFrame.setVisibility(View.VISIBLE);

        // Request a string response from the provided URL.
        JsonArrayRequest popularListRequest = new JsonArrayRequest(Request.Method.GET, AppHelper.baseurl + "/api/listTopContributors", null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        try {

                            for(int i=0;i<response.length();i++)
                            {
                                JSONObject popular = response.getJSONObject(i);
                                String total_itineraries = popular.getString("totalitineraries");
                                JSONObject user = popular.getJSONObject("user");
                                String user_id = user.getString("id");
                                String user_name = user.getString("name");
                                String user_username = user.getString("username");
                                String user_country = user.getString("country_name");
                                String user_avatar = user.getString("avatar_url");
                                JSONObject rankdata = popular.getJSONObject("rank");
                                String user_rank = rankdata.getString("rank");
                                String user_badge = rankdata.getString("badge");

                                // insert data into array
                                contributorsList.add(new UsersModel(user_id, user_name, user_username, user_country, user_avatar, total_itineraries, user_rank, user_badge));

                                contributorsAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        loadUserFrame.setVisibility(View.GONE);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(isAdded()) {
                    Toast.makeText(getActivity().getBaseContext(), "Load top contributors Failed! Please check your connection.", Toast.LENGTH_SHORT).show();
                }
                loadUserFrame.setVisibility(View.GONE);
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Authorization", "Bearer "+access_token);

                return params;
            }
        };

        // Add the request to the VolleySingleton.
        VolleySingleton.getInstance(getActivity().getBaseContext()).addToRequestQueue(popularListRequest);
    }

    private void loadPopularCountries(final String access_token, View view) {
        // get UI elements
        final FrameLayout loadCountriesFrame = (FrameLayout) view.findViewById(R.id.loadCountriesFrame);

        // show loading spinner
        loadCountriesFrame.setVisibility(View.VISIBLE);

        // Request a string response from the provided URL.
        JsonArrayRequest popularListRequest = new JsonArrayRequest(Request.Method.GET, AppHelper.baseurl + "/api/listPopularCountries", null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        try {

                            for(int i=0;i<response.length();i++)
                            {
                                JSONObject popular = response.getJSONObject(i);
                                String total_itineraries = popular.getString("totalitineraries");
                                JSONObject country = popular.getJSONObject("country");
                                String country_name = country.getString("name");
                                String country_code = country.getString("code");
                                String country_id = country.getString("id");

                                // insert data into array
                                countriesList.add(new CountriesModel(country_name, country_code, country_id, total_itineraries));

                                countriesAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        loadCountriesFrame.setVisibility(View.GONE);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(isAdded()) {
                    Toast.makeText(getActivity().getBaseContext(), "Load popular countries Failed! Please check your connection.", Toast.LENGTH_SHORT).show();
                }
                loadCountriesFrame.setVisibility(View.GONE);
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Authorization", "Bearer "+access_token);

                return params;
            }
        };

        // Add the request to the VolleySingleton.
        VolleySingleton.getInstance(getActivity().getBaseContext()).addToRequestQueue(popularListRequest);
    }

    private void loadPopularItineraries(final String access_token, View view) {
        // get UI elements
        final FrameLayout loadItinerariesFrame = (FrameLayout) view.findViewById(R.id.loadItinerariesFrame);

        // show loading spinner
        loadItinerariesFrame.setVisibility(View.VISIBLE);

        // Request a string response from the provided URL.
        JsonArrayRequest popularListRequest = new JsonArrayRequest(Request.Method.GET, AppHelper.baseurl + "/api/listPopularItineraries", null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        try {

                            for(int i=0;i<response.length();i++)
                            {
                                JSONObject itinerary = response.getJSONObject(i);
                                String itinerary_id = itinerary.getString("id");
                                String itinerary_title = itinerary.getString("title");
                                JSONObject country = itinerary.getJSONObject("country");
                                String itinerary_country = country.getString("name");
                                JSONObject user = itinerary.getJSONObject("user");
                                String itinerary_poster_id = user.getString("id");
                                String itinerary_poster_name = user.getString("name");
                                String totallikes = itinerary.getString("totallikes");

                                // insert data into array
                                itinerariesList.add(new PopularItinerariesModel(itinerary_id, itinerary_title, itinerary_country, itinerary_poster_id, itinerary_poster_name, totallikes));

                                itinerariesAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        loadItinerariesFrame.setVisibility(View.GONE);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(isAdded()) {
                    Toast.makeText(getActivity().getBaseContext(), "Load popular itineraries Failed! Please check your connection.", Toast.LENGTH_SHORT).show();
                }
                loadItinerariesFrame.setVisibility(View.GONE);
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Authorization", "Bearer "+access_token);

                return params;
            }
        };

        // Add the request to the VolleySingleton.
        VolleySingleton.getInstance(getActivity().getBaseContext()).addToRequestQueue(popularListRequest);
    }

}
