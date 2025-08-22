package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.UsnaEventListener;

public class DialogDeviceSettings extends JDialog implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	enum Gen {G1, G2, MIX};

	private JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private JButton btnClose = new JButton(LABELS.getString("dlgClose"));
	private JButton btnOKButton = new JButton(LABELS.getString("dlgApply"));
	private JButton btnApplyClose = new JButton(LABELS.getString("dlgApplyClose"));
	private Thread showCurrentThread;
	private AbstractSettingsPanel currentPanel = null;
	
	public static final int FW = 0;
	public static final int WIFI1 = 1;
	public static final int WIFI2 = 2;
	
	private Devices model;
	private int[] devicesInd;

	/**
	 * @wbp.parser.constructor
	 */
	public DialogDeviceSettings(final Frame owner, Devices model, int[] devicesInd) {
		super(owner, false);
		init(owner, model, devicesInd, FW);
	}
	
	public DialogDeviceSettings(final JDialog owner, Devices model, int[] devicesInd, int defaultPanel) {
		super(owner, false);
		init(owner, model, devicesInd, defaultPanel);
	}

	private void init(final Window owner, Devices model, int[] devicesInd, int defaultPanel) {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.model = model;
		this.devicesInd = devicesInd;
		model.addListener(this);

		if(devicesInd.length == 1) {
			setTitle(String.format(LABELS.getString("dlgSetTitle1"), UtilMiscellaneous.getDescName(getLocalDevice(0))));
		} else {
			setTitle(String.format(LABELS.getString("dlgSetTitleMany"), devicesInd.length));
		}

		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(5);

		this.setSize(610, 420);
		setLocationRelativeTo(owner);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 3));
//		btnPanel.setBackground(Main.STATUS_LINE_COLOR);
		getContentPane().add(btnPanel, BorderLayout.SOUTH);
		Gen devTypes = getTypes();
		PanelFWUpdate panelFW = new PanelFWUpdate(this);
		tabbedPane.add(LABELS.getString("dlgSetFWUpdate"), panelFW);
		PanelWIFI panelWIFI1 = new PanelWIFI(this, WIFIManager.Network.PRIMARY);
		tabbedPane.add(LABELS.getString("dlgSetWIFI1"), panelWIFI1);
		PanelWIFI panelWIFI2 = new PanelWIFI(this, WIFIManager.Network.SECONDARY);
		tabbedPane.add(LABELS.getString("dlgSetWIFIBackup"), panelWIFI2);
		PanelResLogin panelResLogin = new PanelResLogin(this, devTypes);
		tabbedPane.add(LABELS.getString("dlgSetRestrictedLogin"), panelResLogin);
		final AbstractSettingsPanel panelMQTT;
		if(devTypes == Gen.G1 && existsOffLine() == false) { // existsOffLine() == false -> deferred mqtt on PanelMQTTMix only
			panelMQTT = new PanelMQTTG1(this);
		} else if(devTypes == Gen.G2 && existsOffLine() == false) {
			panelMQTT = new PanelMQTTG2(this);
		} else /*if(devTypes == Gen.MIX || existsOffLine())*/{
			panelMQTT = new PanelMQTTMix(this);
		}
		tabbedPane.add(LABELS.getString("dlgSetMQTT"), panelMQTT);
		PanelOthers others = new PanelOthers(this);
		tabbedPane.add(LABELS.getString("dlgSetOthers"), others);

		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		btnClose.addActionListener(event -> dispose());
		btnOKButton.addActionListener(event -> {
			SwingUtilities.invokeLater(() -> apply(((AbstractSettingsPanel)tabbedPane.getSelectedComponent()), tabbedPane.getTitleAt(tabbedPane.getSelectedIndex())) );
		});
		btnApplyClose.addActionListener(event -> {
			SwingUtilities.invokeLater(() -> {
				if(apply(((AbstractSettingsPanel)tabbedPane.getSelectedComponent()), tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()))) {
					btnClose.doClick();
				}
			});
		});
		btnPanel.add(btnOKButton);
		btnPanel.add(btnApplyClose);
		btnPanel.add(btnClose);
		
		tabbedPane.setSelectedIndex(defaultPanel);

		tabbedPane.addChangeListener(e -> {
			showCurrent();
			try {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			} catch (InterruptedException e1) {}
		});
		setVisible(true);
		showCurrent();
	}
	
	@Override
	public void dispose() {
		model.removeListener(this);
		if(currentPanel != null) {
			currentPanel.hiding();
		}
		super.dispose();
	}

	private synchronized void showCurrent() {
		if(showCurrentThread != null) {
			showCurrentThread.interrupt();
		}
		btnOKButton.setEnabled(false);
		btnApplyClose.setEnabled(false);
		if(currentPanel != null) {
			currentPanel.hiding();
		}
		currentPanel = ((AbstractSettingsPanel)tabbedPane.getSelectedComponent());
		showCurrentThread = new Thread(() -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				btnClose.requestFocus(); // remove focus from form element, it could be disabled now
				String msg = currentPanel.showing();
				if(Thread.interrupted() == false) {
					if(isVisible() && currentPanel.isVisible() && msg != null && msg.length() > 0) {
						Msg.errorMsg(this, msg);
					} else { //if(msg == null) { // "" -> panel displayed his own message
						btnOKButton.setEnabled(true);
						btnApplyClose.setEnabled(true);
					}
				}
			} catch (InterruptedException e) {
			} finally {
				DialogDeviceSettings.this.setCursor(Cursor.getDefaultCursor());
			}
		});
		showCurrentThread.start();
	}
	
	private boolean apply(AbstractSettingsPanel panel, String name) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			String msg = panel.apply();
			firePropertyChange("S_APPLY", null, null);
			if(msg != null && msg.length() > 0) {
				Msg.showHtmlMessageDialog(this, msg, name, JOptionPane.INFORMATION_MESSAGE);
			}
			return true;
		} catch(IllegalArgumentException e) {
			Msg.errorMsg(this, e.getMessage());
			return false;
		} catch(Exception e) {
			Msg.errorMsg(this, e);
			return false;
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}
	
	ShellyAbstractDevice getLocalDevice(int index) {
		return model.get(devicesInd[index]);
	}
	
	int getLocalIndex(int modelIndex) {
		for(int i = 0; i < devicesInd.length; i++) {
			if(devicesInd[i] == modelIndex) {
				return i;
			}
		}
		return -1;
	}
	
	int getModelIndex(int localIndex) {
		return devicesInd[localIndex];
	}
	
	int getLocalSize() {
		return devicesInd.length;
	}
	
	Devices getModel() {
		return model;
	}
	
	private Gen getTypes() {
		Gen r = null;
		for(int index: devicesInd) {
			ShellyAbstractDevice d =  model.get(index);
			if(d instanceof GhostDevice) {
				return Gen.MIX; // actually unknown
			} else if(r == null) {
				r = d instanceof AbstractG2Device ? Gen.G2 : Gen.G1;
			} else if(d instanceof AbstractG2Device) {
				if(r != Gen.G2) {
					return Gen.MIX;
				}
			} else if(d instanceof AbstractG1Device) {
				if(r != Gen.G1) {
					return Gen.MIX;
				}
			}
		}
		return r;
	}
	
	private boolean existsOffLine() {
		for(int index: devicesInd) {
			Status status =  model.get(index).getStatus();
			if(status == Status.GHOST || status == Status.OFF_LINE) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void update(EventType mesgType, Integer pos) {
		if(mesgType == Devices.EventType.CLEAR) {
			SwingUtilities.invokeLater(() -> dispose()); // devicesInd changes
		}
	}
}