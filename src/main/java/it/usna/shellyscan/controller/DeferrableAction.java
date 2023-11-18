package it.usna.shellyscan.controller;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class DeferrableAction {
	public enum Status {WAITING, CANCELLED, RUNNING, SUCCESS, FAIL};
	private final String description;
	private Task task;
	private String retValue;
	private Status status = Status.WAITING;
	
	public DeferrableAction(/*Devices model, int devIndex,*/ String description, Task runner) {
//		this.model = model;
//		this.devIndex = devIndex;
		this.description = description;
		this.task = runner;
	}
	
	public Status run(ShellyAbstractDevice device) {
		try {
			status = Status.RUNNING;
			retValue = task.run(this, device);
			return (status = (retValue == null || retValue.length() == 0) ? Status.SUCCESS : Status.FAIL);
		} catch(Exception e) {
			this.retValue = e.toString();
			return (status = Status.FAIL);
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
	
	public Object getreturn() {
		return retValue;
	}
	
	public interface Task{
		String run(DeferrableAction deferrable, ShellyAbstractDevice device) throws Exception;
	}
	
	/**
	 * Release resources
	 */
	public void close() {
		task = null;
	}
	
	@Override
	public String toString() {
		return description + " : " + status + " : '" + retValue + "'";
	}
}
