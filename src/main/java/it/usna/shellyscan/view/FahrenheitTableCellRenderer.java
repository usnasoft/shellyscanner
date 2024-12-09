package it.usna.shellyscan.view;

import java.text.NumberFormat;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class FahrenheitTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	private final NumberFormat formatter;

	public FahrenheitTableCellRenderer() {
		setHorizontalAlignment(SwingConstants.RIGHT);
		formatter = NumberFormat.getInstance();
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
	}

	@Override
	public void setValue(Object value) {
        setText(value != null ? formatter.format(((Float)value).floatValue() * 1.8f + 32f) : "");
    }
}
