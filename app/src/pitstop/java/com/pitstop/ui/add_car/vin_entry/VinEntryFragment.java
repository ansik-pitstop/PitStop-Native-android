package com.pitstop.ui.add_car.vin_entry;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.ui.add_car.FragmentSwitcher;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class VinEntryFragment extends Fragment implements VinEntryView{

    @BindView(R.id.add_vehicle)
    Button addVehicleButton;
    @BindView(R.id.VIN)
    EditText vinEditText;
    @BindView(R.id.mileage_input)
    EditText mileageEditText;

    private ViewGroup rootView;
    private VinEntryPresenter presenter;
    private MixpanelHelper mixpanelHelper;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext()))
                .build();

        mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());

        FragmentSwitcher fragmentSwitcher = (FragmentSwitcher)getActivity();

        presenter = new VinEntryPresenter(useCaseComponent,mixpanelHelper,fragmentSwitcher);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_vin_entry, container, false);
        ButterKnife.bind(this, rootView);
        presenter.subscribe(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        presenter.unsubscribe();
        super.onDestroyView();
    }

    @OnClick(R.id.scan_vin)
    protected void scanVinClicked(){

    }

    @OnClick(R.id.add_vehicle)
    protected void addVehicleClicked(){
        presenter.addVehicle(vinEditText.getText().toString());
    }

    @OnTextChanged(R.id.VIN)
    protected void onVinTextChanged(Editable editable){
        presenter.vinChanged(vinEditText.getText().toString());
    }

    @Override
    public void onValidVinInput() {
        addVehicleButton.setEnabled(true);
        addVehicleButton.setBackground(getResources()
                .getDrawable(R.drawable.color_button_rectangle_highlight));
    }

    @Override
    public void onInvalidVinInput() {
        addVehicleButton.setBackground(getResources().getDrawable(R.drawable.color_button_rectangle_grey));
        addVehicleButton.setEnabled(false);
    }

    @Override
    public int getMileage() {
        return Integer.valueOf(mileageEditText.getText().toString());
    }

    @Override
    public void onInvalidMileage() {
        AlertDialog invalidMileageDialog= new AnimatedDialogBuilder(getActivity())
                .setTitle("Invalid Mileage")
                .setMessage("Please input a mileage between 0 and 3,000,000")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("", null).create();
        invalidMileageDialog.show();
    }
}
