package com.rocks.kevinwalker.parkit.vehicle;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rocks.kevinwalker.parkit.NavDrawer;
import com.rocks.kevinwalker.parkit.R;
import com.rocks.kevinwalker.parkit.profiles.ParentProfileFragment;
import com.rocks.kevinwalker.parkit.utils.FirestoreHelper;

import java.util.ArrayList;
import butterknife.BindView;
import butterknife.ButterKnife;

public class VehicleListingFragment extends ParentProfileFragment {

    private static final String ARG_COLUMN_COUNT = "column-count";

    private View mView;

    @BindView(R.id.vehicle_recycler_view) RecyclerView vehicleRecyclerView;

    private int mColumnCount = 1;

    private VehicleListingsInteraction mListener;
    private VehicleListingsAdapter vehicleListingsAdapter;
    private ArrayList<Vehicle> vehicles = new ArrayList<>();

    public VehicleListingFragment () {}

    @SuppressWarnings("unused")
    public static VehicleListingFragment newInstance(int columnCount) {
        VehicleListingFragment fragment = new VehicleListingFragment();
        ArrayList<Vehicle> vehicles = new ArrayList<>();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof VehicleListingsInteraction) {
            mListener = (VehicleListingsInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement VehicleListingsInteraction");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
        if (vehicles.isEmpty()) {
            vehicles.add(FirestoreHelper.getInstance().getUserVehicle());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_vehicle_list, container, false);

        ButterKnife.bind(this, mView);

        FirestoreHelper.getInstance().getAllVehicles();

        // Set the adapter
        if (mView instanceof RecyclerView) {
            Context context = mView.getContext();
            vehicleRecyclerView = (RecyclerView) mView;
            if (mColumnCount <= 1) {
                vehicleRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                vehicleRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            //resetRecyclerView();
        }

        return mView;
    }

    @Override
    public void onResume(){
        super.onResume();

        mListener.setVehicleFabVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mListener.setVehicleFabVisibility(View.GONE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        getActivity().setTitle(getResources().getString(R.string.vehicles_nav_title));
    }

    public void resetRecyclerView() {
        vehicleListingsAdapter = new VehicleListingsAdapter(FirestoreHelper.getInstance().getAllVehicles(), mListener);
        vehicleRecyclerView.setAdapter(vehicleListingsAdapter);
    }

    public void resetRecyclerView(ArrayList<Vehicle> vehicles) {
        vehicleListingsAdapter = new VehicleListingsAdapter(vehicles, mListener);
        vehicleRecyclerView.setAdapter(vehicleListingsAdapter);
    }


    public interface VehicleListingsInteraction {
        void onVehicleListingInteraction(Vehicle item);
        void setVehicleFabVisibility(int viewVisibilityConstant);
    }

}
