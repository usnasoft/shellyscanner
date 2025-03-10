package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import it.usna.shellyscan.view.util.IntegerTextFieldPanel;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.util.AppProperties;

public class PanelIDE extends JPanel {
	private static final long serialVersionUID = 1L;
	private IntegerTextFieldPanel tabSize;
	private JCheckBox chcDarkMode;
	private JRadioButton rdbtnIndentSmart;
	private JRadioButton rdbtnIndentYes;
	private JRadioButton rdbtnIndentNone;
	private JCheckBox chckbxCloseCurly;
	private JCheckBox chckbxClosebracket;
	private JCheckBox chckbxCloseSquare;
	private JCheckBox chckbxCloseString;
	private IntegerTextFieldPanel fontSize;
	
	PanelIDE(final AppProperties appProp) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel_5 = new JLabel(LABELS.getString("dlgAppSetIDETitle"));
		lblNewLabel_5.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_5.gridwidth = 5;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 25, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 0;
		add(lblNewLabel_5, gbc_lblNewLabel_5);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgAppSetIDETabSize"));
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 15);
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 1;
		add(lblNewLabel, gbc_lblNewLabel);
		
		tabSize = new IntegerTextFieldPanel(appProp.getIntProperty(ScannerProperties.PROP_IDE_TAB_SIZE, ScannerProperties.IDE_TAB_SIZE_DEFAULT), 1, 32, false);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 4;
		gbc_textField.anchor = GridBagConstraints.NORTHWEST;
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 1;
		add(tabSize, gbc_textField);
		tabSize.setColumns(2);
		
		JLabel lblNewLabel_4 = new JLabel(LABELS.getString("dlgAppSetIDEFontSize"));
		lblNewLabel_4.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_4.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 15);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 2;
		add(lblNewLabel_4, gbc_lblNewLabel_4);
		
		fontSize = new IntegerTextFieldPanel(appProp.getIntProperty(ScannerProperties.PROP_IDE_FONT_SIZE, ScannerProperties.IDE_FONT_SIZE_DEFAULT), 8, 24, false);
		GridBagConstraints gbc_textFontField = new GridBagConstraints();
		gbc_textFontField.fill = GridBagConstraints.BOTH;
		gbc_textFontField.gridwidth = 4;
		gbc_textFontField.anchor = GridBagConstraints.NORTHWEST;
		gbc_textFontField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.BOTH;
		gbc_textFontField.gridx = 1;
		gbc_textFontField.gridy = 2;
		add(fontSize, gbc_textFontField);
		fontSize.setColumns(2);
		
		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("dlgAppSetIDEAutoIndent"));
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_2.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_2.insets = new Insets(4, 0, 9, 15);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 3;
		add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		rdbtnIndentSmart = new JRadioButton(LABELS.getString("dlgAppSetIDEAutoIndentSmart"));
		GridBagConstraints gbc_rdbtnIndentSmart = new GridBagConstraints();
		gbc_rdbtnIndentSmart.fill = GridBagConstraints.VERTICAL;
		gbc_rdbtnIndentSmart.anchor = GridBagConstraints.NORTHWEST;
		gbc_rdbtnIndentSmart.insets = new Insets(0, 0, 5, 15);
		gbc_rdbtnIndentSmart.gridx = 1;
		gbc_rdbtnIndentSmart.gridy = 3;
		add(rdbtnIndentSmart, gbc_rdbtnIndentSmart);
		
		rdbtnIndentYes = new JRadioButton(LABELS.getString("dlgAppSetIDEAutoIndentYes"));
		GridBagConstraints gbc_rdbtnIndentYes = new GridBagConstraints();
		gbc_rdbtnIndentYes.fill = GridBagConstraints.VERTICAL;
		gbc_rdbtnIndentYes.anchor = GridBagConstraints.NORTHWEST;
		gbc_rdbtnIndentYes.insets = new Insets(0, 0, 5, 15);
		gbc_rdbtnIndentYes.gridx = 2;
		gbc_rdbtnIndentYes.gridy = 3;
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
		gbc_rdbtnIndentNone.fill = GridBagConstraints.VERTICAL;
		gbc_rdbtnIndentNone.anchor = GridBagConstraints.NORTHWEST;
		gbc_rdbtnIndentNone.insets = new Insets(0, 0, 5, 15);
		gbc_rdbtnIndentNone.gridx = 3;
		gbc_rdbtnIndentNone.gridy = 3;
		add(rdbtnIndentNone, gbc_rdbtnIndentNone);
		indentGroup.add(rdbtnIndentNone);
		
		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("dlgAppSetIDEAutoClose"));
		lblNewLabel_3.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_3.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_3.insets = new Insets(4, 0, 9, 15);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 4;
		add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		chckbxCloseCurly = new JCheckBox("{ }");
		chckbxCloseCurly.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_chckbxCloseCurly = new GridBagConstraints();
		gbc_chckbxCloseCurly.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxCloseCurly.fill = GridBagConstraints.VERTICAL;
		gbc_chckbxCloseCurly.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxCloseCurly.gridx = 1;
		gbc_chckbxCloseCurly.gridy = 4;
		add(chckbxCloseCurly, gbc_chckbxCloseCurly);
		chckbxCloseCurly.setSelected(appProp.getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_CURLY, false));
		
		chckbxClosebracket = new JCheckBox("( )");
		chckbxClosebracket.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_chckbxClosebracket = new GridBagConstraints();
		gbc_chckbxClosebracket.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxClosebracket.fill = GridBagConstraints.VERTICAL;
		gbc_chckbxClosebracket.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxClosebracket.gridx = 2;
		gbc_chckbxClosebracket.gridy = 4;
		add(chckbxClosebracket, gbc_chckbxClosebracket);
		chckbxClosebracket.setSelected(appProp.getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_BRACKET, false));
		
		chckbxCloseSquare = new JCheckBox("[ ]");
		chckbxCloseSquare.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_chckbxCloseSquare = new GridBagConstraints();
		gbc_chckbxCloseSquare.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxCloseSquare.fill = GridBagConstraints.VERTICAL;
		gbc_chckbxCloseSquare.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxCloseSquare.gridx = 3;
		gbc_chckbxCloseSquare.gridy = 4;
		add(chckbxCloseSquare, gbc_chckbxCloseSquare);
		chckbxCloseSquare.setSelected(appProp.getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_SQUARE, false));
		
		chckbxCloseString = new JCheckBox("\" \"");
		chckbxCloseString.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_chckbxCloseString = new GridBagConstraints();
		gbc_chckbxCloseString.fill = GridBagConstraints.VERTICAL;
		gbc_chckbxCloseString.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxCloseString.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxCloseString.gridx = 4;
		gbc_chckbxCloseString.gridy = 4;
		add(chckbxCloseString, gbc_chckbxCloseString);
		chckbxCloseString.setSelected(appProp.getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_STRING, false));
		
		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgAppSetIDEDarkMode"));
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_1.insets = new Insets(4, 0, 9, 15);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 5;
		add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		chcDarkMode = new JCheckBox();
		chcDarkMode.setSelected(appProp.getBoolProperty(ScannerProperties.PROP_IDE_DARK, false));
		GridBagConstraints gbc_chcDarkMode = new GridBagConstraints();
		gbc_chcDarkMode.fill = GridBagConstraints.VERTICAL;
		gbc_chcDarkMode.insets = new Insets(0, 0, 5, 5);
		gbc_chcDarkMode.anchor = GridBagConstraints.NORTHWEST;
		gbc_chcDarkMode.gridx = 1;
		gbc_chcDarkMode.gridy = 5;
		add(chcDarkMode, gbc_chcDarkMode);
		
		JLabel lblNewLabelMsg = new JLabel(LABELS.getString("dlgAppSetIDEMsg"));
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabelMsg = new GridBagConstraints();
		gbc_lblNewLabelMsg.gridwidth = 5;
		gbc_lblNewLabelMsg.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabelMsg.weighty = 1.0;
		gbc_lblNewLabelMsg.insets = new Insets(20, 0, 0, 15);
		gbc_lblNewLabelMsg.gridx = 0;
		gbc_lblNewLabelMsg.gridy = 6;
		add(lblNewLabelMsg, gbc_lblNewLabelMsg);
	}
	
	public void store(AppProperties appProp) {
		appProp.setIntProperty(ScannerProperties.PROP_IDE_TAB_SIZE, tabSize.getIntValue());
		appProp.setIntProperty(ScannerProperties.PROP_IDE_FONT_SIZE, fontSize.getIntValue());
		if(rdbtnIndentNone.isSelected()) {
			appProp.setProperty(ScannerProperties.IDE_AUTOINDENT, "NO");
		} else if(rdbtnIndentYes.isSelected()) {
			appProp.setProperty(ScannerProperties.IDE_AUTOINDENT, "YES");
		} else if(rdbtnIndentSmart.isSelected()) {
			appProp.setProperty(ScannerProperties.IDE_AUTOINDENT, "SMART");
		}
		appProp.setBoolProperty(ScannerProperties.IDE_AUTOCLOSE_CURLY, chckbxCloseCurly.isSelected());
		appProp.setBoolProperty(ScannerProperties.IDE_AUTOCLOSE_BRACKET, chckbxClosebracket.isSelected());
		appProp.setBoolProperty(ScannerProperties.IDE_AUTOCLOSE_SQUARE, chckbxCloseSquare.isSelected());
		appProp.setBoolProperty(ScannerProperties.IDE_AUTOCLOSE_STRING, chckbxCloseString.isSelected());
		appProp.setBoolProperty(ScannerProperties.PROP_IDE_DARK, chcDarkMode.isSelected());
	}
}