package it.usna.shellyscan.view.scheduler;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JTextField;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class MethodHints {
	private final AbstractG2Device device;
	
	public MethodHints(AbstractG2Device device) {
		this.device = device;
	}
	
	public Object[] get(JTextField method, JTextField parameters) {
		return new Object[] {
				new AbstractAction("input 0") {

					@Override
					public void actionPerformed(ActionEvent e) {
						method.setText("input");
						parameters.setText("\"id\"=0");
					}
				}
			};
	}
}
