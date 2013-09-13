package com.quantum.unitenfc.Objects;

/**
 * Created by root on 8/4/13.
 */
public class Entry {

    private String author_name;
    private String author_pic_uri;
    private String time_stamp;
    private String message;

    public String getTime_stamp() {
        return time_stamp;
    }

    public void setTime_stamp(String time_stamp) {
        this.time_stamp = time_stamp;
    }

    public String getAuthor_pic_uri() {
        return author_pic_uri;
    }

    public void setAuthor_pic_uri(String author_pic_uri) {
        this.author_pic_uri = author_pic_uri;
    }

    public String getAuthor_name() {
        return author_name;
    }

    public void setAuthor_name(String author_name) {
        this.author_name = author_name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
