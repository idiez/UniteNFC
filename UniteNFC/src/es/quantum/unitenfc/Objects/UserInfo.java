package es.quantum.unitenfc.Objects;

import java.util.List;

public class UserInfo {

	private String user_name;
	private String pic_uri;
	private List<NFCPoint> visited;
	private List<NFCPoint> registered;
	private List<Friend> friends;
	public String getUser_name() {
		return user_name;
	}
	public void setUser_name(String user_name) {
		this.user_name = user_name;
	}
	public String getPic_uri() {
		return pic_uri;
	}
	public void setPic_uri(String pic_uri) {
		this.pic_uri = pic_uri;
	}
	public List<NFCPoint> getVisited() {
		return visited;
	}
	public void setVisited(List<NFCPoint> visited) {
		this.visited = visited;
	}
	public List<NFCPoint> getRegistered() {
		return registered;
	}
	public void setRegistered(List<NFCPoint> registered) {
		this.registered = registered;
	}
	public List<Friend> getFriends() {
		return friends;
	}
	public void setFriends(List<Friend> friends) {
		this.friends = friends;
	}
	
}
