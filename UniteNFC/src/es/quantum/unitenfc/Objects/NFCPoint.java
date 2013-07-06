package es.quantum.unitenfc.Objects;

public class NFCPoint {

	private String name;
	private String posId;	//NFCPoint type
	private String date;
	
	
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
	
}
