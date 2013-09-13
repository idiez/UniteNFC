package com.quantum.unitenfc;

import android.graphics.Bitmap;

public class EntryItem implements Comparable<Object>{

    private Bitmap imageId;
    private String author;
    private String date;
    private String message;

    public EntryItem(Bitmap imageId, String author, String date, String message) {
        this.imageId = imageId;
        this.author = author;
        this.date = date;
        this.message = message;
    }

    public Bitmap getImageId() {
        return imageId;
    }

    public void setImageId(Bitmap imageId) {
        this.imageId = imageId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }

    @Override
    public int compareTo(Object arg0) {
        EntryItem row = (EntryItem)arg0;
        return this.message.compareToIgnoreCase(row.message);
    }
}