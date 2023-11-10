package it.usna.shellyscan.controller;

import java.util.concurrent.Callable;

public class DeferrableAction {
	public enum Status {WAITING, RUNNING, SUCCESS, FAIL};
	private String description;
//	private Task runner;
	private Callable<?> runner;
	private Object retValue;
	private Status status = Status.WAITING;
	
	public DeferrableAction(String description, /*Task*/Callable<?> runner) {
		this.description = description;
		this.runner = runner;
	}
	
	public boolean run() {
		try {
			status = Status.RUNNING;
			retValue = runner.call();
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
	
	public Status getStatus() {
		return status;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Object getreturn() {
		return retValue;
	}
	
//	public interface Task {
//		Object run() throws Exception;
//	}
//	
	/**
	 * Release resources
	 */
	public void close() {
		runner = null;
	}
}
