package com.example.davis;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.MediaController;

public class MyMediaController extends MediaController {
	   public MyMediaController(Context context, View anchor) {
	        super(context);
	        super.setAnchorView(anchor); 
	    }

	   /*
	      @Override  
	       public void setAnchorView(View view)     {  
	           // Do nothing   
	      } 
	     
	     
	    @Override
	    public void show(int timeout) {
	        super.show(0);
	    }
	    
	    @Override
        public void hide()
        {
            super.show();
        }
        
	    
	    @Override
	    public void setMediaPlayer(MediaPlayerControl player) {
	        super.setMediaPlayer(player);
	        //this.show();
	    }*/
}
