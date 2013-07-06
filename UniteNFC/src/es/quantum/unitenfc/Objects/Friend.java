package es.quantum.unitenfc.Objects;

import java.util.List;

public class Friend {
	private String friend_id;
	private String friend_name;
	private String friend_pic_uri;
	private List<NFCPoint> friend_visited;
	
	public String getFriend_id() {
		return friend_id;
	}
	public void setFriend_id(String friend_id) {
		this.friend_id = friend_id;
	}
	public String getFriend_name() {
		return friend_name;
	}
	public void setFriend_name(String friend_name) {
		this.friend_name = friend_name;
	}
	public String getFriend_pic_uri() {
		return friend_pic_uri;
	}
	public void setFriend_pic_uri(String friend_pic_uri) {
		this.friend_pic_uri = friend_pic_uri;
	}
	public List<NFCPoint> getFriend_visited() {
		return friend_visited;
	}
	public void setFriend_visited(List<NFCPoint> friend_visited) {
		this.friend_visited = friend_visited;
	}
	
	public String toString(){
		return friend_id+";"+friend_name+";"+friend_pic_uri;
	}
}
