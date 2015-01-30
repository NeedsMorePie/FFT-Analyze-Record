package com.example.davis;


import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.davis.SongArrayAdapter.ViewHolder;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Service;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.VideoView;
import android.app.Activity;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.TextView;
import android.view.*;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;


public class FragmentRecordings extends Fragment  {
	
	static final int ANIMATION_DURATION = 200;

    public static final String[] titles = new String[] {};
	public static final String[] descriptions = new String[] {};

	public static final Integer[] images = {R.drawable.icon, R.drawable.icon, R.drawable.icon, R.drawable.icon};
	
	ListView listView;
    static List<RowItem> rowItems;
	
	static int listPosition;
	
	static String selectedFilePath;
    static String root_sd = Environment.getExternalStorageDirectory().getAbsolutePath();

	static SongArrayAdapter newadapter;
	
    private ListView SongList;

    private String filePath;

	protected Context context;
    
	public FragmentRecordings() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}
	
	@Override
	 public View onCreateView(LayoutInflater inflater, ViewGroup container, 
		        Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_recordings, container, false);
        
        // INITIALIZING
        SongList = (ListView) rootView.findViewById(R.id.song_list); 
		MainActivity.mediaController = new MyMediaController(getActivity(), SongList);
		MainActivity.videoView.setMediaController(MainActivity.mediaController);
        FrameLayout renameButton = (FrameLayout) rootView.findViewById(R.id.rename_button);
        listView = (ListView) rootView.findViewById(R.id.song_list);
        
        // FILLS LIST
        refresh();
        
        // ONCLICK LISTENERS
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v,pos,id);
            }
        });
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView parentView, View childView, int position, long id) 
            {
                listPosition = position;
            	List<String> populatedList = populateList();
        		String[] songs = new String[ populatedList.size() ];
        		populatedList.toArray(songs);
    	    	String name = songs[listPosition];
    	    	if (checkExtension(name)) { // checks extension first
        	    	if (name.equals(MainActivity.lastSelectedName)){
        	    		MainActivity.mediaController.setAnchorView(SongList);
        	    		MainActivity.mediaController.show();
        	    	} else {
        	    		if (MainActivity.videoView.isPlaying()){
        	    			MainActivity.videoView.pause();
        	    		}
            	    	filePath = root_sd+"/recordings/"+name;
                		Uri video = Uri.parse(filePath);
                		MainActivity.videoView.setVideoURI(video);
                		MainActivity.videoView.requestFocus();
                		MainActivity.mediaController.setAnchorView(SongList);
                		MainActivity.mediaController.show();
                        MainActivity.lastSelectedName = name;
        	    	}
    	    	} else {
    	    		Toast pausing = Toast.makeText(getActivity(),
    						"This is not a playable audio file", Toast.LENGTH_SHORT);
    				pausing.show();
    	    	}
            }
        });
        
        renameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if (listPosition>-1){
            		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            		ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
            		final EditText input = new EditText(context);
            		input.setText(".wav");
            		new AlertDialog.Builder(context)
            	    .setTitle("New file name")
            	    .setView(input)
            	    .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            	        public void onClick(DialogInterface dialog, int whichButton) {
            	        	getSongPath();
            	            Editable value = input.getText(); 
            	            File file = new File(selectedFilePath);
            	            File file2 = new File(root_sd + "/recordings/" + value);
            	            boolean success = file.renameTo(file2);
                    		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    		imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            	            refresh();
            	        }
            	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            	        public void onClick(DialogInterface dialog, int whichButton) {
                    		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    		imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            	        }
            	    }).show();
            		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            	}

            }
        });
        
        if (savedInstanceState != null) {
        }
        
        return rootView;
    }
	
	@Override
	public void onPause(){
		super.onPause();
	}
	 
	public void getSongPath(){
    	List<String> populatedList = populateList();
		String[] songs = new String[ populatedList.size() ];
		populatedList.toArray(songs);
		if (listPosition>-1){
	    	String name = songs[listPosition];
	    	selectedFilePath = root_sd+"/recordings/"+name;
		}
	}
    
	/**
	 * Fills ListView with items
	 */
    public void refresh(){
    	List<String> populatedList = populateList();
		String[] songs = new String[ populatedList.size() ];
		populatedList.toArray(songs);
		
		List<String> populatedDurations = populateDurations();
		String[] durations = new String[ populatedDurations.size() ];
		populatedDurations.toArray(durations);
		
        rowItems = new ArrayList<RowItem>();
        for (int i = 0; i < songs.length; i++) {
            RowItem item = new RowItem(songs[i], durations[i]);
            rowItems.add(item);
        }
		
        newadapter = new SongArrayAdapter(getActivity(),
                R.layout.song, rowItems);
        listView.setAdapter(newadapter);
        listPosition=-1;
    }
	
	protected boolean onLongListItemClick(View v, int pos, long id) {
	    return true;
	}

	/**
	 * OPTIONAL
	 */
	public ArrayList<Integer> populateImages() {
		  String path = root_sd+"/recordings"; 
		  ArrayList<Integer> images = new ArrayList<Integer>();
		  File folder = new File(path);
		  File[] listOfFiles = folder.listFiles(); 
		  for (int i = 0; i < listOfFiles.length; i++) 
		  {
			   if (listOfFiles[i].isFile()) 
			   {
			   images.add(R.drawable.icon);
			   }
		  }
		  return images;
	}    
	
	public static ArrayList<String> populateList() {
		  // Directory path here
		  String path = root_sd+"/recordings"; 
		 
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
	
	/**
	 * Gets durations of audio tracks, formats them, and puts them in an ArrayList
	 */
	public ArrayList<String> populateDurations() {
		  // Directory path here
		  String path = root_sd+"/recordings"; 
		  MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
		  
		  ArrayList<String> files = new ArrayList<String>();
		  File folder = new File(path);
		  File[] listOfFiles = folder.listFiles(); 
		 
		  for (int i = 0; i < listOfFiles.length; i++) 
		  {
			  if (listOfFiles[i].isFile()) 
			  {
				Date creationDate = new Date(listOfFiles[i].lastModified());
			    if(checkExtension(listOfFiles[i].getName())){
					metaRetriever.setDataSource(listOfFiles[i].getPath());
					String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
					long dur = Long.parseLong(duration);
				    long second = dur/1000;
				    long hr = second/3600;
				    long rem = second%3600;
				    long mn = (rem)/60;
				    long sec = (rem)%60;
				    String hrStr = (hr<10 ? "0" : "")+hr;
				    String mnStr = (mn<10 ? "0" : "")+mn;
				    String secStr = (sec<10 ? "0" : "")+sec; 
				    files.add(creationDate+"  |  "+hrStr+ ":"+mnStr+ ":"+secStr+"");
		    	    } else {
		    	    	files.add(creationDate+"  |  ??:??:??");
		    	    }
				}
		  }
		  return files;
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
	    MP4("mp4"),
	    M4A("m4a"),
	    AAC("aac"),
	    TS("ts"),
	    FLAC("flac"),
	    MP3("mp3"),
	    MID("mid"),
	    XMF("xmf"),
	    MXMF("mxmf"),
	    RTTTL("rtttl"),
	    RTX("rtx"),
	    OTA("ota"),
	    IMY("imy"),
	    OGG("ogg"),
	    MKV("mkv"),
	    _3GP("3gp"),
	    WAV("wav");

	    private String filesuffix;

	    SupportedFileFormat( String filesuffix ) {
	        this.filesuffix = filesuffix;
	    }

	    public String getFilesuffix() {
	        return filesuffix;
	    }
	}
	
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }
  

}
