package it.usna.shellyscan.controller;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class DeferrableAction {
	public enum Status {WAITING, CANCELLED, RUNNING, SUCCESS, FAIL};
	private final String description;
//	private final Devices model;
//	private final int devIndex;
	private Task<?> runner;
	private Object retValue;
	private Status status = Status.WAITING;
	
	public DeferrableAction(/*Devices model, int devIndex,*/ String description, Task<?> runner) {
//		this.model = model;
//		this.devIndex = devIndex;
		this.description = description;
		this.runner = runner;
	}
	
	public boolean run(ShellyAbstractDevice device) {
		try {
			status = Status.RUNNING;
			retValue = runner.run(device);
			status = Status.SUCCESS;
			return true;
		} catch(Exception e) {
			this.retValue = e;
			status = Status.FAIL;
			return false;
		}
	}
	
//	public void execute() throws Exception {
//		try {
//			status = Status.RUNNING;
//			runner.run();
//			status = Status.SUCCESS;
//		} catch(Exception e) {
//			this.retValue = e;
//			status = Status.FAIL;
//			throw e;
//		}
//	}
	
	public void cancel() {
		close();
		status = Status.CANCELLED;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Object getreturn() {
		return retValue;
	}
	
	public interface Task<T> {
		T run(ShellyAbstractDevice dedice) throws Exception;
	}
	
	/**
	 * Release resources
	 */
	public void close() {
		runner = null;
	}
}
