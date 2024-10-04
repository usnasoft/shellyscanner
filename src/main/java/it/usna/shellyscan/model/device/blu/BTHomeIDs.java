package it.usna.shellyscan.model.device.blu;

public enum BTHomeIDs {
	SBHT_003C("Blu H&T");
	
	private final String label;
	
	private BTHomeIDs(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
}
