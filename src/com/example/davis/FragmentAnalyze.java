package com.example.davis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ca.uol.aig.fftpack.RealDoubleFFT;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentAnalyze extends Fragment {
	
	private static  String root_sd = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String TAG = FragmentAnalyze.class.getName();
    private String FILENAME;
    int timeCount=0;
    Timer T;
	
    public static final String ARG_OBJECT = "object";

	private RealDoubleFFT transformer;

	static Visualizer visualizer = new Visualizer(0);
    static boolean started = false;
    static FrameLayout analyseButton;
	
	int orientation = 1;
    int frequency = 44100;
	int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int blockSize = 2048;
    int blackKeyPressedColor = 0xFF343434;
    int whiteKeyPressedColor = 0x44222222;
    int blackKeyWidth;
    int previousNoteIndex;
    int[] maxIndex = {1,4,9};
    int[] primaryFreq = new int[3];
    int[] topFreq = new int[3];
    
    boolean[] getNew = {true,true,true};
    boolean[] hasDecreased = {false,false,false};
    
    float blackKeyHeightRatio;
    
    double imageWidth;
    double imageHeight;
    double widthTemp;
    double multiply;
    double avgAmp = 0;
    double tempAvg = 0;
    double threshold = 1;
    double[] primaryMax = new double[3];
    double[] max = new double[3];
    
    String[] note= new String[3];
    String[] previousNote = new String[3];
    String[] currentNote = new String[3];

    ArrayList<Integer> storedMaxFreq = new  ArrayList<Integer>();
    ArrayList<String> previousNotes = new ArrayList<String>();

	VariableNoteFinder noteFinder;
    Button startStopButton;
    RecordAudio recordTask;
    
    ImageView pianoImageWhites;
    ImageView pianoImageBlacks;
    ImageView pianoLines;
    ImageView imageView;
    
    Bitmap pianoBitmapWhites;
    Bitmap pianoBitmapBlacks;
    Bitmap linesBitmap;
    Bitmap bitmap;
    
    Canvas pianoCanvasWhites;
    Canvas pianoCanvasBlacks;
    Canvas linesCanvas;
    Canvas canvas;
    
    Paint pianoPaintWhites;
    Paint pianoPaintBlacks;
    Paint linesPaint;
    Paint paint;

    TextView frequencyDisplay;
    TextView frequencyDisplay2;
    TextView frequencyDisplay3;
    TextView analyseButtonText;
    TextView mediaControlHelp;
    
    FrameLayout backgroundNoiseButton;
    FrameLayout textBackground;
	
	public FragmentAnalyze() {
		
	}
	
	@Override
	 public View onCreateView(LayoutInflater inflater, ViewGroup container, 
		        Bundle savedInstanceState) {
		
		        View rootView = inflater.inflate(R.layout.fragment_analyze, container, false);
		        
		        // NEW DIRECTORY
				File soundDirectory = new File(root_sd+"/analyzed/");
				soundDirectory.mkdirs();
				setFileName();
		        
		        // DIMENSIONS
		        int buttonHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,  (float) 60, getResources().getDisplayMetrics());
				FrameLayout frameLayout = (FrameLayout) getActivity().findViewById(R.id.mainframe);
		        imageWidth = frameLayout.getWidth();
		        imageHeight = frameLayout.getHeight()-buttonHeight;
		        widthTemp = frameLayout.getWidth();
		        multiply = imageWidth/200;
		        
		        // DEFINING
		        noteFinder = new VariableNoteFinder(MainActivity.calibrationValue);
		        analyseButton = (FrameLayout) rootView.findViewById(R.id.analyze_toggle_button);
		        backgroundNoiseButton = (FrameLayout) rootView.findViewById(R.id.background_leves_button);
		        transformer = new RealDoubleFFT(blockSize);
		        textBackground = (FrameLayout) rootView.findViewById(R.id.text_background);
		        mediaControlHelp = (TextView) rootView.findViewById(R.id.media_control_help);
		        
		        // DRAWINGS
		        imageView = (ImageView) rootView.findViewById(R.id.visualization);
		        pianoImageWhites = (ImageView) rootView.findViewById(R.id.blank_whites);
		        pianoImageBlacks = (ImageView) rootView.findViewById(R.id.blank_blacks);
		        pianoLines = (ImageView) rootView.findViewById(R.id.keyboard_lines);
		        frequencyDisplay = (TextView) rootView.findViewById(R.id.frequency_display);
		        frequencyDisplay2 = (TextView) rootView.findViewById(R.id.frequency_display2);
		        frequencyDisplay3 = (TextView) rootView.findViewById(R.id.frequency_display3);
		        analyseButtonText = (TextView) rootView.findViewById(R.id.analyze_text);
		        
		        drawKeyboard();
		        
		        pianoBitmapWhites = Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config.ARGB_8888);
		        pianoCanvasWhites = new Canvas(pianoBitmapWhites);
		        pianoPaintWhites = new Paint();
		        pianoImageWhites.setImageBitmap(pianoBitmapWhites);
		        
		        pianoBitmapBlacks = Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config.ARGB_8888);
		        pianoCanvasBlacks = new Canvas(pianoBitmapBlacks);
		        pianoPaintBlacks = new Paint();
		        pianoImageBlacks.setImageBitmap(pianoBitmapBlacks);

		        // ONCLICK LISTENERS
		        analyseButton.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View v) {
		            	toggleAnalyse(v);
		            }
		        }); 
		        
		        backgroundNoiseButton.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View v) {
		            	if (started) {
		            		threshold = (double) max[0]*7;
	                	} else {
			            	Toast holdButton = Toast.makeText(getActivity().getApplicationContext(), "Analysis must be started", Toast.LENGTH_SHORT);
			        		holdButton.show();
	                	}
		            }
		        }); 
		        
		        if (!MainActivity.analyzeMode){
			        textBackground.setOnClickListener(new View.OnClickListener() {
			            @Override
			            public void onClick(View v) {
			            	if (MainActivity.lastSelectedName != null){
				            	MainActivity.mediaController.setAnchorView(imageView);
				            	MainActivity.mediaController.show();
			            	} else {
			            		Toast noFile = Toast.makeText(getActivity().getApplicationContext(), "No media file is selected", Toast.LENGTH_SHORT);
			            		noFile.show();
			            	}
			            }
			        }); 
		        }
		        
		        // ANALYZE MODE
		        if (MainActivity.analyzeMode){
			        getActivity().setTitle("Analyze From Microphone");
			        mediaControlHelp.setVisibility(View.INVISIBLE);
			        backgroundNoiseButton.setVisibility(View.VISIBLE);
		        } else {
		        	getActivity().setTitle("Analyze From System Output");
		        	mediaControlHelp.setVisibility(View.VISIBLE);
		        	backgroundNoiseButton.setVisibility(View.GONE);
		        	LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f);
		        }
		        
		        return rootView;
		    }
	
	/**
	 * Turns analyzing on and off
	 * -- makes new canvas (and visualizer)
	 */
    public void toggleAnalyse(View v) {
        if (started) {
            if (MainActivity.autoSave) {
            	T.cancel();
            }
            timeCount = 0;
            started = false;
            analyseButtonText.setText("Start Analyzing");
            recordTask.cancel(true);
            bitmap = Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            imageView.setImageBitmap(bitmap);
        	canvas.drawColor(0xFF444444);
            imageView.invalidate();
            note[0] = ""; // resets note
            if (!MainActivity.analyzeMode){
            	visualizer.release();
            }
        } else {
        	timeCount = 0;
        	
        	if (MainActivity.autoSave) {
            	T = new Timer();
            	T.scheduleAtFixedRate(new TimerTask() {         
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                timeCount++;                
                            }
                        });
                    }
                }, 10, 10);
            	setFileName();
            	writeToFile("", false);
        	}
        	
        	drawVisualization(imageView, "start");
            if (!MainActivity.analyzeMode){
            	visualizer = new Visualizer(0);
            }
        }
	}
    
    /**
     * writes a piece of input data to a file
     */
    private void writeToFile(String data, boolean appendMode) {
    	if (MainActivity.autoSave) {
            try {
            	FileOutputStream stream = new FileOutputStream(FILENAME, appendMode);
            	try {
            	    stream.write(data.getBytes());
            	} finally {
            	    stream.close();
            	}
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            } 
    	}
    }
    
    /**
     * sets FILENAME based on date and time
     */
    private void setFileName() {
    	Date date = new Date();
    	CharSequence formattedDate = DateFormat.format("yyyy-MM-dd | hh:mm:ss a", date.getTime());
    	FILENAME = root_sd+"/analyzed/"+formattedDate.toString()+".txt";
    }
    
    /**
     * sets orientation variable to 0 for portrait and 1 for landscape
     */
    public void getOrientation() { 
    	if (MainActivity.needOrientationCheck){
        	int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            switch (rotation) {
            case Surface.ROTATION_0:
            	orientation = 0;
            	break;
            case Surface.ROTATION_90:
            	orientation = 1;
            	break;
            case Surface.ROTATION_180:
            	orientation = 0;
            	break;
            case Surface.ROTATION_270:
            	orientation = 1;
            	break;
            default:
            	break;
            }
    	}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      
      final FrameLayout frameLayout = (FrameLayout) getActivity().findViewById(R.id.framelayout);
      ViewTreeObserver observer = frameLayout.getViewTreeObserver();
      
      // Listen for rotation change
      observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
	      @Override
	      public void onGlobalLayout() {
		      if (started) {
		          drawVisualization(frameLayout, "start");
		          refreshKeyboard();
		          drawKeyboard();
		      } else {
		    	  drawVisualization(frameLayout, "");
		    	  refreshKeyboard();
		    	  drawKeyboard();
		      }
	          frameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
	      }
      });
    }
    
    /**
     * Draws (and starts) the FFT visualizer
     */
    public void drawVisualization(View v, String start){
    	getNewDimensions(v);
        bitmap = Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((int)multiply*5);
        paint.setColor(0xFF0099CC);
        imageView.setImageBitmap(bitmap);
        if (start=="start"){
            started = true;
            analyseButtonText.setText("Stop Analyzing");
            recordTask = new RecordAudio();
            recordTask.execute();
        }
    }
    
    public void getNewDimensions (View v){
        imageWidth = v.getWidth();
        imageHeight = v.getHeight();
        widthTemp = v.getWidth();
        multiply = imageWidth/200;
    }
    
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
        if (started){
        	recordTask.cancel(true);
        }
        started = false;
        super.onSaveInstanceState(savedInstanceState);
    	}
	
	@Override
	public void onPause(){
        if (started){
        	recordTask.cancel(true);
        }
        started = false;
        analyseButtonText.setText("Start Analyzing");
        if (!MainActivity.analyzeMode){
        	visualizer.release();
        }
		super.onPause();
	}
	
	@Override
    public void onStart() {
        super.onStart();
    }
	
	/**
	 * Loops through FFT results (index 15-250) to find the index with the highest amplitude
	 * Uses index to find frequency (Frequency = I*(SamplingRate/SampleSize))
	 */
    public void getFrequency(double[] toTransform, int p,int n, int m){
        if (getNew[p] == true){
            max[p] = toTransform[0];
            if (MainActivity.analyzeMode) {
                for(int i = 15; i < 250; i++) { // int i=15; i < toTransform.length
                    if (toTransform[i] > max[p] &&
                    		i != maxIndex[n] && i != maxIndex[m] && i != 0) { // && i>15 && i<250
                        max[p] = toTransform[i];
                        maxIndex[p] = i;
                    }
                }
            } else {
                for(int i = 25; i < 300; i++) { // int i=15; i < toTransform.length
                    if (toTransform[i] > max[p] &&
                    		i != maxIndex[n] && i != maxIndex[m] && i != 0) { // && i>15 && i<250
                        max[p] = toTransform[i];
                        maxIndex[p] = i;
                    }
                }
            }
            if (MainActivity.analyzeMode){
            	topFreq[p] = (int) Math.round((double)(maxIndex[p]*frequency/blockSize));
            } else {
            	topFreq[p] = maxIndex[p]* 12;
            }
        } else {
        	max[p] = toTransform[maxIndex[p]];
        }
    }
    
    /**
     * use this method to get a hold on max amplitudes -- returns true when it's best to update the display
     */
    public boolean needUpdate(int p, int m, int n, TextView display){
    	if (max[p] > threshold){ //threshold
            if ((double)max[p] > ((double)primaryMax[p]+0.5)) {
            	if (MainActivity.analyzeMode) {
                	if (max[p] > threshold*1.2){
                		getNew[0] = false;
                	}
            	}
            	primaryMax[p] = max[p];
            	primaryFreq[p] = topFreq[p];
            	return false;
            } else if ((double)max[p] < (double)(primaryMax[p]-0.5)) {
            	note[p] = noteFinder.getNote(primaryFreq[p]);
        		//display.setText(Integer.toString(primaryFreq[p])+" Hz - "+note[p]);
            	//setPianoKey();
        		getNew[0] = true;
        		primaryMax[p] = 0;
        		return true;
            } else { return false; }
    	} else {
    		getNew[0] = true;
    		primaryMax[p] = 0;
    		return false;
    	}
    }
    
    /**
     * First, fill up the storedMaxFreq with values
     * Once filled, get a new value every time this function runs
     * -- move all the values down a slot
     * -- put in a new value, and remove the oldest value
     * Loop through storedValueAmounts, a 2D array, within each loop iteration of storedMaxFreq in order to fill the 2D array ([frequency value][number of occurrences])
     * -- check if the slot has duplicate values. if so, add 1 to the second dimension of that slot
     * -- otherwise, check if slot is empty. if so, fill it in with the new storedMaxFreq value
     * Using the data in storedMaxFreq, find a frequency to display
     * -- Loop to sum up and average all the values in storedMaxFreq
     * -- Loop to compare this average with each value in storedMaxFreq. take the value closest to the average (least difference between it and the average)
     * -- Loop through storedValueAmounts to sum up recurrent frequencies (mostCommonSum) and their weight (amountOfCommonSums)
     * -- -- sum up the frequencies that occur more than once (storedValueAmounts[i][1] > 0)
     * -- -- then, amountOfCommonSums += (storedValueAmounts[i][1]+1) to add weight
     * -- -- otherwise, add all unpreferred frequencies to the array list: unpreferredFrequencies 
     * -- Loop to remove bad values from the recurrent frequencies that we just added
     * -- -- if statement ensures that the already selected "most usable" frequency isn't accounted for when removing bad frequencies
     * -- -- if a value is outside a 21% margin of the most usable frequency, subtract it and its weight
     * -- Loop through unpreferredFrequencies (frequencies that only occur once) to find decently valid values
     * -- -- if an unpreferred value is still within a 10% margin of the most usable frequency, add it and weight it as 1
     * -- Finally, divide the total sum by the weights (mostCommonSum/amountOfCommonSums) to get an average that represents the most accurate frequency
     * -- -- return 0 if amountOfCommonSums > 0 to avoid accidentally dividing by 0
     * Update visualization display
     * -- Get note based on the returned frequency
     * -- Fill previousNotes (an ArrayList) with the previous 3 notes using the same method as step 1 and 2 of this function
     * -- Loop through previousNotes to check if all notes are the same or not
     * -- -- if not all the same, changeNote will be false. vice versa
     * -- If changeNote is true:
     * -- -- update previousNote to be currentNote, then update currentNote to the actual currentNote (previousNote is always one step behind)
     * -- -- if currentNote != previousNote, meaning there is a need to update the display, then update!
     */
    public void updateText(int p, int m, int n, TextView display) {
    	if (max[p] > threshold){ //threshold
        	if (storedMaxFreq.size() < 6) { // fills the array
        		storedMaxFreq.add(topFreq[p]);
        	} else { // if storedMaxFreq is full
        		
        		// DEFINITIONS
        		int mostCommonSum = 0;
        		int amountOfCommonSums = 0;
        		boolean changeNote = true;
        		int[][] storedValueAmounts = new int[8][2];
        		
        		ArrayList<Integer> preferredFrequencies = new ArrayList<Integer>();
        		int mostUsable = 0;
        		int overallAverage = 0;
        		ArrayList<Integer> unpreferredFrequencies = new ArrayList<Integer>();
        		
        		// UPDATES ARRAY
        		ArrayList<Integer> tempStorage = new ArrayList<Integer>();
        		tempStorage.addAll(storedMaxFreq);
        		storedMaxFreq.clear();
        		storedMaxFreq.add(0, topFreq[p]);
        		storedMaxFreq.addAll(1, tempStorage);
        		storedMaxFreq.remove(6);
        		
        		// FINDS RECURRING FREQUENCIES
        		for (int i=0; i<storedMaxFreq.size(); i++) { // loop through the storedMaxFreq
        			for (int x=0; x<storedValueAmounts.length; x++){ // loop to check for empty slots in storedValueAmounts and fill them
        				if (storedValueAmounts[x][0] != 0 && storedMaxFreq.get(i) == storedValueAmounts[x][0]) { // if slot is filled and contents are the same, just increase the count by 1
        					storedValueAmounts[x][1] += 1;
        					break;
        				} else if (storedValueAmounts[x][0] == 0) { // if slot is empty, take it
        					storedValueAmounts[x][0] = storedMaxFreq.get(i);
                			break;
        				}
        			}
        		}

        		// GETS FREQUENCY TO DISPLAY
        		for (int i=0; i<storedMaxFreq.size(); i++) { // gets overall average
        			overallAverage += storedMaxFreq.get(i);
        		}
        		overallAverage /= storedMaxFreq.size();
        		int difference = 100000;
        		for (int i=0; i<storedMaxFreq.size(); i++){ // finds mostUsable
        			if (Math.abs(overallAverage-storedMaxFreq.get(i)) < difference) {
        				difference = Math.abs(overallAverage-storedMaxFreq.get(i));
        				mostUsable = storedMaxFreq.get(i);
        			}
        		}
        		for (int i=0; i<storedValueAmounts.length; i++) { // loop to sum up the most common frequencies
        			if (storedValueAmounts[i][1] > 0) {
        				preferredFrequencies.add(i);
        				mostCommonSum += storedValueAmounts[i][0] * (storedValueAmounts[i][1]+1); // adds weight to the summing
        				amountOfCommonSums += (storedValueAmounts[i][1]+1);
        			} else {
        				unpreferredFrequencies.add(storedValueAmounts[i][0]);
        			}
        		}
        		for (int i=0; i<(preferredFrequencies.size()); i++) { // loop to remove bad values from the most common frequencies 
        			if (storedValueAmounts[preferredFrequencies.get(i)][0] != mostUsable) {
            			if (storedValueAmounts[preferredFrequencies.get(i)][0] < (double)mostUsable*0.79
            					|| storedValueAmounts[preferredFrequencies.get(i)][0] > (double)mostUsable*1.21) {
	        				mostCommonSum -= storedValueAmounts[preferredFrequencies.get(i)][0] * (storedValueAmounts[preferredFrequencies.get(i)][1]+1); 
	        				amountOfCommonSums -= (storedValueAmounts[preferredFrequencies.get(i)][1]+1);
            			}
        			}
        		}
        		for (int i=0; i<unpreferredFrequencies.size(); i++) { // adds back in previously removed values that were, in hindsight, pretty good
        			if (unpreferredFrequencies.get(i) > mostUsable*0.9 && unpreferredFrequencies.get(i) < mostUsable*1.1) {
        				mostCommonSum += unpreferredFrequencies.get(i);
        				amountOfCommonSums++;
        			} 
        		}
        		if (amountOfCommonSums > 0) {
            		mostCommonSum = (int) Math.round((double)(mostCommonSum/amountOfCommonSums));
        		} else {
        			mostCommonSum = 0;
        		}
        		
        		// UPDATES NOTE & TEXT
        		//previousNote[p] = note[p];
        		note[p] = noteFinder.getNote(mostCommonSum);
        		int noteIndex = noteFinder.getIndex(mostCommonSum);;
        		
        		
        		if (previousNotes.size() < 3) { // sets how many times each note must be detected in a row before changing the piano visualization
        			previousNotes.add(note[p]);
            	} else {
            		ArrayList<String> tempStorage2 = new ArrayList<String>();
            		tempStorage2.addAll(previousNotes);
            		previousNotes.clear();
            		previousNotes.add(0, note[p]);
            		previousNotes.addAll(1, tempStorage2);
            		previousNotes.remove(3);
            		for (int i=1; i<(previousNotes.size()); i++) {
            			if (previousNotes.get(i) == previousNotes.get(i-1)) {
            				changeNote = true;
            			} else {
            				changeNote = false;
            				break;
            			}
            		}
            	}
        		
        		if (changeNote) { // if note change required, update currentNote & previous note
        			previousNote[p] = currentNote[p];
        			currentNote[p] = note[p];
            		if (currentNote[p] != previousNote[p]) { // check if display update needed
                		display.setText(Integer.toString(mostCommonSum)+" Hz - "+note[p]);
                		setPianoKey();
                		writeToFile(timeCount+":"+noteIndex+";", true);
                		previousNoteIndex = noteIndex;
            		} else {
            			writeToFile(timeCount+":"+previousNoteIndex+";", true);
        			}
        		} 
        		
        		// OPTIONAL LOGGING
        		/*
        		String logList =  "Member name: ";
        		for (int i = 0; i<storedMaxFreq.size(); i++){
        			logList += storedMaxFreq.get(i) + ", ";
        		}
        		logList += "\nstoredValueAmounts: " + Arrays.deepToString(storedValueAmounts);
        		Log.w("Davis", logList + "\n"+mostCommonSum+" Hz - "+note[p]); */
        		
        		
        	}
        	
        	// OPTIONAL BOLDING
        	if (max[p] > 5) {
        		display.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        	} else {
        		display.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        	}
    	}
    }
    
    public void getAvg(double[] toTransform){
        double sum = 0;
        for(int i = 5; i < 201; i++) {
        	sum += toTransform[i];
        }
        avgAmp = (int) Math.abs(sum/195*200);
    }
	
	public class RecordAudio extends AsyncTask<Void, double[], Void> {

        @Override
        protected Void doInBackground(Void... arg0) {

        	if (MainActivity.analyzeMode){
        		
        		// RECORD FROM MICROPHONE
        		try {
        			
        			//SET UP AUDIORECORDER
                    int bufferSize = AudioRecord.getMinBufferSize(frequency, 
                            channelConfiguration, audioEncoding); 
                    AudioRecord audioRecord = new AudioRecord( 
                            MediaRecorder.AudioSource.VOICE_RECOGNITION, frequency, 
                            channelConfiguration, audioEncoding, bufferSize); 
                    short[] buffer = new short[blockSize];
                    double[] toTransform = new double[blockSize];
                    audioRecord.startRecording();

                    // RECORDS AUDIO & PERFORMS FFT
                    while (started) {
                        int bufferReadResult = audioRecord.read(buffer, 0, blockSize);
                        for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                            toTransform[i] = (double) buffer[i] / 32768.0; // / 32768.0
                        }
                        
                        transformer.ft(toTransform);
                        
                        //getAvg(toTransform);
                        getFrequency(toTransform, 0, 1, 2);
                        //getFrequency(toTransform, 1, 0, 2);
                        //getFrequency(toTransform, 2, 1, 0);
                        publishProgress(toTransform);
                    }

                    audioRecord.stop();

                } catch (Throwable t) {
                    t.printStackTrace();
                    Log.e("AudioRecord", "Recording Failed");
                }
        	} else { // RECORD FROM SOUND MIX
        		
        		// SETS UP VISUALIZER
    	        visualizer = new Visualizer(0);
    	        visualizer.setEnabled(false);
    	        int capRate = Visualizer.getMaxCaptureRate();
    	        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
    	        
    	        // USES VISUALIZER TO RETURN AUDIO & THEN PERFORMS FFT
    	        Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener() {
    	          public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
    	          int samplingRate) {   
    	        	  double[] toTransform = new double[blockSize];
                      for (int i = 0; i < bytes.length; i++) {
                          toTransform[i] = (double) (bytes[i]) / 8192.0; // 32768.0
                      }
                      transformer.ft(toTransform);
                      //getAvg(toTransform);
                      getFrequency(toTransform, 0, 1, 2);
                      publishProgress(toTransform);
    	          }
    	          public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
    	            int samplingRate) {
    	          }       
    	        };
    	        
    	        int status = visualizer.setDataCaptureListener(captureListener,
    	        	    capRate, true/*wave*/, false/*no fft needed*/);
    	        visualizer.setScalingMode(Visualizer.SCALING_MODE_AS_PLAYED);
    	        visualizer.setEnabled(true);
        	}
            
            return null;
        }
        
        @Override
        protected void onProgressUpdate(double[]... toTransform) {
        	
        	// ORIENTATION CHECK
        	getOrientation();
            if (orientation==0) { // 0 for portrait
            	if (imageWidth>imageHeight){
            		FrameLayout frameLayout = (FrameLayout) getActivity().findViewById(R.id.framelayout);
  		          drawVisualization(frameLayout, "start");
  		          refreshKeyboard();
  		          drawKeyboard();
            	}
            } else if (orientation==1) { // 1 for landscape
            	if (imageWidth<imageHeight){
            		FrameLayout frameLayout = (FrameLayout) getActivity().findViewById(R.id.framelayout);
  		          drawVisualization(frameLayout, "start");
  		          refreshKeyboard();
  		          drawKeyboard();
            	}
            }
        	
            // CALLS TEXT UPDATE
        	updateText(0, 1, 2, frequencyDisplay);
        	//updateText(1, 0, 2, frequencyDisplay2);
        	//updateText(2, 0, 1, frequencyDisplay3);
        	
        	
        	// DRAWS VISUALIZATION
            canvas.drawColor(0xFF444444);

            for (int i = 0; i < toTransform[0].length/multiply; i++) {
                int x = i*(int)multiply;
                int downy = (int) ((imageHeight+5) - (toTransform[0][i] * 80)); 
                int upy = (int) imageHeight+5;

                canvas.drawLine(x, downy, x, upy, paint);
            }
            imageView.invalidate();
        }
    }
	
	public void drawWhitePressed(int n, int m){
    	pianoPaintWhites.setColor(whiteKeyPressedColor);
    	pianoCanvasWhites.drawRect((float)imageWidth/7*n, (float)0, (float)imageWidth/7*m, (float)imageHeight, pianoPaintWhites);
	}
	
	public void drawBlackPressed(int n){
		pianoPaintBlacks.setColor(blackKeyPressedColor);
    	pianoCanvasBlacks.drawRect((float)imageWidth/7*n-blackKeyWidth/2, (float)0, (float)imageWidth/7*+blackKeyWidth/2, (float)imageHeight/blackKeyHeightRatio, pianoPaintBlacks);
	}
	
	/**
	 * Draws new key depending on note
	 */
	public void setPianoKey(){
        switch (note[0]){
        case "C#/Db":
        	refreshKeyboard();
        	pianoPaintBlacks.setColor(blackKeyPressedColor);
        	pianoCanvasBlacks.drawRect((float)imageWidth/7-blackKeyWidth/2, (float)0, (float)imageWidth/7+blackKeyWidth/2, (float)imageHeight/blackKeyHeightRatio, pianoPaintBlacks);
        	break;
        case "D#/Eb":
        	refreshKeyboard();
        	pianoPaintBlacks.setColor(blackKeyPressedColor);
        	pianoCanvasBlacks.drawRect((float)imageWidth/7*2-blackKeyWidth/2, (float)0, (float)imageWidth/7*2+blackKeyWidth/2, (float)imageHeight/blackKeyHeightRatio, pianoPaintBlacks);
        	break;
        case "F#/Gb":
        	refreshKeyboard();
        	pianoPaintBlacks.setColor(blackKeyPressedColor);
        	pianoCanvasBlacks.drawRect((float)imageWidth/7*4-blackKeyWidth/2, (float)0, (float)imageWidth/7*4+blackKeyWidth/2, (float)imageHeight/blackKeyHeightRatio, pianoPaintBlacks);
        	break;
        case "G#/Ab":
        	refreshKeyboard();
        	pianoPaintBlacks.setColor(blackKeyPressedColor);
        	pianoCanvasBlacks.drawRect((float)imageWidth/7*5-blackKeyWidth/2, (float)0, (float)imageWidth/7*5+blackKeyWidth/2, (float)imageHeight/blackKeyHeightRatio, pianoPaintBlacks);
        	break;
        case "A#/Bb":
        	refreshKeyboard();
        	pianoPaintBlacks.setColor(blackKeyPressedColor);
        	pianoCanvasBlacks.drawRect((float)imageWidth/7*6-blackKeyWidth/2, (float)0, (float)imageWidth/7*6+blackKeyWidth/2, (float)imageHeight/blackKeyHeightRatio, pianoPaintBlacks);
        	break;
        case "C":
        	refreshKeyboard();
        	pianoPaintWhites.setColor(whiteKeyPressedColor);
        	pianoCanvasWhites.drawRect((float)imageWidth/7*0, (float)0, (float)imageWidth/7*1, (float)imageHeight, pianoPaintWhites);
        	break;
        case "D":
        	refreshKeyboard();
        	pianoPaintWhites.setColor(whiteKeyPressedColor);
        	pianoCanvasWhites.drawRect((float)imageWidth/7*1, (float)0, (float)imageWidth/7*2, (float)imageHeight, pianoPaintWhites);
        	break;
        case "E":
        	refreshKeyboard();
        	pianoPaintWhites.setColor(whiteKeyPressedColor);
        	pianoCanvasWhites.drawRect((float)imageWidth/7*2, (float)0, (float)imageWidth/7*3, (float)imageHeight, pianoPaintWhites);
        	break;
        case "F":
        	refreshKeyboard();
        	pianoPaintWhites.setColor(whiteKeyPressedColor);
        	pianoCanvasWhites.drawRect((float)imageWidth/7*3, (float)0, (float)imageWidth/7*4, (float)imageHeight, pianoPaintWhites);
        	break;
        case "G":
        	refreshKeyboard();
        	pianoPaintWhites.setColor(whiteKeyPressedColor);
        	pianoCanvasWhites.drawRect((float)imageWidth/7*4, (float)0, (float)imageWidth/7*5, (float)imageHeight, pianoPaintWhites);
        	break;
        case "A":
        	refreshKeyboard();
        	pianoPaintWhites.setColor(whiteKeyPressedColor);
        	pianoCanvasWhites.drawRect((float)imageWidth/7*5, (float)0, (float)imageWidth/7*6, (float)imageHeight, pianoPaintWhites);
        	break;
        case "B":
        	refreshKeyboard();
        	pianoPaintWhites.setColor(whiteKeyPressedColor);
        	pianoCanvasWhites.drawRect((float)imageWidth/7*6, (float)0, (float)imageWidth/7*7, (float)imageHeight, pianoPaintWhites);
        	break;
    	default:
    		refreshKeyboard();
    		break;
        } 
	}
	
	/**
	 * Resets canvasses
	 */
	public void refreshKeyboard(){
        pianoBitmapWhites = Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config.ARGB_8888);
        pianoCanvasWhites = new Canvas(pianoBitmapWhites);
        pianoImageWhites.setImageBitmap(pianoBitmapWhites);
        blackKeyWidth = (int)imageWidth/10;
    	pianoPaintWhites = new Paint();
    	
        pianoBitmapBlacks = Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config.ARGB_8888);
        pianoCanvasBlacks = new Canvas(pianoBitmapBlacks);
        pianoImageBlacks.setImageBitmap(pianoBitmapBlacks);
        blackKeyWidth = (int)imageWidth/10;
    	pianoPaintBlacks = new Paint();
	}
	
	public void drawKeyboard(){
		getOrientation();
        if (orientation==0) {
        	blackKeyHeightRatio = 3;
        } else if (orientation==1) {
        	blackKeyHeightRatio = 2;
        }
        blackKeyWidth = (int)imageWidth/10;
        linesBitmap = Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config.ARGB_8888);
        linesCanvas = new Canvas(linesBitmap);
        linesPaint = new Paint();
        pianoLines.setImageBitmap(linesBitmap);
        for (int i=0; i<imageWidth; i+=imageWidth/7){	
        	linesPaint.setColor(0x77888888);
        	linesCanvas.drawLine((float)i, (float)0, (float)i, (float)imageHeight, linesPaint);
        	linesPaint.setColor(0xFF202020); 
        	if (i == (int)imageWidth/7*3 || i==0 || i==(int)imageWidth/7*7){
        		continue;
        	} else {
        		linesCanvas.drawRect((float)(i-blackKeyWidth/2), (float)0, (float)(i+blackKeyWidth/2), (float)(imageHeight/blackKeyHeightRatio), linesPaint);
        	}
        }
	}
	 
}
