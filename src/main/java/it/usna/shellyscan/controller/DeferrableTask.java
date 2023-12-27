package it.usna.shellyscan.controller;

import java.io.Closeable;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class DeferrableTask implements Closeable {
	public enum Status {WAITING, CANCELLED, RUNNING, SUCCESS, FAIL};
	private final String description;
	private Task task;
	private String retValue;
	private Status status = Status.WAITING;
	
	public DeferrableTask(String description, Task runner) {
		this.description = description;
		this.task = runner;
	}
	
	public Status run(ShellyAbstractDevice device) {
		try {
			status = Status.RUNNING;
			retValue = task.run(this, device);
			return (status = (retValue == null || retValue.length() == 0) ? Status.SUCCESS : Status.FAIL);
		} catch(Exception e) {
			String msg = e.getMessage();
			this.retValue = msg.length() > 0 ? msg : e.toString();
			return (status = Status.FAIL);
		} finally {
			close();
		}
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
		return description + " : " + status + " : '" + retValue + "'";
	}
	
	/**
	 * Task definition
	 */
	public interface Task {
		String run(DeferrableTask deferrable, ShellyAbstractDevice device) throws Exception;
	}
}