package com.afifzafri.backpacktrack;


import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

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
public class CountryItinerariesFragment extends Fragment {

    private SearchView searchView = null;
    private SearchView.OnQueryTextListener queryTextListener;

    // for swipe to refresh widget
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // define last page, need for API to load next page. Will be increase by each request
    private int lastPage = 1;

    // we need this variable to lock and unlock loading more
    // e.g we should not load more when volley is already loading,
    // loading will be activated when volley completes loading
    private boolean itShouldLoadMore = true;

    // initialize adapter and data structure here
    private ListCountriesAdapter mAdapter;
    // Countries list Array
    private List<CountriesModel> countriesList;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager  mLayoutManager;


    public CountryItinerariesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_country_itineraries, container, false);

        // read from SharedPreferences
        final SharedPreferences sharedpreferences = getActivity().getSharedPreferences("logindata", Context.MODE_PRIVATE);
        final String access_token = sharedpreferences.getString("access_token", "");

        // you must assign all objects to avoid nullPointerException
        countriesList = new ArrayList<>();
        mAdapter = new ListCountriesAdapter(countriesList);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.countries_list);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // specify an adapter (see also next example)
        mRecyclerView.setAdapter(mAdapter);

        // create a function for the first load
        firstLoadData(view, access_token);

        // here add a recyclerView listener, to listen to scrolling,
        // we don't care when user scrolls upwards, will only be careful when user scrolls downwards
        // this listener is freely provided for by android, no external library
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            // for this tutorial, this is the ONLY method that we need, ignore the rest
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    // Recycle view scrolling downwards...
                    // this if statement detects when user reaches the end of recyclerView, this is only time we should load more
                    if (!recyclerView.canScrollVertically(RecyclerView.FOCUS_DOWN)) {
                        // remember "!" is the same as "== false"
                        // here we are now allowed to load more, but we need to be careful
                        // we must check if itShouldLoadMore variable is true [unlocked]
                        if (itShouldLoadMore) {
                            loadMore(view, access_token);
                        }
                    }

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

                        lastPage = 1; // reset back current page to first page
                    }
                }
        );

        return view;
    }

    private void firstLoadData(final View view, final String access_token) {
        // get UI elements
        final FrameLayout loadingFrame = (FrameLayout) view.findViewById(R.id.loadingFrame);

        // show loading spinner
        loadingFrame.setVisibility(View.VISIBLE);

        itShouldLoadMore = false; // lock this guy,(itShouldLoadMore) to make sure,
        // user will not load more when volley is processing another request
        // only load more when  volley is free

        // Request a string response from the provided URL.
        JsonObjectRequest countriesListRequest = new JsonObjectRequest(Request.Method.GET, AppHelper.baseurl + "/api/listVisitedCountries", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // remember here we are in the main thread, that means,
                        //volley has finished processing request, and we have our response.
                        // What else are you waiting for? update itShouldLoadMore = true;
                        itShouldLoadMore = true;

                        try {
                            JSONArray countries = response.getJSONArray("data");

                            if (countries.length() <= 0) {
                                // we need to check this, to make sure, our dataStructure JSonArray contains
                                // something
                                // check if activity have been attach to the fragment
                                if(isAdded()) {
                                    // if no data available, show background image to inform no data
                                    FrameLayout emptyStateFrame = (FrameLayout) view.findViewById(R.id.emptyStateFrame);
                                    emptyStateFrame.setVisibility(View.VISIBLE);
                                }
                                itShouldLoadMore = false;
                                loadingFrame.setVisibility(View.GONE);
                                return; // return will end the program at this point
                            }

                            for(int i=0;i<countries.length();i++)
                            {
                                JSONObject data = countries.getJSONObject(i);
                                String total_itineraries = data.getString("totalitineraries");
                                JSONObject country = data.getJSONObject("country");
                                String country_name = country.getString("name");
                                String country_code = country.getString("code");
                                String country_id = country.getString("id");

                                // insert data into array
                                countriesList.add(new CountriesModel(country_name, country_code, country_id, total_itineraries));

                                mAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        loadingFrame.setVisibility(View.GONE);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // check if activity have been attach to the fragment
                if(isAdded()) {
                    // show connection error icon and message
                    FrameLayout dcFrame = (FrameLayout) view.findViewById(R.id.dcFrame);
                    dcFrame.setVisibility(View.VISIBLE);
                    Toast.makeText(getActivity().getApplicationContext(), "Load Countries Failed! Please check your connection.", Toast.LENGTH_SHORT).show();
                }
                loadingFrame.setVisibility(View.GONE);
                itShouldLoadMore = true; // even if volley failed, set true so we can retry again
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
        VolleySingleton.getInstance(getActivity().getBaseContext()).addToRequestQueue(countriesListRequest);

        lastPage++; // increment the page number
    }

    private void loadMore(View view, final String access_token) {
        // get UI elements
        final ProgressBar loadMoreSpin = (ProgressBar) view.findViewById(R.id.loadMoreSpin);

        // show loading spinner
        loadMoreSpin.setVisibility(View.VISIBLE);

        itShouldLoadMore = false; // lock this guy,(itShouldLoadMore) to make sure,
        // user will not load more when volley is processing another request
        // only load more when  volley is free

        // Request a string response from the provided URL.
        JsonObjectRequest countriesListRequest = new JsonObjectRequest(Request.Method.GET, AppHelper.baseurl + "/api/listVisitedCountries?page="+lastPage, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // since volley has completed and it has our response, now let's update
                        // itShouldLoadMore
                        itShouldLoadMore = true;

                        try {
                            JSONArray countries = response.getJSONArray("data");

                            if (countries.length() <= 0) {
                                // we need to check this, to make sure, our dataStructure JSonArray contains
                                // something
                                // check if activity have been attach to the fragment
                                if(isAdded()) {
                                    Toast.makeText(getActivity().getApplicationContext(), "No more countries available", Toast.LENGTH_SHORT).show();
                                }
                                itShouldLoadMore = false;
                                loadMoreSpin.setVisibility(View.GONE);
                                return; // return will end the program at this point
                            }

                            for(int i=0;i<countries.length();i++)
                            {
                                JSONObject data = countries.getJSONObject(i);
                                String total_itineraries = data.getString("totalitineraries");
                                JSONObject country = data.getJSONObject("country");
                                String country_name = country.getString("name");
                                String country_code = country.getString("code");
                                String country_id = country.getString("id");

                                // insert data into array
                                countriesList.add(new CountriesModel(country_name, country_code, country_id, total_itineraries));

                                mAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        loadMoreSpin.setVisibility(View.GONE);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // check if activity have been attach to the fragment
                if(isAdded()) {
                    Toast.makeText(getActivity().getApplicationContext(), "Load more countries failed! Please check your connection.", Toast.LENGTH_SHORT).show();
                }
                loadMoreSpin.setVisibility(View.GONE);

                itShouldLoadMore = true; // even if volley failed, set true so we can retry again
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
        VolleySingleton.getInstance(getActivity().getBaseContext()).addToRequestQueue(countriesListRequest);

        lastPage++; // increment the page number
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.itineraries_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setQueryHint("Search itinerary title...");

            queryTextListener = new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    Log.i("onQueryTextChange", newText);

                    return true;
                }
                @Override
                public boolean onQueryTextSubmit(String query) {
                    Log.i("onQueryTextSubmit", query);

                    // when user submit search (enter), open search result page
                    // redirect to edit profile page
                    Intent intentPage = new Intent(getActivity(), SearchItinerariesActivity.class);
                    intentPage.putExtra("query", query);
                    startActivity(intentPage);

                    return true;
                }
            };
            searchView.setOnQueryTextListener(queryTextListener);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                // Not implemented here
                return false;
            default:
                break;
        }
        searchView.setOnQueryTextListener(queryTextListener);
        return super.onOptionsItemSelected(item);
    }

}
