package es.quantum.unitenfc;

import android.graphics.Bitmap;

public class RowItem implements Comparable<Object>{

    private Bitmap imageId;
    private String title;

    public RowItem(Bitmap imageId, String title) {
        this.imageId = imageId;
        this.title = title;
    }

    public Bitmap getImageId() {
        return imageId;
    }

    public void setImageId(Bitmap imageId) {
        this.imageId = imageId;
    }

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

	@Override
	public int compareTo(Object arg0) {
		 RowItem row = (RowItem)arg0;        
	     return this.title.compareToIgnoreCase(row.title);           
	}
}