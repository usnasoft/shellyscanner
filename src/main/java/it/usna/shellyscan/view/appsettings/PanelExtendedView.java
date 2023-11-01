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
import javax.swing.border.EmptyBorder;

import it.usna.shellyscan.view.DevicesTable;
import it.usna.util.AppProperties;

public class PanelExtendedView extends JPanel {
	private static final long serialVersionUID = 1L;
	private static int viewCol[];
	JRadioButton rdbtnDetailedViewFull, rdbtnDetailedViewAsIs, rdbtnNDetailedViewEstimate, rdbtnDetailedViewHorizontal;
	
	PanelExtendedView(DevicesTable devTable, final AppProperties appProp) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{0.1, 0.0, 0.0, 1.0, 0.5};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel_10 = new JLabel(LABELS.getString("dlgAppSetDetailedViewLabel"));
		lblNewLabel_10.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_10 = new GridBagConstraints();
		gbc_lblNewLabel_10.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_10.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_10.gridx = 0;
		gbc_lblNewLabel_10.gridy = 0;
		add(lblNewLabel_10, gbc_lblNewLabel_10);
		
		rdbtnDetailedViewFull = new JRadioButton(LABELS.getString("dlgAppSetDetailedViewFull"));
		GridBagConstraints gbc_rdbtnDetailedViewFull = new GridBagConstraints();
		gbc_rdbtnDetailedViewFull.gridwidth = 2;
		gbc_rdbtnDetailedViewFull.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDetailedViewFull.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnDetailedViewFull.gridx = 1;
		gbc_rdbtnDetailedViewFull.gridy = 0;
		add(rdbtnDetailedViewFull, gbc_rdbtnDetailedViewFull);
		
		rdbtnDetailedViewAsIs = new JRadioButton(LABELS.getString("dlgAppSetDetailedAsIs"));
		GridBagConstraints gbc_rdbtnDetailedViewAsIs = new GridBagConstraints();
		gbc_rdbtnDetailedViewAsIs.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDetailedViewAsIs.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnDetailedViewAsIs.gridx = 3;
		gbc_rdbtnDetailedViewAsIs.gridy = 0;
		add(rdbtnDetailedViewAsIs, gbc_rdbtnDetailedViewAsIs);
		
		rdbtnDetailedViewHorizontal = new JRadioButton(LABELS.getString("dlgAppSetDetailedHorizintal"));
		GridBagConstraints gbc_rdbtnDetailedViewHorizontal = new GridBagConstraints();
		gbc_rdbtnDetailedViewHorizontal.gridwidth = 2;
		gbc_rdbtnDetailedViewHorizontal.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDetailedViewHorizontal.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnDetailedViewHorizontal.gridx = 1;
		gbc_rdbtnDetailedViewHorizontal.gridy = 1;
		add(rdbtnDetailedViewHorizontal, gbc_rdbtnDetailedViewHorizontal);
		
		rdbtnNDetailedViewEstimate = new JRadioButton(LABELS.getString("dlgAppSetDetailedCompute"));
		GridBagConstraints gbc_rdbtnNDetailedViewEstimate = new GridBagConstraints();
		gbc_rdbtnNDetailedViewEstimate.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNDetailedViewEstimate.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNDetailedViewEstimate.gridx = 3;
		gbc_rdbtnNDetailedViewEstimate.gridy = 1;
		add(rdbtnNDetailedViewEstimate, gbc_rdbtnNDetailedViewEstimate);
		
		ButtonGroup detailedViewGroup = new ButtonGroup();
		detailedViewGroup.add(rdbtnDetailedViewFull);
		detailedViewGroup.add(rdbtnDetailedViewAsIs);
		detailedViewGroup.add(rdbtnNDetailedViewEstimate);
		detailedViewGroup.add(rdbtnDetailedViewHorizontal);
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

		JSeparator separator_4 = new JSeparator();
		GridBagConstraints gbc_separator_4 = new GridBagConstraints();
		gbc_separator_4.insets = new Insets(0, 0, 5, 0);
		gbc_separator_4.gridwidth = 5;
		gbc_separator_4.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_4.gridx = 0;
		gbc_separator_4.gridy = 2;
		add(separator_4, gbc_separator_4);
		
		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("dlgAppSetLblColums"));
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 3;
		add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		JPanel columnsPanel = getColumnsPanel(devTable);
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.anchor = GridBagConstraints.NORTH;
		gbc_panel.weighty = 1.0;
		gbc_panel.gridwidth = 5;
		gbc_panel.insets = new Insets(0, 10, 0, 5);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 12;
		add(columnsPanel, gbc_panel);
	}
	
	private static JPanel getColumnsPanel(DevicesTable devTable) {
		JPanel columnsPanel = new JPanel();
		columnsPanel.setLayout(new GridLayout(0, 5, 1, 3));
		for(int col = 0; col < devTable.getModel().getColumnCount(); col++) {
			columnsPanel.add(generateCheckbox(devTable, col));
		}
		JButton btnAllColumns = new JButton(LABELS.getString("dlgAppSetLblAll"));
		btnAllColumns.setBorder(new EmptyBorder(3, 15, 3, 15));
		btnAllColumns.addActionListener(event -> {
			devTable.restoreColumns();
			for(Component c: columnsPanel.getComponents()) {
				if(c instanceof JCheckBox box) {
					box.setSelected(true);
				}
			}
			devTable.columnsWidthAdapt();
		});
		JPanel allButtonPanel = new JPanel(new BorderLayout());
		allButtonPanel.add(btnAllColumns, BorderLayout.WEST);
		columnsPanel.add(allButtonPanel);
		return columnsPanel;
	}
	
	private static JCheckBox generateCheckbox(DevicesTable devTable, int col)  {
		String name = devTable.getModel().getColumnName(col);
		JCheckBox chk = new JCheckBox(name.length() > 0 ? name : LABELS.getString("col_status_exp"), devTable.isColumnVisible(col));
//		chk.setName("C" + col);
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