package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import it.usna.shellyscan.view.IntegerTextFieldPanel;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.util.AppProperties;
import javax.swing.JCheckBox;

public class PanelIDE extends JPanel {
	private static final long serialVersionUID = 1L;
	IntegerTextFieldPanel tabSize;
	JCheckBox chcDarkMode;
	
	PanelIDE(final AppProperties appProp) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{0.0, 1.0};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgAppSetIDETabSize"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		
		tabSize = new IntegerTextFieldPanel(appProp.getIntProperty(ScannerProperties.PROP_IDE_TAB_SIZE, ScannerProperties.IDE_TAB_SIZE_DEFAULT), 1, 32, false);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.anchor = GridBagConstraints.NORTHWEST;
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		add(tabSize, gbc_textField);
		tabSize.setColumns(2);
		
		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgAppSetIDEDarkMode"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_1.weighty = 1.0;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		chcDarkMode = new JCheckBox();
		chcDarkMode.setSelected(appProp.getBoolProperty(ScannerProperties.PROP_IDE_DARK, false));
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 1;
		add(chcDarkMode, gbc_chckbxNewCheckBox);
	}
}
