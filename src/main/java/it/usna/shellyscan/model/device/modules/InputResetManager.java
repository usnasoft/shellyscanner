package it.usna.shellyscan.model.device.modules;

public interface InputResetManager {
	enum Status {TRUE, FALSE, NOT_APPLICABLE, MIX};
	
	Status getVal();
	
	Boolean getValAsBoolean();
	
	String enableReset(boolean enable);
}
