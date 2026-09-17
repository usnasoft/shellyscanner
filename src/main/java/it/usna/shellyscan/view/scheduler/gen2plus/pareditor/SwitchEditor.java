package it.usna.shellyscan.view.scheduler.gen2plus.pareditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import it.usna.shellyscan.view.scheduler.gen2plus.pareditor.ParamEditorDialog.EditorPanel;

/**
 * On / Off
 */
public class SwitchEditor extends JPanel implements EditorPanel {
	private static final long serialVersionUID = 1L;
	private JToggleButton onOffButton = new JToggleButton(new ImageIcon(SwitchEditor.class.getResource("/images/Standby24.png")));
	private static final Pattern ON_OFF_PATTERN = Pattern.compile("\"on\"\\s*:\\s*((true)|(false))");
	
	public SwitchEditor(final String parameters) {
		setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));
		setLayout(new BorderLayout(65, 0));
		
		this.add(new JLabel(LABELS.getString("lblOnOff")), BorderLayout.WEST);
		
		var onOffMatcher = ON_OFF_PATTERN.matcher(parameters);
		onOffMatcher.find();
		boolean on = onOffMatcher.group(1).equals("true");

		onOffButton.setSelectedIcon(new ImageIcon(SwitchEditor.class.getResource("/images/StandbyOn24.png")));
		onOffButton.setContentAreaFilled(false);
		onOffButton.setRolloverEnabled(false);
		onOffButton.setBorder(BorderFactory.createEmptyBorder());
		onOffButton.setSelected(on);

		this.add(onOffButton, BorderLayout.EAST);
	}
	
	public static boolean check(String par) {
		return ON_OFF_PATTERN.matcher(par).find();
	}
	
	@Override
	public String change(String par) {
		var m = ON_OFF_PATTERN.matcher(par);
		return  m.replaceFirst("\"on\":" + (onOffButton.isSelected() ? "true" : "false"));
	}
}