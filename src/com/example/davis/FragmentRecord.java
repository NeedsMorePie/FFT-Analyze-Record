package com.example.davis;


import java.io.File;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class FragmentRecord extends Fragment {

    private String root_sd = Environment.getExternalStorageDirectory().getAbsolutePath();

    public FragmentRecord(){
    }
    
    /*
	public FragmentRecord(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	} */

	@Override
	 public View onCreateView(LayoutInflater inflater, ViewGroup container, 
		        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        	View rootView = inflater.inflate(R.layout.fragment_record, container, false);
        	
    		FrameLayout stopButton = (FrameLayout) rootView.findViewById(R.id.stop_button);
    		stopButton.setElevation(599);
        	
        	if(MainActivity.isPaused){
        		ImageView recordButton = (ImageView) rootView.findViewById(R.id.record_button);
            	recordButton.setImageResource(R.drawable.recordpause);
            	MainActivity.mChronometer = (Chronometer) rootView.findViewById(R.id.chronometer);
            	MainActivity.mChronometer.setBase(MainActivity.savedBase + SystemClock.elapsedRealtime() - MainActivity.lastTimePause);
        	} else {
                if (!MainActivity.mStartRecording){
                	ImageView recordButton = (ImageView) rootView.findViewById(R.id.record_button);
                	recordButton.setImageResource(R.drawable.recording);
                } 
        	} 
        	
			if (MainActivity.timePlaying){
				MainActivity.mChronometer = (Chronometer) rootView.findViewById(R.id.chronometer);
				MainActivity.mChronometer.setBase(MainActivity.masterChronometer.getBase() + SystemClock.elapsedRealtime() - MainActivity.lastTimeLoad);
				MainActivity.mChronometer.start();
			}

		        // If activity recreated (such as from screen rotate), restore
		        // the previous article selection set by onSaveInstanceState().
		        // This is primarily necessary when in the two-pane layout.
		        if (savedInstanceState != null) {
		            
		        }

		        return rootView;
		    }
	@Override
    public void onStart() {
        super.onStart();

        // During startup, check 

	}
	
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }
	 
}
