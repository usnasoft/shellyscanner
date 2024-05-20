package it.usna.shellyscan.view.devsettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Font;
import javax.swing.JTextField;
import java.awt.Insets;

public class PanelOthers extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	
	private final static Logger LOG = LoggerFactory.getLogger(PanelOthers.class);
	private JTextField textField;

	protected PanelOthers(DialogDeviceSettings parent) {
		super(parent);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgNTPServer"));
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		
		textField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		add(textField, gbc_textField);
		textField.setColumns(30);
	}

	@Override
	String showing() throws InterruptedException {
		ShellyAbstractDevice d = null;
		for(int i = 0; i < parent.getLocalSize(); i++) {
			d = parent.getLocalDevice(i);
		}
		return null;
	}

	@Override
	String apply() {
		// TODO Auto-generated method stub
		return null;
	}
}

// https://api.shelly.cloud/timezone/tzlist

// Shelly.ListTimezones
