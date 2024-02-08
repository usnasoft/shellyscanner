package it.usna.shellyscan.controller;

import java.io.Closeable;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class DeferrableTask implements Closeable {
	public enum Status {WAITING, CANCELLED, RUNNING, SUCCESS, FAIL};
	public enum Type {FW_UPDATE, RESTORE, BACKUP, MQTT, LOGIN};
	private final Type type;
	private final String description;
	private Task task;
	private String retValue;
	private Status status = Status.WAITING;
	
	public DeferrableTask(Type type, String description, Task runner) {
		this.type = type;
		this.description = description;
		this.task = runner;
	}
	
	public final Status run(ShellyAbstractDevice device) {
		try {
			status = Status.RUNNING;
			retValue = task.run(this, device);
			status = (retValue == null || retValue.length() == 0) ? Status.SUCCESS : Status.FAIL;
		} catch(Exception e) {
			String msg = e.getMessage();
			this.retValue = msg.length() > 0 ? msg : e.toString();
			status = Status.FAIL;
		} finally {
			close();
		}
		return status;
	}
	
	public void cancel() {
		close();
		status = Status.CANCELLED;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status s) {
		status = s;
	}
	
	public synchronized boolean statusToRun() {
		if(status == Status.WAITING) {
			status = Status.RUNNING;
			return true;
		}
		return false;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getReturn() {
		return retValue;
	}
	
	/**
	 * Release resources
	 */
	@Override
	public void close() {
		task = null;
	}
	
	@Override
	public String toString() {
		return type + " : " + description + " : " + status + " : '" + retValue + "'";
	}
	
	/**
	 * Task definition
	 */
	@FunctionalInterface
	public interface Task {
		String run(DeferrableTask deferrable, ShellyAbstractDevice device) throws Exception;
	}
}