package com.example.davis;
public class SavesRowItem {
    //private int imageId;
    private String title;
     
    public SavesRowItem(String title) {
        //this.imageId = imageId;
        this.title = title;
    }
    
    /*
    public int getImageId() {
        return imageId;
    }
    public void setImageId(int imageId) {
        this.imageId = imageId;
    }
    */

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    @Override
    public String toString() {
        return title;
    }   
}