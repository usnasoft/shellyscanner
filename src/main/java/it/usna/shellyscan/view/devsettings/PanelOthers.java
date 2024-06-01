package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.model.device.DeviceOfflineException;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;

public class PanelOthers extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	
	private final static Logger LOG = LoggerFactory.getLogger(PanelOthers.class);
	private JTextField ntpServerTextField;

	protected PanelOthers(DialogDeviceSettings parent) {
		super(parent);
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		scrollPane.setViewportView(contentPanel);
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgNTPServer"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		contentPanel.add(lblNewLabel, gbc_lblNewLabel);
		
		ntpServerTextField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		contentPanel.add(ntpServerTextField, gbc_textField);
		ntpServerTextField.setColumns(30);
	}

	@Override
	String showing() throws InterruptedException {
		ntpServerTextField.setEnabled(false);
		ShellyAbstractDevice d = null;
		String sntpServerGlobal = "";
		boolean first = true;
		for(int i = 0; i < parent.getLocalSize(); i++) {
			d = parent.getLocalDevice(i);
			if(Thread.interrupted()) {
				throw new InterruptedException();
			}
			try {
				String ntpServer = d.getTimeAndLocationManager().getSNTPServer();
				if(first) {
					sntpServerGlobal = ntpServer;
					first = false;
				} else {
					if(ntpServer == null || ntpServer.equals(sntpServerGlobal) == false) sntpServerGlobal = "";
				}
			} catch (DeviceOfflineException | UnsupportedOperationException e) {
				LOG.debug("PanelOthers.showing offline {}", d.getHostname());
			} catch (IOException | RuntimeException e) {
				LOG.error("PanelOthers.showing", e);
			}
		}
		ntpServerTextField.setEnabled(true); // form is now active
		ntpServerTextField.setText(sntpServerGlobal);
		return null;
	}

	@Override
	String apply() {
		final String server = ntpServerTextField.getText().trim();
		if(server.isEmpty()) {
			throw new IllegalArgumentException(LABELS.getString("dlgNTPServerEmptyError"));
		}
		String res = "<html>";
		for(int i = 0; i < parent.getLocalSize(); i++) {
			final ShellyAbstractDevice device = parent.getLocalDevice(i);
			if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
				res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
				DeferrablesContainer dc = DeferrablesContainer.getInstance();
				dc.addOrUpdate(parent.getModelIndex(i), DeferrableTask.Type.SNTP, LABELS.getString("dlgNTPServer"), (def, dev) -> {
					return dev.getTimeAndLocationManager().setSNTPServer(server);
				});
			} else {
				String msg = device.getTimeAndLocationManager().setSNTPServer(server);
				if(msg != null) {
					if(LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + " (" + msg + ")<br>";
				} else {
					res += String.format(LABELS.getString("dlgSetMultiMsgOk"), device.getHostname()) + "<br>";
				}
			}
		}
		try {
			showing();
		} catch (InterruptedException e) {}
		return res;
	}
}