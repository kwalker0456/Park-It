package com.rocks.kevinwalker.parkit.spot;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rocks.kevinwalker.parkit.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SpotListingsAdapter extends RecyclerView.Adapter<SpotListingsAdapter.ViewHolder> {

    private final List<Spot> mValues;
    private final SpotListingsFragment.SpotListingsInteraction mListener;

    public SpotListingsAdapter(List<Spot> items, SpotListingsFragment.SpotListingsInteraction listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_spot_list_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        //TODO: Modify to speed up
        holder.mItem = mValues.get(position);
        holder.txt_payment_type.setText(mValues.get(position).getPaymentType().toString());
        holder.txt_cost.setText(String.valueOf(mValues.get(position).getHourlyRate()));
        holder.txt_distance.setText(String.valueOf(mValues.get(position).getSpotDistance()));
        holder.txt_spot_name.setText(mValues.get(position).getName());

        if (!mValues.get(position).getSpotPhotoURL().isEmpty()) {
            Picasso.get().load(String.valueOf(mValues.get(position).getSpotPhotoURL()))
                    .centerCrop()
                    .resize(316, 200)
                    .rotate(90)
                    .placeholder(R.drawable.parking_space)
                    .into(holder.spot_image);
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onSpotListingInteraction(holder.mItem);
                    Toast.makeText((Context)mListener, mValues.get(position).getName(), Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView txt_payment_type;
        public final TextView txt_distance;
        public final TextView txt_cost;
        public final TextView txt_spot_name;
        public final ImageView spot_image;
        public Spot mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            txt_payment_type = view.findViewById(R.id.txt_payment_type);
            txt_distance = view.findViewById(R.id.txt_distance);
            txt_cost = view.findViewById(R.id.txt_cost);
            txt_spot_name = view.findViewById(R.id.txt_spot_name);
            spot_image = view.findViewById(R.id.spot_image);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
