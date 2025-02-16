package it.usna.shellyscan.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.Meters.Type;

public class DeviceMetersCellRenderer extends JPanel implements TableCellRenderer {
	private static final long serialVersionUID = 1L;
	private final static int HIDE_LIMIT = 4;
	private final static Insets INSETS_LABEL1 = new Insets(0, 0, 0, 2);
	private final static Insets INSETS_LABEL2 = new Insets(0, 6, 0, 2);

	private static Border EMPTY_BORDER;
	private final static Border FOCUS_BORDER = UIManager.getBorder("Table.focusCellHighlightBorder");
	private final static Font LABEL_FONT = new Font("Tahoma", Font.BOLD, 11);
//	private final Object[] singleArrayObj = new Object[1];
//	private final static MessageFormat SWITCH_FORMATTER = new MessageFormat("{0,choice,0#open|1#closed|1<{0,number,integer}}", Locale.ENGLISH);
	
	private final static JLabel EMPTY = new JLabel();
	private final static GridBagConstraints GBC_FILLER = new GridBagConstraints();
	static {
		EMPTY.setOpaque(true);
		GBC_FILLER.weightx = 1.0;
	}
	private boolean tempUnitCelsius;

	public DeviceMetersCellRenderer(boolean celsius) {
		this.tempUnitCelsius = celsius;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowWeights = new double[] {1.0, 1.0, 1.0, 1.0, 1.0}; // up to 5 rows
		setLayout(gridBagLayout);
		final Insets borderInsets = FOCUS_BORDER.getBorderInsets(this);
		EMPTY_BORDER = BorderFactory.createEmptyBorder(borderInsets.top, borderInsets.left, borderInsets.bottom, borderInsets.right);
	}

	@Override
	public JPanel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//		try {
		removeAll();
		if(value != null) {
			final Color foregroundColor = isSelected ? table.getSelectionForeground() : table.getForeground();
			Meters ms[] = (Meters[])value;
			for(int i = 0; i < ms.length; i++) {
				final Meters m = ms[i];
				if(m != null) {
					int j = 0;
					Type[] types = m.getTypes();
					for(Meters.Type t: types) {
						if(types.length <= HIDE_LIMIT || isVisible(t)) {
							JLabel label = new JLabel(Main.LABELS.getString("METER_LBL_" + t));
							GridBagConstraints gbc_label = new GridBagConstraints();
							gbc_label.insets = (j > 0) ? INSETS_LABEL2 : INSETS_LABEL1;
							gbc_label.anchor = GridBagConstraints.WEST;
							gbc_label.weightx = 0.0;
							gbc_label.gridx = j * 2;
							gbc_label.gridy = i;
							label.setForeground(foregroundColor);
							label.setFont(LABEL_FONT);
							add(label, gbc_label);

							JLabel val;
							float metValue = m.getValue(t);
							if(t == Meters.Type.T || t == Meters.Type.T1 || t == Meters.Type.T2 || t == Meters.Type.T3 || t == Meters.Type.T4) {
								if(tempUnitCelsius) {
									val = new JLabel(String.format(Locale.ENGLISH, Main.LABELS.getString("METER_VAL_T"), metValue));
								} else { // fahrenheit 
									val = new JLabel(String.format(Locale.ENGLISH, Main.LABELS.getString("METER_VAL_T_F"), metValue * 1.8f + 32f));
								}
							} else if(t == Meters.Type.EX) {
//								singleArrayObj[0] = metValue; val = new JLabel(SWITCH_FORMATTER.format(singleArrayObj));
								val = new JLabel(Main.LABELS.getString((metValue == 0f) ? "METER_VAL_EX_0" : "METER_VAL_EX_1"));
							} else {
								val = new JLabel(String.format(Locale.ENGLISH, Main.LABELS.getString("METER_VAL_" + t), metValue));
								if(metValue == 0f) {
									val.setEnabled(false);
								}
							}
							GridBagConstraints gbc_value = new GridBagConstraints();
							gbc_value.anchor = GridBagConstraints.EAST;
							gbc_value.weightx = 0.0;
							gbc_value.gridx = gbc_label.gridx + 1;
							gbc_value.gridy = i;
							val.setForeground(foregroundColor);
							add(val, gbc_value);

							j++;
						}
					}
					GBC_FILLER.gridx = (j * 2) + 2;
					GBC_FILLER.gridy = i;
					add(EMPTY, GBC_FILLER);
				}
			}
		}
		setBorder(hasFocus ? FOCUS_BORDER : EMPTY_BORDER);
		return this;
//		}catch(Exception e) {
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