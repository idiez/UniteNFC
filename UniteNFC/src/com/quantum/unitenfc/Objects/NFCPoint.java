package com.quantum.unitenfc.Objects;

public class NFCPoint {

	private String name;
	private String posId;	//NFCPoint type
	private String date;
    private String wall;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPosId() {
		return posId;
	}

	public void setPosId(String posId) {
		this.posId = posId;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String toString(){
		return name+";"+posId+";"+date;
	}

    public String getWall() {
        return wall;
    }

    public void setWall(String wall) {
        this.wall = wall;
    }
}
