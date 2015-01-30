package com.example.davis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.view.Display;

import java.io.IOException;

import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.os.IBinder;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.app.PendingIntent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.MediaController.MediaPlayerControl;

public class MainActivity extends Activity {

	static MyMediaController mediaController;
	static VideoView videoView;

	static Chronometer mChronometer;
	static Chronometer masterChronometer;
	
	static long savedBase;
	static long lastTimePause;
	static long lastTimeLoad;

	static int sampleRate;
	static int calibrationValue;
	
	static String setFileName;
	static String lastSelectedName = null;

	static boolean gainMode;
	static boolean autoSave;
	static boolean analyzeMode;
	static boolean mStartRecording = true;
	static boolean needOrientationCheck;
	static boolean isPaused = false;
	static boolean timePaused = false;
	static boolean timePlaying = false;
	
	private static String mFileName = null;
	
	private boolean append = false;

	private String root_sd = Environment.getExternalStorageDirectory().getAbsolutePath();
	private final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	private final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private String[] mNavigationTitles;
	
	private int RECORDER_SAMPLERATE;
	private final int RECORDER_BPP = 16;
	private final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
	private final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private AudioRecord recorder = null;
	private int bufferSize = 0;
	private Thread recordingThread = null;
	private boolean isRecording = false;
	
	private DrawerAdapter drawerAdapter;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	PowerManager.WakeLock wakeLock;
	
	boolean mStartPlaying = true;

