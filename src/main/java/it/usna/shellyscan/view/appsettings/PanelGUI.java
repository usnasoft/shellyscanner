package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import it.usna.shellyscan.view.DevicesTable;
import it.usna.util.AppProperties;

public class PanelGUI extends JPanel {
	private static final long serialVersionUID = 1L;
	private static int viewCol[];
	JTextField csvTextField;
	JRadioButton detailsButton;
	JRadioButton rdbtnDetailedViewFull;
	JRadioButton rdbtnDetailedViewAsIs;
	JRadioButton rdbtnNDetailedViewEstimate;
	
	PanelGUI(DevicesTable devTable, final AppProperties appProp) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 1.0, 2.0};
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 0};
//		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
//		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE, 0.0, 0.0};
		setLayout(gridBagLayout);
		
		JLabel label = new JLabel(LABELS.getString("dlgAppSetDoubleClickLabel"));
		label.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.anchor = GridBagConstraints.WEST;
		gbc_label.insets = new Insets(0, 0, 5, 5);
		gbc_label.gridx = 0;
		gbc_label.gridy = 0;
		add(label, gbc_label);
		
		detailsButton = new JRadioButton(LABELS.getString("dlgAppSetDetails"));
		GridBagConstraints gbc_detailsButton = new GridBagConstraints();
		gbc_detailsButton.anchor = GridBagConstraints.WEST;
		gbc_detailsButton.insets = new Insets(0, 0, 5, 5);
		gbc_detailsButton.gridx = 1;
		gbc_detailsButton.gridy = 0;
		add(detailsButton, gbc_detailsButton);
		
		JRadioButton webUIButton = new JRadioButton(LABELS.getString("dlgAppSetWEBUI"));
		GridBagConstraints gbc_webUIButton = new GridBagConstraints();
		gbc_webUIButton.anchor = GridBagConstraints.WEST;
		gbc_webUIButton.insets = new Insets(0, 0, 5, 0);
		gbc_webUIButton.gridx = 2;
		gbc_webUIButton.gridy = 0;
		add(webUIButton, gbc_webUIButton);
		
		ButtonGroup dClockGroup = new ButtonGroup();
		dClockGroup.add(detailsButton);
		dClockGroup.add(webUIButton);
		if(appProp.getProperty(DialogAppSettings.PROP_DCLICK_ACTION, DialogAppSettings.PROP_DCLICK_ACTION_DEFAULT).equals("DET")) {
			detailsButton.setSelected(true);
		} else  {
			webUIButton.setSelected(true);
		}
		
		JSeparator separator_1_2 = new JSeparator();
		GridBagConstraints gbc_separator_1_2 = new GridBagConstraints();
		gbc_separator_1_2.insets = new Insets(0, 0, 5, 0);
		gbc_separator_1_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1_2.gridwidth = 4;
		gbc_separator_1_2.gridx = 0;
		gbc_separator_1_2.gridy = 1;
		add(separator_1_2, gbc_separator_1_2);
		
		JLabel lblNewLabel_10 = new JLabel(LABELS.getString("dlgAppSetDetailedViewLabel"));
		lblNewLabel_10.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_10 = new GridBagConstraints();
		gbc_lblNewLabel_10.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_10.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_10.gridx = 0;
		gbc_lblNewLabel_10.gridy = 2;
		add(lblNewLabel_10, gbc_lblNewLabel_10);
		
		rdbtnDetailedViewFull = new JRadioButton(LABELS.getString("dlgAppSetDetailedViewFull"));
		GridBagConstraints gbc_rdbtnDetailedViewFull = new GridBagConstraints();
		gbc_rdbtnDetailedViewFull.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDetailedViewFull.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnDetailedViewFull.gridx = 1;
		gbc_rdbtnDetailedViewFull.gridy = 2;
		add(rdbtnDetailedViewFull, gbc_rdbtnDetailedViewFull);
		
		rdbtnDetailedViewAsIs = new JRadioButton(LABELS.getString("dlgAppSetDetailedAsIs"));
		GridBagConstraints gbc_rdbtnDetailedViewAsIs = new GridBagConstraints();
		gbc_rdbtnDetailedViewAsIs.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDetailedViewAsIs.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnDetailedViewAsIs.gridx = 2;
		gbc_rdbtnDetailedViewAsIs.gridy = 2;
		add(rdbtnDetailedViewAsIs, gbc_rdbtnDetailedViewAsIs);
		
		JRadioButton rdbtnDetailedViewHorizontal = new JRadioButton(LABELS.getString("dlgAppSetDetailedHorizintal"));
		GridBagConstraints gbc_rdbtnDetailedViewHorizontal = new GridBagConstraints();
		gbc_rdbtnDetailedViewHorizontal.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDetailedViewHorizontal.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnDetailedViewHorizontal.gridx = 1;
		gbc_rdbtnDetailedViewHorizontal.gridy = 3;
		add(rdbtnDetailedViewHorizontal, gbc_rdbtnDetailedViewHorizontal);
		
		rdbtnNDetailedViewEstimate = new JRadioButton(LABELS.getString("dlgAppSetDetailedCompute"));
		GridBagConstraints gbc_rdbtnNDetailedViewEstimate = new GridBagConstraints();
		gbc_rdbtnNDetailedViewEstimate.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNDetailedViewEstimate.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnNDetailedViewEstimate.gridx = 2;
		gbc_rdbtnNDetailedViewEstimate.gridy = 3;
		add(rdbtnNDetailedViewEstimate, gbc_rdbtnNDetailedViewEstimate);
		
		ButtonGroup detailedViewGroup = new ButtonGroup();
		detailedViewGroup.add(rdbtnDetailedViewFull);
		detailedViewGroup.add(rdbtnDetailedViewAsIs);
		detailedViewGroup.add(rdbtnDetailedViewHorizontal);
		detailedViewGroup.add(rdbtnNDetailedViewEstimate);
		String detailsViewMode = appProp.getProperty(DialogAppSettings.PROP_DETAILED_VIEW_SCREEN, DialogAppSettings.PROP_DETAILED_VIEW_SCREEN_DEFAULT);
		if(detailsViewMode.equals(DialogAppSettings.PROP_DETAILED_VIEW_SCREEN_FULL)) {
			rdbtnDetailedViewFull.setSelected(true);
		} else if(detailsViewMode.equals(DialogAppSettings.PROP_DETAILED_VIEW_SCREEN_HORIZONTAL)) {
			rdbtnDetailedViewHorizontal.setSelected(true);
		} else if(detailsViewMode.equals(DialogAppSettings.PROP_DETAILED_VIEW_SCREEN_ESTIMATE)) {
			rdbtnNDetailedViewEstimate.setSelected(true);
		} else {
			rdbtnDetailedViewAsIs.setSelected(true);
		}

		JSeparator separator_1_1 = new JSeparator();
		GridBagConstraints gbc_separator_1_1 = new GridBagConstraints();
		gbc_separator_1_1.insets = new Insets(0, 0, 5, 0);
		gbc_separator_1_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1_1.gridwidth = 4;
		gbc_separator_1_1.gridx = 0;
		gbc_separator_1_1.gridy = 4;
		add(separator_1_1, gbc_separator_1_1);
		
		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgAppSetCSV"));
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 5;
		add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		csvTextField = new JTextField(appProp.getProperty(DialogAppSettings.PROP_CSV_SEPARATOR, DialogAppSettings.PROP_CSV_SEPARATOR_DEFAULT));
		GridBagConstraints gbc_csvTextField = new GridBagConstraints();
		gbc_csvTextField.anchor = GridBagConstraints.WEST;
		gbc_csvTextField.insets = new Insets(0, 0, 5, 5);
		gbc_csvTextField.gridx = 1;
		gbc_csvTextField.gridy = 5;
		add(csvTextField, gbc_csvTextField);
		csvTextField.setColumns(2);

		JSeparator separator_3 = new JSeparator();
		GridBagConstraints gbc_separator_3 = new GridBagConstraints();
		gbc_separator_3.insets = new Insets(0, 0, 5, 0);
		gbc_separator_3.gridwidth = 4;
		gbc_separator_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_3.gridx = 0;
		gbc_separator_3.gridy = 6;
		add(separator_3, gbc_separator_3);
		
		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("dlgAppSetLblColums"));
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 7;
		add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		JPanel columnsPanel = getColumnsPanel(devTable);
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.anchor = GridBagConstraints.NORTH;
		gbc_panel.weighty = 1.0;
		gbc_panel.gridwidth = 4;
		gbc_panel.insets = new Insets(0, 10, 0, 5);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 8;
		add(columnsPanel, gbc_panel);
	}
	
	private static JPanel getColumnsPanel(DevicesTable devTable) {
		JPanel columnsPanel = new JPanel();
		columnsPanel.setLayout(new GridLayout(0, 5, 1, 3));
		for(int col = 0; col < devTable.getModel().getColumnCount(); col++) {
			columnsPanel.add(generateCheckbox(devTable, col));
		}
		JButton btnRestoreColumns = new JButton(LABELS.getString("dlgAppSetLblReset"));
		btnRestoreColumns.addActionListener(event -> {
			devTable.restoreColumns();
			for(Component c: columnsPanel.getComponents()) {
				if(c instanceof JCheckBox) {
					((JCheckBox)c).setSelected(true);
				}
			}
			devTable.columnsWidthAdapt();
		});
		JPanel resetPanel = new JPanel(new BorderLayout());
		resetPanel.add(btnRestoreColumns, BorderLayout.WEST);
		columnsPanel.add(resetPanel);
		btnRestoreColumns.setBorder(new EmptyBorder(3, 15, 3, 15));
		return columnsPanel;
	}
	
	private static JCheckBox generateCheckbox(DevicesTable devTable, int col)  {
		String name = devTable.getModel().getColumnName(col);
		JCheckBox chk = new JCheckBox(name.length() > 0 ? name : LABELS.getString("col_status_exp"), devTable.isColumnVisible(col));
		chk.addActionListener(e -> { //	chk.addItemListener(e -> { // questo fa scattare l'evento su setSelected(...) io non voglio
			if(viewCol == null) {
				viewCol = new int[devTable.getModel().getColumnCount()];
				Arrays.fill(viewCol, -1);
			}	
			if(chk.isSelected()) {
				devTable.showColumn(col, viewCol[col]);
			} else {
				viewCol[col] = devTable.hideColumn(col);
			}
			devTable.resetRowsComputedHeight();
			devTable.columnsWidthAdapt();
		});
		return chk;
	}
}