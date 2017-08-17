package com.pitstop.ui.vehicle_health_report.emissions_test_progress.emissions_report_view;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.florent37.viewanimator.AnimationListener;
import com.github.florent37.viewanimator.ViewAnimator;
import com.pitstop.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-08-17.
 */

public class EmissionsReportFragment extends Fragment implements EmissionsReportView {

    private EmissionsReportPresenter presenter;
    @BindView(R.id.emissions_report_cell_one)
    RelativeLayout cellOne;
    @BindView(R.id.emissions_report_cell_one_details)
    RelativeLayout cellOneDetails;
    @BindView(R.id.cell_one_text)
    TextView cellOneText;


    @BindView(R.id.emissions_report_cell_two)
    RelativeLayout cellTwo;
    @BindView(R.id.emissions_report_cell_two_details)
    RelativeLayout cellTwoDetails;

    @BindView(R.id.emissions_report_cell_three)
    RelativeLayout cellThree;
    @BindView(R.id.emissions_report_cell_three_details)
    RelativeLayout cellThreeDetails;

    @BindView(R.id.emissions_report_cell_four)
    RelativeLayout cellFour;
    @BindView(R.id.emissions_report_cell_four_details)
    RelativeLayout cellFourDetails;

    @BindView(R.id.emissions_report_cell_five)
    RelativeLayout cellFive;
    @BindView(R.id.emissions_report_cell_five_details)
    RelativeLayout cellFiveDetails;

    @BindView(R.id.emissions_report_cell_six)
    RelativeLayout cellSix;
    @BindView(R.id.emissions_report_cell_six_details)
    RelativeLayout cellSixDetails;



    private Context context;

    private boolean dropDownInProgress;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        View view = inflater.inflate(R.layout.fragment_emissions_report,container,false);
        dropDownInProgress = false;
        presenter = new EmissionsReportPresenter();
        ButterKnife.bind(this,view);
        cellOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onCellClicked(cellOneDetails);
            }
        });
        cellTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onCellClicked(cellTwoDetails);
            }
        });
        cellThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onCellClicked(cellThreeDetails);
            }
        });
        cellFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onCellClicked(cellFourDetails);
            }
        });
        cellFive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onCellClicked(cellFiveDetails);
            }
        });
        cellSix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onCellClicked(cellSixDetails);
            }
        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscirebe(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void toggleCellDetails(View cell) {
        if(dropDownInProgress){return;}
        if(cell.getHeight() == 0){//open
            ViewAnimator.animate(cell)
                    .onStart(new AnimationListener.Start() {
                        @Override
                        public void onStart() {
                            dropDownInProgress = true;
                        }
                    })
                    .onStop(new AnimationListener.Stop() {
                        @Override
                        public void onStop() {
                            dropDownInProgress = false;
                        }
                    })
                    .dp().height(0,100)
                    .duration(200)
                    .start();
        }else{//close
            ViewAnimator.animate(cell)
                    .onStart(new AnimationListener.Start() {
                        @Override
                        public void onStart() {
                            dropDownInProgress = true;
                        }
                    })
                    .onStop(new AnimationListener.Stop() {
                        @Override
                        public void onStop() {
                            dropDownInProgress = false;
                        }
                    })
                    .dp().height(100,0)
                    .duration(200)
                    .start();
        }
    }
}
