package com.example.davis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.davis.FragmentRecordings.SupportedFileFormat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.Build;
import android.preference.PreferenceManager;

public class AnalyzedActivity extends Activity {
	
	int orientation;
	private static Context context;
	int width;
	int height;
	DisplayMetrics metrics;
	HorizontalScrollView scrollView;
	private static final String TAG = "";
	private static String root_sd = Environment.getExternalStorageDirectory().getAbsolutePath();
    private String FILENAME;
    int[][] dataArray;
    
    private ListView SaveList;
    ListView listView;
    static List<SavesRowItem> saveRowItems;
    static int saveListPosition;
    static SavesArrayAdapter savesadapter;
	protected static String selectedSaveFilePath;
	private String filePath;
	String lastSelectedSave;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_analyzed);
		
		// ACTIONBAR
		ActionBar actionBar = getActionBar();
	    // add the custom view to the action bar
	    actionBar.setCustomView(R.layout.auto_save_switch);
	    Switch autoSaveSwitch = (Switch) actionBar.getCustomView().findViewById(R.id.autosave_actionbar_switch);
	    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
	    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
	    autoSaveSwitch.setChecked(SP.getBoolean("auto_save", false)); 
	    actionBar.setDisplayHomeAsUpEnabled(true);
		
		// INITIALIZING
		FILENAME = root_sd+"/analyzed/"+"myFile.txt";
		scrollView = (HorizontalScrollView) findViewById(R.id.music_staff_scrollview);
		final LinearLayout layout = (LinearLayout) findViewById(R.id.music_staff_layout);
		final DrawStaffView drawStaffView = new DrawStaffView (this);
		final LinearLayout notesLayout = (LinearLayout) findViewById(R.id.music_notes_layout);
		final DrawNotesView drawNotesView = new DrawNotesView (this);
		AnalyzedActivity.context = getApplicationContext();
		
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.analyzed_linear_layout);
		
		SaveList = (ListView) findViewById(R.id.save_list_view); 
        listView = (ListView) findViewById(R.id.save_list_view);
        refresh();

        // ORIENTATION CHECK
	    getOrientation();
	    if (orientation == 0) {
	    	linearLayout.setOrientation(LinearLayout.VERTICAL);
	    } else if (orientation == 1) {
	    	linearLayout.setOrientation(LinearLayout.HORIZONTAL);
	    }
        
		// DRAWING
		ViewTreeObserver viewTreeObserver = scrollView.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {
		  viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
		    	scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		    	height = scrollView.getHeight();
		    	getWidth();
		    	layout.addView(drawStaffView,width,height);
		    	notesLayout.addView(drawNotesView,width,height);
		    	if (listView.getCount() != 0) {
			    	selectSave(0);
		    	}
		    	
		    }
		  });
		}
		
		// LISTENERS
		autoSaveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(AnalyzedActivity.this);
				SharedPreferences.Editor editor = SP.edit();
		    	if (isChecked) {
		            // The toggle is enabled
		    		editor.putBoolean("auto_save", true);
		        } else {
		            // The toggle is disabled
		        	editor.putBoolean("auto_save", false);
		        }
				editor.commit(); 
				MainActivity.autoSave = SP.getBoolean("auto_save", false);
		    }
		});
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView parentView, View childView, int position, long id) 
            {
        		saveListPosition = position;
            	List<String> populatedList = populateSaveList();
        		String[] songs = new String[ populatedList.size() ];
        		populatedList.toArray(songs);
            	String name = songs[saveListPosition];
            	if (checkExtension(name)) { // checks extension first
        	    	if (name != lastSelectedSave) {
        	    		FILENAME = root_sd+"/analyzed/"+name;
                		lastSelectedSave = name;
                		getWidth();
                    	layout.removeAllViews();
                		layout.addView(drawStaffView,width,height);
        	    	}
            	} else {
            		Toast pausing = Toast.makeText(AnalyzedActivity.context,
        					"This is not save file.", Toast.LENGTH_SHORT);
        			pausing.show();
            	}
            }
        });
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    final int pos, long id) {
            	selectSave(pos);
        		InputMethodManager imm = (InputMethodManager) AnalyzedActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        		ContextThemeWrapper context = new ContextThemeWrapper(AnalyzedActivity.this, android.R.style.Theme_Holo);
        		final EditText input = new EditText(context);
        		input.setText(".txt");
        		new AlertDialog.Builder(context)
        	    .setTitle("New file name")
        	    .setView(input)
        	    .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
        	        public void onClick(DialogInterface dialog, int whichButton) {
        	        	getSavePath();
        	            Editable value = input.getText(); 
        	            File file = new File(selectedSaveFilePath);
        	            File file2 = new File(root_sd + "/analyzed/" + value);
        	            boolean success = file.renameTo(file2);
                		InputMethodManager imm = (InputMethodManager) AnalyzedActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                		imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        	            refresh(); 
        	        }
        	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	        public void onClick(DialogInterface dialog, int whichButton) {
                		InputMethodManager imm = (InputMethodManager) AnalyzedActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                		imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0); 
        	        }
        	    }).show(); 
        		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                return true;
            }
        }); 
		
	}
	
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);

	    final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.analyzed_linear_layout);
	    // Checks the orientation of the screen
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	    	linearLayout.setOrientation(LinearLayout.HORIZONTAL);
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	    	linearLayout.setOrientation(LinearLayout.VERTICAL);
	    }
	    
		  ViewTreeObserver observer = linearLayout.getViewTreeObserver();
	}
	
    /**
     * sets orientation variable to 0 for portrait and 1 for landscape
     */
    public void getOrientation() { 
    	int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
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
	
	/**
	 * selects a save based on a position in the listView
	 * @param position
	 * @param layout
	 * @param drawStaffView
	 */
	public void selectSave(int position) {
		listView.performItemClick(listView.getChildAt(position), position, listView.getItemIdAtPosition(position));
	}
	
	/**
	 * gets a width value (in DP) based on the length of the dataAray
	 */
	public void getWidth() {
		readFileToArray(FILENAME);
		if (dataArray != null) {
	    	width = (int) (getDP(dataArray[dataArray.length-1][0]+130));
		} else {
			width = 0;
		}
    	if (width < scrollView.getWidth()) {
    		width = scrollView.getWidth();
    	}
	}
	
	public void getSavePath(){
    	List<String> populatedList = populateSaveList();
		String[] saves = new String[ populatedList.size() ];
		populatedList.toArray(saves);
		if (saveListPosition>-1){
	    	String name = saves[saveListPosition];
	    	selectedSaveFilePath = root_sd+"/analyzed/"+name;
		}
	}
	
	/**
	 * populates the arraylist
	 * @return
	 */
	public static ArrayList<String> populateSaveList() {
		  // Directory path here
		  String path = root_sd+"/analyzed"; 
		 
		  ArrayList<String> files = new ArrayList<String>();
		  File folder = new File(path);
		  File[] listOfFiles = folder.listFiles(); 
		 
		  for (int i = 0; i < listOfFiles.length; i++) 
		  {
			   if (listOfFiles[i].isFile()) 
			   {
			   files.add(listOfFiles[i].getName());
			   }
		  }
		  return files;
	}
	
    public void refresh(){
    	List<String> populatedList = populateSaveList();
		String[] saves = new String[ populatedList.size() ];
		populatedList.toArray(saves);
		
		saveRowItems = new ArrayList<SavesRowItem>();
        for (int i = 0; i < saves.length; i++) {
            SavesRowItem item = new SavesRowItem(saves[i]);
            saveRowItems.add(item);
        }
		
        savesadapter = new SavesArrayAdapter(this,
                R.layout.saves, saveRowItems);
        listView.setAdapter(savesadapter);
        saveListPosition=-1;
    }
	
	/**
	 * returns an array filled with data from a .txt file
	 */
	public void readFileToArray(String fileName) {
		
		// DEFINITIONS
		dataArray = null;
		File file = new File(fileName);
		int dataArrayLength = 0;
		char colon = ":".charAt(0);
		char semiColon = ";".charAt(0);
		String contents;
		
		// READ FILE CONTENTS TO STRING
		int length = (int) file.length();
		if (length != 0) {
			byte[] bytes = new byte[length];
		    try {
				FileInputStream in = new FileInputStream(file);
				try {
				    in.read(bytes);
				} finally {
					in.close();
				}
			} catch (IOException e) {
				Log.e("Exception", "File read failed: " + e.toString());
			}
			contents = new String(bytes);
			//Log.w("Exception", contents);
		} else { contents = null; }
		
		if (contents != null) { // continue only if there is content in the file
			
			// GET ARRAY LENGTH & INITIALIZE ARRAY
			for (char matchColon: contents.toCharArray()) {
				if (matchColon == colon) {
					dataArrayLength++;
				}
		    }
			dataArray = new int[dataArrayLength][2];
			
			// INTERPRET DATA FROM FILE INTO ARRAY FORM
			int arrayIndexCount = 0;
			String tempStringStorage = "";
			boolean storeTime = true;
			for (char s: contents.toCharArray()) {
				if (storeTime) {
					if (s == colon) { // once an : is hit, set storeTime to false and reset tempStringStorage
						storeTime = false;
						dataArray[arrayIndexCount][0] = Integer.parseInt(tempStringStorage);
						tempStringStorage = "";
					} else {
						tempStringStorage += Character.toString(s);
					}
				} else {
					if (s == semiColon) { // once a ; is hit, set storeTime to true and reset tempStringStorage, then increase arrayIndexCount
						storeTime = true;
						dataArray[arrayIndexCount][1] = Integer.parseInt(tempStringStorage);
						tempStringStorage = "";
						arrayIndexCount++;
					} else {
						tempStringStorage += Character.toString(s);
					}
				}
		    }
			
			/*
			String temp = "";
			for (int i = 0; i<dataArray.length; i++){
			    for (int j = 0; j<dataArray[i].length; j++){
			    	temp += dataArray[i][j]+" ";
			    }
			    System.out.println(temp);
			    temp = "";
			} */
			
		}
		
	}

	
	/**
	 * custom view for data visualization. all drawing is done in onDraw
	 */
    private class DrawStaffView extends View {
        public DrawStaffView(Context context) {
            super(context);
        }
        protected void onDraw(Canvas canvas) {
        	canvas.drawColor(0xFF222222);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            int leftLineSpace = getDP(0);
            int fontSize = getDP(18);
            int note = 0;
            int lineColor = 0x99FFFFFF;
            int noteColor = 0xFF999999;
            int noteHeight = getDP(13);
            int noteWidth = getDP(15);
            
            // DRAW HORIZONTAL STAFF LINES
            paint.setColor(lineColor);
            for (int i=height/8; i<height-10; i+=height/8) {
                canvas.drawLine(leftLineSpace, i, width, i, paint);
                paint.setTextSize(fontSize);
                note++;
            }
            
            // DRAW NOTES
            if (dataArray != null) {
        		for (int i=0; i<dataArray.length; i++){
        			paint.setColor(noteColor);
        			switch (dataArray[i][1]){
        	        case 0: // G#/Ab
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*3-noteHeight/2-height/16, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*3+noteHeight/2-height/16, paint);
        	        	break;
        	        case 1: // A
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*2-noteHeight/2, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*2+noteHeight/2, paint);
        	        	break;
        	        case 2: // A#/Bb
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*2-noteHeight/2-height/16, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*2+noteHeight/2-height/16, paint);
        	        	break;
        	        case 3: // B
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*1-noteHeight/2, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*1+noteHeight/2, paint);
        	        	break;
        	        case 4: // C
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*7-noteHeight/2, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*7+noteHeight/2, paint);
        	        	break;
        	        case 5: // C#/Db
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*7-noteHeight/2-height/16, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*7+noteHeight/2-height/16, paint);
        	        	break;
        	        case 6: // D
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*6-noteHeight/2, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*6+noteHeight/2, paint);
        	        	break;
        	        case 7: // D#/Eb
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*6-noteHeight/2-height/16, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*6+noteHeight/2-height/16, paint);
        	        	break;
        	        case 8: // E
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*5-noteHeight/2, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*5+noteHeight/2, paint);
        	        	break;
        	        case 9: // F
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*4-noteHeight/2, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*4+noteHeight/2, paint);
        	        	break;
        	        case 10: // F#/Gb
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*4-noteHeight/2-height/16, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*4+noteHeight/2-height/16, paint);
        	        	break;
        	        case 11: // G
        	        	canvas.drawRect(getDP(dataArray[i][0])+leftLineSpace, height/8*3-noteHeight/2, getDP(dataArray[i][0])+leftLineSpace+noteWidth, height/8*3+noteHeight/2, paint);
        	        	break;
        	    	default:
        	    		break;
        	        } 
        		}
            }
        }
    }
    
    /**
     * Draws note text on the left side
     */
    private class DrawNotesView extends View {
        public DrawNotesView(Context context) {
            super(context);
        }
        protected void onDraw(Canvas canvas) {
        	canvas.drawColor(0xFF222222);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            int leftFontSpace = getDP(16);
            int fontSize = getDP(18);
            int yDisplacement = getDP(7);
            int note = 0;
            int lineColor = 0x99FFFFFF;
            int leftLineSpace = getDP(45);
            
            paint.setColor(0x66121212);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(lineColor);
            canvas.drawLine(leftLineSpace, 0, leftLineSpace, height, paint);
            for (int i=height/8; i<height-10; i+=height/8) {
                paint.setColor(lineColor);
                paint.setTextSize(fontSize);
                note++;
                switch (note) {
                case 1:
                	canvas.drawText("B", leftFontSpace, i+yDisplacement, paint);
                	break;
                case 2:
                	canvas.drawText("A", leftFontSpace, i+yDisplacement, paint);
                	break;
                case 3:
                	canvas.drawText("G", leftFontSpace, i+yDisplacement, paint);
                	break;
                case 4:
                	canvas.drawText("F", leftFontSpace, i+yDisplacement, paint);
                	break;
                case 5:
                	canvas.drawText("E", leftFontSpace, i+yDisplacement, paint);
                	break;
                case 6:
                	canvas.drawText("D", leftFontSpace, i+yDisplacement, paint);
                	break;
                case 7:
                	canvas.drawText("C", leftFontSpace, i+yDisplacement, paint);
                	break;
            	default:
            		break;
                }
            }
        }
    }
    
    /**
     * turns pixels into a DP value for proper scaling purposes while drawing
     * @param pixels
     * @return
     */
    public int getDP(int pixels) {
    	return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels, getResources().getDisplayMetrics());
    }
    
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.analyzed, menu);
		ActionBar actionBar = getActionBar();
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
		Switch autoSaveToggle = (Switch) actionBar.getCustomView().findViewById(R.id.autosave_actionbar_switch);
		autoSaveToggle.setChecked(SP.getBoolean("auto_save", false)); */
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void setTitle(CharSequence title) {
		getActionBar().setTitle(title);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.autosave_actionbar_switch) {

			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Check if extension is valid for this app
	 */
	private boolean checkExtension( String fileName ) {
	    String ext = getFileExtension(fileName);
	    if ( ext == null) return false;
	    try {
	        if ( SupportedFileFormat.valueOf(ext.toUpperCase()) != null ) {
	            return true;
	        }
	    } catch(IllegalArgumentException e) {
	        return false;    
	    }
	    return false; 
	}
	
	public String getFileExtension( String fileName ) {
	    int i = fileName.lastIndexOf('.');
	    if (i > 0) {
	        return fileName.substring(i+1);
	    } else 
	        return null;
	}
	
	public enum SupportedFileFormat
	{
	    TXT("txt");

	    private String filesuffix;

	    SupportedFileFormat( String filesuffix ) {
	        this.filesuffix = filesuffix;
	    }

	    public String getFilesuffix() {
	        return filesuffix;
	    }
	}

}
