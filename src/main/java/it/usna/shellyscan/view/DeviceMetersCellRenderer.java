package it.usna.shellyscan.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.MissingResourceException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.meters.Meters.Type;

public class DeviceMetersCellRenderer extends JPanel implements TableCellRenderer {
	private static final long serialVersionUID = 1L;
	private static final int HIDE_LIMIT = 4;
	private static final Insets INSETS_LABEL1 = new Insets(0, 0, 0, 2);
	private static final Insets INSETS_LABEL2 = new Insets(0, 6, 0, 2);

	private static final Border FOCUS_BORDER = UIManager.getBorder("Table.focusCellHighlightBorder");
	private final Font labelFont;
	private final Border emptyBorder;
	
	private static final Component EMPTY_ALIGN_FILLER = Box.createHorizontalStrut(0);
	private static final GridBagConstraints GBC_FILLER = new GridBagConstraints();
	
	private boolean tempUnitCelsius;

	public DeviceMetersCellRenderer(boolean celsius) {
		this.tempUnitCelsius = celsius;
		GBC_FILLER.weightx = 1.0;
		GBC_FILLER.gridy = 0;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowWeights = new double[] {1.0, 1.0, 1.0, 1.0, 1.0}; // up to 5 rows
		setLayout(gridBagLayout);
		final Insets borderInsets = FOCUS_BORDER.getBorderInsets(this);
		emptyBorder = BorderFactory.createEmptyBorder(borderInsets.top, borderInsets.left, borderInsets.bottom, borderInsets.right);
		Font defFont = new JLabel().getFont();
		labelFont = defFont.deriveFont(Font.BOLD, defFont.getSize() - 1);
	}

	@Override
	public JPanel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//		try {
		removeAll();
		if(value != null) {
			final Color foregroundColor = isSelected ? table.getSelectionForeground() : table.getForeground();
			Meters[] ms = (Meters[])value;
			int maxCol = 0;
			for(int gridRow = 0; gridRow < ms.length; gridRow++) {
				final Meters m = ms[gridRow];
				if(m != null) {
					int gridCol = 0;
					Type[] types = m.getTypes();
					for(Meters.Type t: types) {
						if(types.length <= HIDE_LIMIT || isVisible(t)) {
							JLabel label = new JLabel(Main.LABELS.getString("METER_LBL_" + t));
							GridBagConstraints gbc_label = new GridBagConstraints();
							gbc_label.insets = (gridCol > 0) ? INSETS_LABEL2 : INSETS_LABEL1;
							gbc_label.anchor = GridBagConstraints.WEST;
							gbc_label.weightx = 0.0;
							gbc_label.gridx = gridCol;
							gbc_label.gridy = gridRow;
							label.setForeground(foregroundColor);
							label.setFont(labelFont);
							add(label, gbc_label);

							JLabel val;
							float metValue = m.getValue(t);
							if(t == Meters.Type.T || t == Meters.Type.T1 || t == Meters.Type.T2 || t == Meters.Type.T3 || t == Meters.Type.T4) {
								if(tempUnitCelsius) {
									val = new JLabel(String.format(Locale.ENGLISH, Main.LABELS.getString("METER_VAL_T"), metValue));
								} else { // fahrenheit 
									val = new JLabel(String.format(Locale.ENGLISH, Main.LABELS.getString("METER_VAL_T_F"), metValue * 1.8f + 32f));
								}
							} else if(t.isEnumType()) {
								try {
									val = new JLabel(Main.LABELS.getString("METER_VAL_" + t + "_"  + (int)metValue));
								} catch(MissingResourceException e) {
									val = new JLabel("-");
								}
							} else {
								val = new JLabel(String.format(Locale.ENGLISH, Main.LABELS.getString("METER_VAL_" + t), metValue));
								if(metValue == 0f) {
									val.setEnabled(false);
								}
							}
							GridBagConstraints gbc_value = new GridBagConstraints();
							gbc_value.anchor = GridBagConstraints.EAST;
							gbc_value.weightx = 0.0;
							gbc_value.gridx = gridCol + 1;
							gbc_value.gridy = gridRow;
							val.setForeground(foregroundColor);
							add(val, gbc_value);

							gridCol += 2;
						}
					}
					if(gridCol > maxCol) {
						maxCol = gridCol;
					}
//					GBC_FILLER.gridx = gridCol + 2;
//					GBC_FILLER.gridy = gridRow;
//					add(EMPTY_ALIGN_FILLER, GBC_FILLER);
				}
			}
			// add a filler on row 0 last column + 1 
			GBC_FILLER.gridx = maxCol;
			add(EMPTY_ALIGN_FILLER, GBC_FILLER);
		}
		setBorder(hasFocus ? FOCUS_BORDER : emptyBorder);
		return this;
//		} catch(Exception e) {
//			e.printStackTrace();
//			return this;
//		}
	}
	
	public void setTempUnit(boolean celsius) {
		tempUnitCelsius = celsius;
	}
	
	private static boolean isVisible(Meters.Type t) {
		return /*t != Meters.Type.VAR &&*/ t != Meters.Type.VA && t != Meters.Type.FREQ;
	}
	
	public static boolean hasHiddenMeasures(Meters meters) {
		Type[] types = meters.getTypes();
		if(types.length > HIDE_LIMIT) {
			for(Meters.Type t: meters.getTypes()) {
				if(/*t == Meters.Type.VAR ||*/ t == Meters.Type.VA || t == Meters.Type.FREQ) {
					return true;
				}
			}
		}
		return false;
	}
	
//	public static void main(String ...strings) {
//		MessageFormat f = new MessageFormat("{0,choice,0# x|1# y|1< {0,number,integer}}");
//		System.out.println(f.format(new Object[] {10}));
//		
//		System.out.println(MessageFormat.format("{0,choice,0# x|1# y|1< {0,number,integer}}", new Object[] {1}));
//	}
}