	private void notification(String message, boolean ongoing, boolean cancel) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.appiconsmaller).setContentTitle(message)
				.setContentText("Touch to return to application.").setOngoing(ongoing);
		
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, MainActivity.class);
		resultIntent.setAction(Intent.ACTION_MAIN);
		resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder.setContentIntent(resultPendingIntent);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(999, mBuilder.build());
		if (cancel) {
			mNotificationManager.cancel(999);
		}
	}

	public MainActivity() {
		File soundDirectory = new File(root_sd+"/recordings/");
		soundDirectory.mkdirs();
		mFileName = root_sd;
		mFileName += "/recordings/recording" + ".wav";
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		needOrientationCheck = true;
		
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
		sampleRate = Integer.parseInt(SP.getString("record_quality", "44100"));
		setFileName = SP.getString("file_name", "recording");
		calibrationValue = Integer.parseInt(SP.getString("calibrate_value", "440"));
		gainMode = SP.getBoolean("gain_preference", true);
		analyzeMode = SP.getBoolean("analyse_mode", false);
		autoSave = SP.getBoolean("auto_save", false);
		RECORDER_SAMPLERATE = sampleRate;

		videoView = (VideoView) findViewById(R.id.video_player);
		videoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return true;
			}
		});

		masterChronometer = (Chronometer) findViewById(R.id.master_chronometer);

		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
				RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

		mTitle = mDrawerTitle = getTitle();
		mNavigationTitles = getResources().getStringArray(
				R.array.navigation_array);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		// set a custom shadow that overlays the main content when the drawer
		// opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// set up the drawer's list view with items and click listener
		drawerAdapter = new DrawerAdapter(this,R.layout.drawer_list_item, mNavigationTitles);
		//mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mNavigationTitles));
		mDrawerList.setAdapter(drawerAdapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description for accessibility */
		R.string.drawer_close /* "close drawer" description for accessibility */
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		if (savedInstanceState == null) {
			selectItem(0);
		}

	}

	/**
	 * Loops through file names in specified folder to find an empty slot
	 */
	public String newFileName() {
		int n = 0;
		File myFile = new File(root_sd + "/recordings/" + setFileName + n
				+ ".wav");
		while (myFile.exists()) {
			n += 1;
			myFile = new File(root_sd + "/recordings/" + setFileName + n
					+ ".wav");
		}
		mFileName = root_sd;
		mFileName += "/recordings/" + setFileName + n
				+ AUDIO_RECORDER_FILE_EXT_WAV;
		return mFileName;
	}
	
	private String getTempFilename() {
		return (root_sd + "/recordings/" + AUDIO_RECORDER_TEMP_FILE);
	}

	/**
	 * Initializes recorder and starts recording and wakeLock
	 */
	private void startRecording() {
		if (gainMode) {
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
					RECORDER_SAMPLERATE, RECORDER_CHANNELS,
					RECORDER_AUDIO_ENCODING, bufferSize);
		} else {
			recorder = new AudioRecord(
					MediaRecorder.AudioSource.VOICE_RECOGNITION,
					RECORDER_SAMPLERATE, RECORDER_CHANNELS,
					RECORDER_AUDIO_ENCODING, bufferSize);
		}

		int i = recorder.getState();
		if (i == 1)
			recorder.startRecording();

		isRecording = true;

		recordingThread = new Thread(new Runnable() {

			@Override
			public void run() {
				writeAudioDataToFile();
			}
		}, "AudioRecorder Thread");

		recordingThread.start();

		acquireWakeLock();
		notification("Now Recording", true, false);
	}

	/**
	 * release recorder but set append to true. releases wakelock too
	 */
	private void pauseRecording() {
		if (null != recorder) {
			isRecording = false;

			int i = recorder.getState();
			if (i == 1)
				recorder.stop();
			recorder.release();

			recorder = null;
			recordingThread = null;
		}
		append = true;
		releaseWakeLock();
		notification("Recording Paused", true, false);
	}

	/**
	 * releases recorder and sets append to false. writes temp file to a .wav file
	 */
	private void stopRecording() {
		if (null != recorder) {
			isRecording = false;

			int i = recorder.getState();
			if (i == 1)
				recorder.stop();
			recorder.release();

			recorder = null;
			recordingThread = null;
		}
		append = false;
		copyWaveFile(getTempFilename(), newFileName());
		deleteTempFile();

		releaseWakeLock();
		notification("Recording Ended", false, true);
	}

	private void stopPausedRecording() {
		append = false;
		copyWaveFile(getTempFilename(), newFileName());
		deleteTempFile();

		// releaseWakeLock();
		notification("Recording Ended", false, true);
	}

	private void deleteTempFile() {
		File file = new File(getTempFilename());
		file.delete();
	}

	/**
	 * Loops to read data of a certain buffersize, then write it to a temp file using a FileOutputStream
	 */
	private void writeAudioDataToFile() {
		byte data[] = new byte[bufferSize];
		String filename = getTempFilename();
		FileOutputStream os = null;

		try {
			os = new FileOutputStream(filename, append);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		int read = 0;

		if (null != os) {
			while (isRecording) {
				read = recorder.read(data, 0, bufferSize);

				if (AudioRecord.ERROR_INVALID_OPERATION != read) {
					try {
						os.write(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void copyWaveFile(String inFilename, String outFilename) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = RECORDER_SAMPLERATE;
		int channels = 2;
		long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

		byte data[] = new byte[bufferSize];

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);

			while (in.read(data) != -1) {
				out.write(data);
			}

			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {

		byte[] header = new byte[44];

		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = RECORDER_BPP; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		out.write(header, 0, 44);
	}

	public void acquireWakeLock() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My wakelook");
		wakeLock.acquire();
	}

	public void releaseWakeLock() {
		wakeLock.release();
	}

	public void onDestroy() {
		super.onDestroy();
		if (!mStartRecording) {
			stopRecording();
		}
		if (masterChronometer.isActivated()) {
			mChronometer.setBase(SystemClock.elapsedRealtime());
			masterChronometer.setBase(SystemClock.elapsedRealtime());
			mChronometer.stop();
			masterChronometer.stop();
		}
		if (!analyzeMode){
			FragmentAnalyze.visualizer.release();
		}
		finish();
	}

	public void StartRecord(View v) {
		if (mStartRecording) {
			mChronometer = (Chronometer) findViewById(R.id.chronometer);
			
			// BUTTON ANIMATION
			Drawable[] layers = new Drawable[2];
			if (isPaused) {
				layers[0] = getResources().getDrawable(R.drawable.recordpause);
				layers[1] = getResources().getDrawable(R.drawable.recording);
				mChronometer.setBase(savedBase);
				masterChronometer.setBase(savedBase);
			} else {
				layers[0] = getResources().getDrawable(R.drawable.record);
				layers[1] = getResources().getDrawable(R.drawable.recording);
				if (!timePlaying) {
					mChronometer.setBase(SystemClock.elapsedRealtime());
					masterChronometer.setBase(SystemClock.elapsedRealtime());
				}
			}
			TransitionDrawable transition = new TransitionDrawable(layers);
			((ImageView) v).setImageDrawable(transition);
			transition.startTransition(300);
			
			// START RECORDING
			startRecording();
			
			// START TIMER DISPLAY
			if (timePaused) {
				mChronometer.setBase(mChronometer.getBase()
						+ SystemClock.elapsedRealtime() - lastTimePause);
				masterChronometer.setBase(masterChronometer.getBase()
						+ SystemClock.elapsedRealtime() - lastTimePause);
				timePaused = !timePaused;
			}
			mChronometer.start();
			masterChronometer.start();
			timePlaying = true;
			isPaused = false;
			
			// TOAST NOTIFICATION
			Toast pausing = Toast.makeText(this,
					"Press again to pause recording", Toast.LENGTH_SHORT);
			pausing.show();
		} else {
			
			// BUTTON ANIMATION
			Drawable[] layers2 = new Drawable[2];
			layers2[0] = getResources().getDrawable(R.drawable.recording);
			layers2[1] = getResources().getDrawable(R.drawable.recordpause);
			TransitionDrawable transition2 = new TransitionDrawable(layers2);
			((ImageView) v).setImageDrawable(transition2);
			transition2.startTransition(300);
			
			// PAUSE RECORDING
			pauseRecording();
			
			// PAUSE CHRONOMETER
			mChronometer = (Chronometer) findViewById(R.id.chronometer);
			lastTimePause = SystemClock.elapsedRealtime();
			savedBase = mChronometer.getBase();
			timePaused = true;
			timePlaying = false;
			mChronometer.stop();
			masterChronometer.stop();
			isPaused = true;
			
			// TOAST NOTIFICATION
			Toast resuming = Toast.makeText(this,
					"Press again to resume recording", Toast.LENGTH_SHORT);
			resuming.show();
		}
		mStartRecording = !mStartRecording;
	}

	public void stopRecord(View v) {
		
		// BUTTON ANIMATION
		Drawable[] layers2 = new Drawable[2];
		if (isPaused) {
			layers2[0] = getResources().getDrawable(R.drawable.recordpause);
			layers2[1] = getResources().getDrawable(R.drawable.record);
			TransitionDrawable transition2 = new TransitionDrawable(layers2);
			ImageView recordButton = (ImageView) findViewById(R.id.record_button);
			recordButton.setImageDrawable(transition2);
			transition2.startTransition(500);
			
			// END RECORDING
			stopPausedRecording();
		} else {
			if (!mStartRecording) { // if currently recording
				layers2[0] = getResources().getDrawable(R.drawable.recording);
				layers2[1] = getResources().getDrawable(R.drawable.record);
				TransitionDrawable transition2 = new TransitionDrawable(layers2);
				ImageView recordButton = (ImageView) findViewById(R.id.record_button);
				recordButton.setImageDrawable(transition2);
				transition2.startTransition(500);
				
				// END RECORDING
				stopRecording();
			} else {
				Toast noRecording = Toast.makeText(this,
						"Nothing is currently recording", Toast.LENGTH_SHORT);
				noRecording.show();
			}
		}
		
		// STOP TIMER DISPLAY
		mChronometer = (Chronometer) findViewById(R.id.chronometer);
		mChronometer.setBase(SystemClock.elapsedRealtime());
		masterChronometer.setBase(SystemClock.elapsedRealtime());
		masterChronometer.stop();
		mChronometer.stop();
		timePlaying = false;
		timePaused = false;
		mStartRecording = true;
		isPaused = false;
	}

	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

	@Override
	public void onPause() {
		if (!analyzeMode){
			FragmentAnalyze.visualizer.release();
		}
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content
		// view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle action buttons
		switch (item.getItemId()) {
		case 0:
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * The click listener for ListView in the navigation drawer 
	 */
	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position);
			((DrawerAdapter)parent.getAdapter()).selectItem(position);
		}
	}

	/**
	 * Changes main screen based on specified position
	 */
	private void selectItem(int position) {
		// update the main content by replacing fragments
		lastTimeLoad = SystemClock.elapsedRealtime();
		android.app.Fragment fragment = null;
		PreferenceFragment settings = null;
		switch (position) {
		case 0: // RECORDING SCREEN
			fragment = new FragmentRecord();
			SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
			sampleRate = Integer.parseInt(SP.getString("record_quality",
					"44100"));
			setFileName = SP.getString("file_name", "recording");
			gainMode = SP.getBoolean("gain_preference", true);
			RECORDER_SAMPLERATE = sampleRate;
			needOrientationCheck = false;
			break;
		case 1: // RECORDINGS
			fragment = new FragmentRecordings();
			needOrientationCheck = false;
			break;
		case 2: // ANALYZING
			fragment = new FragmentAnalyze(); 
			SharedPreferences SP2 = PreferenceManager.getDefaultSharedPreferences(this);
			calibrationValue = Integer.parseInt(SP2.getString("calibrate_value", "440"));
			analyzeMode = SP2.getBoolean("analyse_mode", false);
			autoSave = SP2.getBoolean("auto_save", false);
			needOrientationCheck = true;
			break;
		case 3: // SETTINGS
			settings = new FragmentSettings();
			needOrientationCheck = false;
			break;
		default:
			break;
		}

		if (fragment != null) {
			getFragmentManager().beginTransaction()
					.replace(R.id.content_frame, fragment).commit();

			// update selected item and title, then close the drawer
			mDrawerList.setItemChecked(position, true);
			setTitle(mNavigationTitles[position]);
			mDrawerLayout.closeDrawer(mDrawerList);
		} else if (settings != null) {
			// replaces fragment
			getFragmentManager().beginTransaction()
					.replace(R.id.content_frame, settings)
					.addToBackStack(null).commit();

			// update selected item and title, then close the drawer
			mDrawerList.setItemChecked(position, true);
			setTitle(mNavigationTitles[position]);
			mDrawerLayout.closeDrawer(mDrawerList);

		} else {
		// error in creating fragment
		Log.e("MainActivity", "Error in creating fragment");
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
	
	public void switchAnalyzeMode(View v) {

		// CHANGE PREFERENCES
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = SP.edit();
		if (SP.getBoolean("analyse_mode", false)) {
			editor.putBoolean("analyse_mode", false);
		} else {
			editor.putBoolean("analyse_mode", true);
		}
		editor.commit(); 
		
		// stops visualizer if need be
		if (FragmentAnalyze.started){
			FragmentAnalyze.analyseButton.performClick();
			FragmentAnalyze.visualizer.release();
		}
		
		selectItem(2); // reloads fragment
		
	}

	public void openFragmentAnalyzed(View v) {
		Intent myIntent = new Intent(this, AnalyzedActivity.class);
		startActivity(myIntent);
	}
}