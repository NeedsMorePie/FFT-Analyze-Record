package com.example.davis;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DrawerAdapter extends ArrayAdapter<String> {
    private int selectedItem;

    public DrawerAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);
    }

    public void selectItem(int selectedItem){
        this.selectedItem = selectedItem;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);
        if (!(position==selectedItem)){
            ((TextView)convertView).setTypeface(Typeface.create("sans-serif-light", position == selectedItem ? Typeface.BOLD : Typeface.NORMAL));
        } else {
            ((TextView)convertView).setTypeface(Typeface.create("sans-serif", position == selectedItem ? Typeface.NORMAL : Typeface.NORMAL));
        }

        return convertView;
    }
}
