package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import it.usna.shellyscan.view.IntegerTextFieldPanel;
import it.usna.util.AppProperties;

public class PanelIDE extends JPanel {
	private static final long serialVersionUID = 1L;
	IntegerTextFieldPanel tabSize;
	
	PanelIDE(final AppProperties appProp) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{0.0, 1.0};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgAppSetIDETabSize"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.weighty = 1.0;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		
		tabSize = new IntegerTextFieldPanel(appProp.getIntProperty(DialogAppSettings.PROP_IDE_TAB_SIZE, DialogAppSettings.IDE_TAB_SIZE_DEFAULT), 1, 32, false);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.anchor = GridBagConstraints.WEST;
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		add(tabSize, gbc_textField);
		tabSize.setColumns(2);
	}
}
