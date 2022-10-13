package it.usna.shellyscan.model;

public class ModelMessage {
	public enum Type {
		ADD,
		UPDATE,
		REMOVE,
		READY, // Model is ready
		CLEAR
	};
	private Type type;
	private Object ind;
	
	public ModelMessage(Type type, Object body) {
		this.type = type;
		this.ind = body;
	}
	
	public Type getType() {
		return type;
	}
	
	public Object getBody() {
		return ind;
	}
}
