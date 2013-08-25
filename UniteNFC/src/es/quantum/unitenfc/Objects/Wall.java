package es.quantum.unitenfc.Objects;

import java.util.List;

public class Wall {

    private String wall_pos_type;
    private String wall_title;
    private String wall_description;
    private String last_seen_when;
    private String last_seen_where;
    private String wall_tag_content;
    private boolean wall_tag_private;
    private int my_rating;
    private float mean_rating;
    private List<Entry> entry_list;
    private boolean sudo;

    public String getType() {
        return wall_pos_type;
    }

    public void setType(String type) {
        this.wall_pos_type = type;
    }

    public String getTitle() {
        return wall_title;
    }

    public void setTitle(String title) {
        this.wall_title = title;
    }

    public String getDescription() {
        return wall_description;
    }

    public void setDescription(String description) {
        this.wall_description = description;
    }

    public String getLast_seen_when() {
        return last_seen_when;
    }

    public void setLast_seen_when(String last_seen_when) {
        this.last_seen_when = last_seen_when;
    }

    public String getLast_seen_where() {
        return last_seen_where;
    }

    public void setLast_seen_where(String last_seen_where) {
        this.last_seen_where = last_seen_where;
    }

    public String getWall_tag_content() {
        return wall_tag_content;
    }

    public void setWall_tag_content(String wall_tag_content) {
        this.wall_tag_content = wall_tag_content;
    }

    public int getMy_rating() {
        return my_rating;
    }

    public void setMy_rating(int my_rating) {
        this.my_rating = my_rating;
    }

    public float getMean_rating() {
        return mean_rating;
    }

    public void setMean_rating(float mean_rating) {
        this.mean_rating = mean_rating;
    }

    public List<Entry> getEntry_list() {
        return entry_list;
    }

    public void setEntry_list(List<Entry> entry_list) {
        this.entry_list = entry_list;
    }


    public boolean isSudo() {
        return sudo;
    }

    public void setSudo(boolean sudo) {
        this.sudo = sudo;
    }

    public boolean isWall_tag_private() {
        return wall_tag_private;
    }

    public void setWall_tag_private(boolean wall_tag_private) {
        this.wall_tag_private = wall_tag_private;
    }
}