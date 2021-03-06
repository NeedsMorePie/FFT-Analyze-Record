package com.example.davis;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.animation.Animation.AnimationListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
 
public class SavesArrayAdapter extends ArrayAdapter<SavesRowItem> {
 
	private LayoutInflater mInflater;
	private int resId;
	
    Context context;
 
    public SavesArrayAdapter(Context context, int resourceId,
            List<SavesRowItem> items) {
        super(context, resourceId, items);
        this.context = context;
        
    }

    public static class ViewHolder {
    	public boolean needInflate;
        ImageView delete;
        TextView txtTitle;
    }
    
	private class MyCell {
		public String name;
	}
     
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        
        SavesRowItem rowItem = getItem(position);
         
        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
        	convertView = mInflater.inflate(R.layout.saves, null);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.save_title);
            holder.delete = (ImageView) convertView.findViewById(R.id.delete_save);
            convertView.setTag(holder);
        } else if (((ViewHolder)convertView.getTag()).needInflate) {
        	convertView = mInflater.inflate(R.layout.saves, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.save_title);
            holder.delete = (ImageView) convertView.findViewById(R.id.delete_save);
            convertView.setTag(holder);
		} 
        
            holder = (ViewHolder) convertView.getTag(); 
                 
        holder.txtTitle.setText(rowItem.getTitle());
        holder.delete.setImageResource(R.drawable.trash_can);
        
		holder.needInflate = false;
		convertView.setTag(holder);
        
		final View view = convertView;
		
		holder.delete.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				deleteItem(view, position);
		    	List<String> populatedList = AnalyzedActivity.populateSaveList();
				String[] songs = new String[ populatedList.size() ];
				populatedList.toArray(songs);
		    	String name = songs[position];
		    	AnalyzedActivity.selectedSaveFilePath = FragmentRecordings.root_sd+"/analyzed/"+name;
		    	File file = new File(AnalyzedActivity.selectedSaveFilePath);
		    	boolean deleted = file.delete();
			}
		});
		
        return convertView;
    }
    
    
	private void deleteItem(final View v, final int index) {
		AnimationListener al = new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation arg0) {
				AnalyzedActivity.saveRowItems.remove(index);

				ViewHolder vh = (ViewHolder)v.getTag();
				vh.needInflate = true;
				
				AnalyzedActivity.savesadapter.notifyDataSetChanged();
			}
			@Override public void onAnimationRepeat(Animation animation) {}
			@Override public void onAnimationStart(Animation animation) {}
		};
		collapse(v, al);
	}
    
	private void collapse(final View v, AnimationListener al) {
		final int initialHeight = v.getMeasuredHeight();

		Animation anim = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				if (interpolatedTime == 1) {
					v.setVisibility(View.GONE);
				}
				else {
					v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
					//v.getLayoutParams().height = 0;
					v.requestLayout();
				}
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		if (al!=null) {
			anim.setAnimationListener(al);
		}
		anim.setDuration(FragmentRecordings.ANIMATION_DURATION);
		v.startAnimation(anim);
	}
    
    @Override
    public boolean hasStableIds(){
    	return true;
    }
    
}