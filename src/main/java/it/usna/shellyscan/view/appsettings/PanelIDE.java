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

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import java.awt.Font;

public class PanelIDE extends JPanel {
	private static final long serialVersionUID = 1L;
	IntegerTextFieldPanel tabSize;
	JCheckBox chcDarkMode;
	JRadioButton rdbtnIndentSmart;
	JRadioButton rdbtnIndentYes;
	JRadioButton rdbtnIndentNone;
	JCheckBox chckbxCloseCurly;
	JCheckBox chckbxClosebracket;
	JCheckBox chckbxCloseSquare;
	JCheckBox chckbxCloseString;
	
	PanelIDE(final AppProperties appProp) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgAppSetIDETabSize"));
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 15);
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		
		tabSize = new IntegerTextFieldPanel(appProp.getIntProperty(ScannerProperties.PROP_IDE_TAB_SIZE, ScannerProperties.IDE_TAB_SIZE_DEFAULT), 1, 32, false);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 2;
		gbc_textField.anchor = GridBagConstraints.NORTHWEST;
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		add(tabSize, gbc_textField);
		tabSize.setColumns(2);
		
		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("dlgAppSetIDEAutoIndent"));
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_2.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 15);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 1;
		add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		rdbtnIndentSmart = new JRadioButton(LABELS.getString("dlgAppSetIDEAutoIndentSmart"));
		GridBagConstraints gbc_rdbtnIndentSmart = new GridBagConstraints();
		gbc_rdbtnIndentSmart.anchor = GridBagConstraints.NORTHWEST;
		gbc_rdbtnIndentSmart.insets = new Insets(0, 0, 5, 15);
		gbc_rdbtnIndentSmart.gridx = 1;
		gbc_rdbtnIndentSmart.gridy = 1;
		add(rdbtnIndentSmart, gbc_rdbtnIndentSmart);
		
		rdbtnIndentYes = new JRadioButton(LABELS.getString("dlgAppSetIDEAutoIndentYes"));
		GridBagConstraints gbc_rdbtnIndentYes = new GridBagConstraints();
		gbc_rdbtnIndentYes.anchor = GridBagConstraints.NORTHWEST;
		gbc_rdbtnIndentYes.insets = new Insets(0, 0, 5, 15);
		gbc_rdbtnIndentYes.gridx = 2;
		gbc_rdbtnIndentYes.gridy = 1;
		add(rdbtnIndentYes, gbc_rdbtnIndentYes);
		
		ButtonGroup indentGroup = new ButtonGroup();
		indentGroup.add(rdbtnIndentSmart);
		indentGroup.add(rdbtnIndentYes);
		String indentProp = appProp.getProperty(ScannerProperties.IDE_AUTOINDENT);
		if("YES".equals(indentProp)) {
			rdbtnIndentYes.setSelected(true);
		} else if("NO".equals(indentProp)) {
			rdbtnIndentNone.setSelected(true);
		} else {
			rdbtnIndentSmart.setSelected(true); // default
		}
		
		rdbtnIndentNone = new JRadioButton(LABELS.getString("dlgAppSetIDEAutoIndentNo"));
		GridBagConstraints gbc_rdbtnIndentNone = new GridBagConstraints();
		gbc_rdbtnIndentNone.anchor = GridBagConstraints.NORTHWEST;
		gbc_rdbtnIndentNone.insets = new Insets(0, 0, 5, 15);
		gbc_rdbtnIndentNone.gridx = 3;
		gbc_rdbtnIndentNone.gridy = 1;
		add(rdbtnIndentNone, gbc_rdbtnIndentNone);
		indentGroup.add(rdbtnIndentNone);
		
		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("dlgAppSetIDEAutoClose"));
		lblNewLabel_3.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_3.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 15);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 2;
		add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		chckbxCloseCurly = new JCheckBox("{ }");
		chckbxCloseCurly.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_chckbxCloseCurly = new GridBagConstraints();
		gbc_chckbxCloseCurly.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxCloseCurly.fill = GridBagConstraints.VERTICAL;
		gbc_chckbxCloseCurly.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxCloseCurly.gridx = 1;
		gbc_chckbxCloseCurly.gridy = 2;
		add(chckbxCloseCurly, gbc_chckbxCloseCurly);
		chckbxCloseCurly.setSelected(appProp.getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_CURLY, false));
		
		chckbxClosebracket = new JCheckBox("( )");
		chckbxClosebracket.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_chckbxClosebracket = new GridBagConstraints();
		gbc_chckbxClosebracket.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxClosebracket.fill = GridBagConstraints.VERTICAL;
		gbc_chckbxClosebracket.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxClosebracket.gridx = 2;
		gbc_chckbxClosebracket.gridy = 2;
		add(chckbxClosebracket, gbc_chckbxClosebracket);
		chckbxClosebracket.setSelected(appProp.getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_BRACKET, false));
		
		chckbxCloseSquare = new JCheckBox("[ ]");
		chckbxCloseSquare.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_chckbxCloseSquare = new GridBagConstraints();
		gbc_chckbxCloseSquare.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxCloseSquare.fill = GridBagConstraints.VERTICAL;
		gbc_chckbxCloseSquare.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxCloseSquare.gridx = 3;
		gbc_chckbxCloseSquare.gridy = 2;
		add(chckbxCloseSquare, gbc_chckbxCloseSquare);
		chckbxCloseSquare.setSelected(appProp.getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_SQUARE, false));
		
		chckbxCloseString = new JCheckBox("\" \"");
		chckbxCloseString.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_chckbxCloseString = new GridBagConstraints();
		gbc_chckbxCloseString.fill = GridBagConstraints.VERTICAL;
		gbc_chckbxCloseString.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxCloseString.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxCloseString.gridx = 4;
		gbc_chckbxCloseString.gridy = 2;
		add(chckbxCloseString, gbc_chckbxCloseString);
		chckbxCloseString.setSelected(appProp.getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_STRING, false));
		
		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgAppSetIDEDarkMode"));
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_1.weighty = 1.0;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 0, 15);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 3;
		add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		chcDarkMode = new JCheckBox();
		chcDarkMode.setSelected(appProp.getBoolProperty(ScannerProperties.PROP_IDE_DARK, false));
		GridBagConstraints gbc_chcDarkMode = new GridBagConstraints();
		gbc_chcDarkMode.insets = new Insets(0, 0, 0, 5);
		gbc_chcDarkMode.anchor = GridBagConstraints.NORTHWEST;
		gbc_chcDarkMode.gridx = 1;
		gbc_chcDarkMode.gridy = 3;
		add(chcDarkMode, gbc_chcDarkMode);
	}
}
