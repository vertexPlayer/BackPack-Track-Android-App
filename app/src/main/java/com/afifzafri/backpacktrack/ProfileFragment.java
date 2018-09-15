package com.afifzafri.backpacktrack;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // get UI elements
        final TextView textName = (TextView) view.findViewById(R.id.textName);
        final TextView textUsername = (TextView) view.findViewById(R.id.textUsername);
        final TextView textCountry = (TextView) view.findViewById(R.id.textCountry);
        final TextView textEmail = (TextView) view.findViewById(R.id.textEmail);
        final TextView textPhone = (TextView) view.findViewById(R.id.textPhone);
        final ImageView avatar_pic = (ImageView) view.findViewById(R.id.avatar_pic);

        // read from SharedPreferences
        final SharedPreferences sharedpreferences = getActivity().getSharedPreferences("logindata", Context.MODE_PRIVATE);
        final String access_token = sharedpreferences.getString("access_token", "");

        // Instantiate the RequestQueue.
        RequestQueue profileQueue = Volley.newRequestQueue(getActivity().getApplicationContext());

        // Request a string response from the provided URL.
        JsonObjectRequest profileRequest = new JsonObjectRequest(Request.Method.GET, AppConstants.baseurl + "/api/user", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            // parse JSON response
                            int id = response.getInt("id");
                            String name = response.getString("name");
                            String username = response.getString("username");
                            String phone = response.getString("phone");
                            String address = response.getString("address");
                            String email = response.getString("email");
                            String avatar_url = response.getString("avatar_url");
                            JSONObject country = response.getJSONObject("country");
                            String country_name = country.getString("name");

                            // set values to the elements
                            textName.setText(name);
                            textUsername.setText("@"+username);
                            textPhone.setText(phone);
                            textEmail.setText(email);
                            textCountry.setText(country_name);
                            // set avatar image using Picasso library
                            if(avatar_url != null && !avatar_url.isEmpty() && avatar_url != "null") {
                                Picasso.get().load(avatar_url).into(avatar_pic);
                            }

                            Toast.makeText(getActivity().getApplicationContext(), "Profile data loaded!", Toast.LENGTH_SHORT).show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity().getApplicationContext(), "Profile data not loaded!", Toast.LENGTH_SHORT).show();
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

        // Add the request to the RequestQueue.
        profileQueue.add(profileRequest);

        return view;
    }

}