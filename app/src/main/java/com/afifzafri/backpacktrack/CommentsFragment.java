package com.afifzafri.backpacktrack;


import android.content.Context;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
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
public class CommentsFragment extends Fragment {

    // for swipe to refresh widget
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // define last page, need for API to load next page. Will be increase by each request
    private int lastPage = 1;

    // we need this variable to lock and unlock loading more
    // e.g we should not load more when volley is already loading,
    // loading will be activated when volley completes loading
    private boolean itShouldLoadMore = true;

    // initialize adapter and data structure here
    private ListCommentsAdapter mAdapter;
    // List for all data array
    private List<CommentsModel> commentsList;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;


    public CommentsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_comments, container, false);

        // get itinerary id
        final String itinerary_id = getArguments().getString("itinerary_id");
        final String itinerary_user_id = getArguments().getString("itinerary_user_id");

        // read from SharedPreferences
        final SharedPreferences sharedpreferences = getActivity().getSharedPreferences("logindata", Context.MODE_PRIVATE);
        final String access_token = sharedpreferences.getString("access_token", "");
        final String authUserId = sharedpreferences.getString("user_id", "");

        // get UI elements
        final FrameLayout loadingFrame = (FrameLayout) view.findViewById(R.id.loadingFrame);


        // ----------------- Get and list all comments -----------------
        // you must assign all objects to avoid nullPointerException
        commentsList = new ArrayList<>();
        mAdapter = new ListCommentsAdapter(commentsList, authUserId, itinerary_user_id, access_token);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.listComments);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // specify an adapter (see also next example)
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setFocusable(false);

        // create a function for the first load
        firstLoadData(view, itinerary_id, access_token);

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
                            loadMore(view, itinerary_id, access_token);
                        }
                    }

                }
            }
        });

        // ----------------- Post new comments -----------------
        final ImageButton sendBtn = (ImageButton) view.findViewById(R.id.sendBtn);
        final TextView editComment = (TextView) view.findViewById(R.id.editComment);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String newComment = editComment.getText().toString(); // get input
                if(newComment != null && !newComment.equals(""))
                {
                    sendBtn.setEnabled(false); // disable button
                    loadingFrame.setVisibility(View.VISIBLE);// show loading progress bar

                    JSONObject commentParams = new JSONObject(); // login parameters

                    try {
                        commentParams.put("message", newComment);
                        commentParams.put("itinerary_id", itinerary_id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Request a string response from the provided URL.
                    JsonObjectRequest createRequest = new JsonObjectRequest(Request.Method.POST, AppHelper.baseurl + "/api/newComment", commentParams,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {

                                    try {

                                        int code = Integer.parseInt(response.getString("code"));

                                        if(code == 200)
                                        {
                                            // parse JSON response
                                            String message = response.getString("message");
                                            if(isAdded()) {
                                                Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                                            }

                                            // empty all input
                                            editComment.setText("");

                                            // refresh fragment
                                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                                            ft.detach(CommentsFragment.this).attach(CommentsFragment.this).commit();
                                            lastPage = 1; // reset back current page to first page
                                        }
                                        else if(code == 400)
                                        {
                                            String errormsg = response.getString("message");

                                            // check if response contain errors messages
                                            if(response.has("error"))
                                            {
                                                JSONObject errors = response.getJSONObject("error");
                                                if(errors.has("message"))
                                                {
                                                    String err = errors.getJSONArray("message").getString(0);
                                                    editComment.setError(err);
                                                }
                                                if(errors.has("itinerary_id"))
                                                {
                                                    String err = errors.getJSONArray("itinerary_id").getString(0);
                                                    editComment.setError(err);
                                                }
                                            }

                                            if(isAdded()) {
                                                Toast.makeText(getActivity().getApplicationContext(), errormsg, Toast.LENGTH_SHORT).show();
                                            }
                                            sendBtn.setEnabled(true);
                                            loadingFrame.setVisibility(View.GONE);
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if(isAdded()) {
                                Toast.makeText(getActivity().getApplicationContext(), "Unable to post new comment! Please check your connection.", Toast.LENGTH_SHORT).show();
                            }

                            sendBtn.setEnabled(true);
                            loadingFrame.setVisibility(View.GONE);
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

                    // set new timeout, fix double request bug if network connection is slow
                    createRequest.setRetryPolicy(new DefaultRetryPolicy(
                            0,
                            -1,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                    // Add the request to the VolleySingleton.
                    VolleySingleton.getInstance(getActivity().getBaseContext()).addToRequestQueue(createRequest);
                }
                else
                {
                    editComment.setError("Write a comment");
                }

            }
        });

        // refresh fragment when perform swipe to refresh
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.detach(CommentsFragment.this).attach(CommentsFragment.this).commit();

                        mSwipeRefreshLayout.setRefreshing(false);

                        lastPage = 1; // reset back current page to first page
                    }
                }
        );

        return view;
    }

    private void firstLoadData(final View view, final String itinerary_id, final String access_token) {
        // get UI elements
        final FrameLayout loadingFrame = (FrameLayout) view.findViewById(R.id.loadingFrame);

        // show loading spinner
        loadingFrame.setVisibility(View.VISIBLE);

        itShouldLoadMore = false; // lock this guy,(itShouldLoadMore) to make sure,
        // user will not load more when volley is processing another request
        // only load more when  volley is free

        // Request a string response from the provided URL.
        JsonObjectRequest commentsListRequest = new JsonObjectRequest(Request.Method.GET, AppHelper.baseurl + "/api/listCommentsPaginated/"+itinerary_id, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // remember here we are in the main thread, that means,
                        //volley has finished processing request, and we have our response.
                        // What else are you waiting for? update itShouldLoadMore = true;
                        itShouldLoadMore = true;

                        try {
                            JSONArray comments = response.getJSONArray("data");

                            if (comments.length() <= 0) {
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

                            for(int i=0;i<comments.length();i++)
                            {
                                JSONObject comment = comments.getJSONObject(i);
                                String id = comment.getString("id");
                                String message = comment.getString("message");
                                String datetime = comment.getString("created_at");
                                JSONObject user = comment.getJSONObject("user");
                                String user_name = user.getString("name");
                                String user_id = user.getString("id");
                                String user_username = user.getString("username");
                                String user_avatar = user.getString("avatar_url");

                                // insert data into array
                                commentsList.add(new CommentsModel(id, user_name, user_username, user_id, user_avatar, message, datetime));

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
                    Toast.makeText(getActivity().getApplicationContext(), "Load comments Failed! Please check your connection.", Toast.LENGTH_SHORT).show();
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
        VolleySingleton.getInstance(getActivity().getBaseContext()).addToRequestQueue(commentsListRequest);

        lastPage++; // increment the page number
    }

    private void loadMore(View view, final String itinerary_id, final String access_token) {
        // get UI elements
        final ProgressBar loadMoreSpin = (ProgressBar) view.findViewById(R.id.loadMoreSpin);

        // show loading spinner
        loadMoreSpin.setVisibility(View.VISIBLE);

        itShouldLoadMore = false; // lock this guy,(itShouldLoadMore) to make sure,
        // user will not load more when volley is processing another request
        // only load more when  volley is free

        // Request a string response from the provided URL.
        JsonObjectRequest commentsListRequest = new JsonObjectRequest(Request.Method.GET, AppHelper.baseurl + "/api/listCommentsPaginated/"+itinerary_id+"?page="+lastPage, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // since volley has completed and it has our response, now let's update
                        // itShouldLoadMore
                        itShouldLoadMore = true;

                        try {
                            JSONArray comments = response.getJSONArray("data");

                            if (comments.length() <= 0) {
                                // we need to check this, to make sure, our dataStructure JSonArray contains
                                // something
                                // check if activity have been attach to the fragment
                                if(isAdded()) {
                                    Toast.makeText(getActivity().getApplicationContext(), "No more comments available", Toast.LENGTH_SHORT).show();
                                }
                                itShouldLoadMore = false;
                                loadMoreSpin.setVisibility(View.GONE);
                                return; // return will end the program at this point
                            }

                            for(int i=0;i<comments.length();i++)
                            {
                                JSONObject comment = comments.getJSONObject(i);
                                String id = comment.getString("id");
                                String message = comment.getString("message");
                                String datetime = comment.getString("created_at");
                                JSONObject user = comment.getJSONObject("user");
                                String user_name = user.getString("name");
                                String user_id = user.getString("id");
                                String user_username = user.getString("username");
                                String user_avatar = user.getString("avatar_url");

                                // insert data into array
                                commentsList.add(new CommentsModel(id, user_name, user_username, user_id, user_avatar, message, datetime));

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
                    Toast.makeText(getActivity().getApplicationContext(), "Load more comments failed! Please check your connection.", Toast.LENGTH_SHORT).show();
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
        VolleySingleton.getInstance(getActivity().getBaseContext()).addToRequestQueue(commentsListRequest);

        lastPage++; // increment the page number
    }

}
