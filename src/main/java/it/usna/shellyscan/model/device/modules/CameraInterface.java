package it.usna.shellyscan.model.device.modules;

public interface CameraInterface extends MotionInterface {
//	String getWebStream(int index);
	
	String setPrivacy(boolean enable);
	boolean getPrivacy();
}
