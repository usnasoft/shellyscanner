package it.usna.shellyscan.view;

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import it.usna.swing.NumericTextField;

public class IntegerTextFieldPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final NumericTextField<Integer> itf;
	private final JButton upB;
	private final JButton downB;

	public IntegerTextFieldPanel(int min, int max) {
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));
		itf = new NumericTextField<Integer>(min, max);
		add(itf);
		
		upB = new JButton(itf.upAction());
		upB.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 2));
		upB.setContentAreaFilled(false);
		add(upB);
		
		downB = new JButton(itf.downAction());
		downB.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 5));
		downB.setContentAreaFilled(false);
		add(downB);
	}
	
	public IntegerTextFieldPanel(int init, int min, int max, boolean allowNull) {
		this(min, max);
		itf.setValue(init);
		itf.allowNull(allowNull);
	}
	
	public void setColumns(int col) {
		itf.setColumns(col);
	}
	
	public void setValue(Number val) {
		itf.setValue(val);
	}
	
	public String getText() {
		return itf.getText();
	}
	
	public int getIntValue() {
		return itf.getIntValue();
	}
	
	public boolean isEmpty() {
		return itf.isEmpty();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		itf.setEnabled(enabled);
		upB.setEnabled(enabled);
		downB.setEnabled(enabled);
	}
}