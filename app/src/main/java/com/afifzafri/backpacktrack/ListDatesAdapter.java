package com.afifzafri.backpacktrack;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ListDatesAdapter extends RecyclerView.Adapter<ListDatesAdapter.MyViewHolder> {

    private List<ItineraryDatesModel> allDataList;
    private Context mContext;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView activity_date;
        public RecyclerView activities_list;

        public MyViewHolder(View v) {
            super(v);
            activity_date = (TextView) v.findViewById(R.id.activity_date);
            this.activity_date = activity_date;

            activities_list = (RecyclerView) v.findViewById(R.id.activities_list);
            this.activities_list = activities_list;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ListDatesAdapter(Context mContext, List<ItineraryDatesModel> allDataList) {
        this.mContext = mContext;
        this.allDataList = allDataList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ListDatesAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
        // create a new view
        View v= LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_itinerary_dates, parent, false);
        MyViewHolder vh= new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        // Set itinerary title
        String actdate = new AppHelper().convertDate(allDataList.get(position).getDate());
        String datetitle = "Day " + (position+1) + " (" + actdate + ")";
        holder.activity_date.setText(datetitle);

        List activitiesList = allDataList.get(position).getActivitiesList();

        ListViewActivitiesAdapter itemListDataAdapter = new ListViewActivitiesAdapter(mContext, activitiesList);

        holder.activities_list.setHasFixedSize(true);
        holder.activities_list.setLayoutManager(new LinearLayoutManager(mContext));
        holder.activities_list.setAdapter(itemListDataAdapter);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return allDataList.size();
    }
}