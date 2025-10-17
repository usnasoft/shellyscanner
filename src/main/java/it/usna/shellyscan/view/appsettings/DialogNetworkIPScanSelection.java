package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.Inet4Address;
import java.net.InetAddress;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.IPCollection;
import it.usna.shellyscan.view.util.IntegerTextFieldPanel;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.util.AppProperties;

public class DialogNetworkIPScanSelection extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final String IPV4_REGEX_3 = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){2}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

	JTextField[] baseIP = new JTextField[10];
	IntegerTextFieldPanel[] firstIP = new IntegerTextFieldPanel[10];
	IntegerTextFieldPanel[] lastIP = new IntegerTextFieldPanel[10];
	
	private final AppProperties appProp = ScannerProperties.instance();

	public DialogNetworkIPScanSelection(final JDialog owner) {
		super(owner, LABELS.getString("dlgAppSetIPScan"), true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		getContentPane().setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane);

		JPanel panel = new JPanel();
		scrollPane.setViewportView(panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0};
		gbl_panel.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);

		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgAppSetIPBase"));
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(5, 10, 10, 5);
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 0;
		panel.add(lblNewLabel_1, gbc_lblNewLabel_1);

		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("dlgAppSetIPFirst"));
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(5, 2, 10, 5);
		gbc_lblNewLabel_2.gridx = 1;
		gbc_lblNewLabel_2.gridy = 0;
		panel.add(lblNewLabel_2, gbc_lblNewLabel_2);

		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("dlgAppSetIPLast"));
		lblNewLabel_3.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.insets = new Insets(5, 2, 10, 0);
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.gridx = 2;
		gbc_lblNewLabel_3.gridy = 0;
		panel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		String baseIPProp = appProp.getProperty(ScannerProperties.BASE_SCAN_IP + "0");
		if(baseIPProp == null) {
			try {
				baseIPProp = ((Inet4Address)InetAddress.getLocalHost()).getHostAddress(); // I want an exception in case of IPV6
				baseIPProp = baseIPProp.substring(0, baseIPProp.lastIndexOf('.'));
			} catch (/*UnknownHost*/Exception e) {
				baseIPProp = "";
			}
		}
		
		baseIP[0] = new JTextField(baseIPProp);
		GridBagConstraints gbc_baseIP = new GridBagConstraints();
		gbc_baseIP.insets = new Insets(0, 10, 5, 20);
		gbc_baseIP.anchor = GridBagConstraints.WEST;
		gbc_baseIP.gridx = 0;
		gbc_baseIP.gridy = 1;
		panel.add(baseIP[0], gbc_baseIP);
		baseIP[0].setColumns(11);

		firstIP[0] = new IntegerTextFieldPanel(appProp.getIntProperty(ScannerProperties.FIRST_SCAN_IP + "0", ScannerProperties.FIST_SCAN_IP_DEFAULT), 0, 255, false);
		GridBagConstraints gbc_firstIP = new GridBagConstraints();
		gbc_firstIP.anchor = GridBagConstraints.WEST;
		gbc_firstIP.insets = new Insets(0, 0, 5, 20);
		gbc_firstIP.gridx = 1;
		gbc_firstIP.gridy = 1;
		panel.add(firstIP[0], gbc_firstIP);
		firstIP[0].setColumns(3);

		lastIP[0] = new IntegerTextFieldPanel(appProp.getIntProperty(ScannerProperties.LAST_SCAN_IP + "0", ScannerProperties.LAST_SCAN_IP_DEFAULT), 0, 255, false);
		GridBagConstraints gbc_lastIP = new GridBagConstraints();
		gbc_lastIP.insets = new Insets(0, 0, 5, 10);
		gbc_lastIP.anchor = GridBagConstraints.WEST;
		gbc_lastIP.gridx = 2;
		gbc_lastIP.gridy = 1;
		panel.add(lastIP[0], gbc_lastIP);
		lastIP[0].setColumns(3);

		for(int i = 1; i < baseIP.length; i++) {
			row(panel, i);
		}
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.SOUTH);
		JButton btnOk = new JButton(LABELS.getString("dlgOK"));
		btnOk.addActionListener(e -> dispose());
		panel_1.add(btnOk);

		pack();
		setLocationRelativeTo(owner);
	}

	private void row(JPanel panel, int row) {
		baseIP[row] = new JTextField(appProp.getProperty(ScannerProperties.BASE_SCAN_IP + row));
		GridBagConstraints gbc_baseIP = new GridBagConstraints();
		gbc_baseIP.insets = new Insets(0, 10, 5, 20);
		gbc_baseIP.anchor = GridBagConstraints.WEST;
		gbc_baseIP.gridx = 0;
		gbc_baseIP.gridy = row + 1;
		panel.add(baseIP[row], gbc_baseIP);
		baseIP[row].setColumns(11);

		String val;
		firstIP[row] = new IntegerTextFieldPanel((val = appProp.getProperty(ScannerProperties.FIRST_SCAN_IP + row)) == null ? null : Integer.valueOf(val), 0, 255, true);
		GridBagConstraints gbc_firstIP = new GridBagConstraints();
		gbc_firstIP.anchor = GridBagConstraints.WEST;
		gbc_firstIP.insets = new Insets(0, 0, 5, 20);
		gbc_firstIP.gridx = 1;
		gbc_firstIP.gridy = row + 1;
		panel.add(firstIP[row], gbc_firstIP);
		firstIP[row].setColumns(3);

		lastIP[row] = new IntegerTextFieldPanel((val = appProp.getProperty(ScannerProperties.LAST_SCAN_IP + row)) == null ? null : Integer.valueOf(val), 0, 255, true);
		GridBagConstraints gbc_lastIP = new GridBagConstraints();
		gbc_lastIP.insets = new Insets(0, 0, 5, 10);
		gbc_lastIP.anchor = GridBagConstraints.WEST;
		gbc_lastIP.gridx = 2;
		gbc_lastIP.gridy = row + 1;
		panel.add(lastIP[row], gbc_lastIP);
		lastIP[row].setColumns(3);
		
		JButton erase = new JButton(new UsnaAction(null, null, "/images/erase-9-16.png", e -> {
			baseIP[row].setText("");
			firstIP[row].setValue(null);
			lastIP[row].setValue(null);
		}));
		erase.setBorder(BorderFactory.createEmptyBorder());
		erase.setContentAreaFilled(false);
		erase.setFocusable(false);

		GridBagConstraints gbc_lastErase = new GridBagConstraints();
		gbc_lastErase.insets = new Insets(0, 0, 5, 10);
		gbc_lastErase.anchor = GridBagConstraints.WEST;
		gbc_lastErase.gridx = 3;
		gbc_lastErase.gridy = row + 1;
		panel.add(erase, gbc_lastErase);;
	}
	
	@Override
	public void dispose() {
		if(validateData()) {
			super.dispose();
		}
	}
	
	public boolean validateData() {
		String ip = baseIP[0].getText();
		if(ip.isEmpty() || ip.matches(IPV4_REGEX_3) == false) {
			JOptionPane.showMessageDialog(this, LABELS.getString("dlgAppSetScanNetworWrongBase"), LABELS.getString("dlgAppSetTitle"), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		for(int i = 1; i < 10; i++) {
			ip = baseIP[i].getText();
			if(ip.isEmpty() == false && (ip.matches(IPV4_REGEX_3) == false || firstIP[i].getText().isEmpty() || lastIP[i].getText().isEmpty())) {
				JOptionPane.showMessageDialog(this, LABELS.getString("dlgAppSetScanNetworWrongBase"), LABELS.getString("dlgAppSetTitle"), JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}
	
	public void store(Devices model) {
		IPCollection ipCollecton = new IPCollection();
		boolean change = false;
		for(int i = 0; i < 10; i++) {
			String ip = baseIP[i].getText();
			if(ip.isEmpty() == false) {
				change |= appProp.changeProperty(ScannerProperties.BASE_SCAN_IP + i, ip);
				change |= appProp.changeProperty(ScannerProperties.FIRST_SCAN_IP + i, firstIP[i].getText());
				change |= appProp.changeProperty(ScannerProperties.LAST_SCAN_IP + i, lastIP[i].getText());
				ipCollecton.add(ip, appProp.getIntProperty(ScannerProperties.FIRST_SCAN_IP + i), appProp.getIntProperty(ScannerProperties.LAST_SCAN_IP + i));
			} else {
				change |= appProp.remove(ScannerProperties.BASE_SCAN_IP + i) != null;
				change |= appProp.remove(ScannerProperties.FIRST_SCAN_IP + i) != null;
				change |= appProp.remove(ScannerProperties.LAST_SCAN_IP + i) != null;
			}
		}
		if(change) {
			model.setIPInterval(ipCollecton);
		}
	}

//	public static void main(String ...strings) throws IOException {
//		ScannerProperties.init(System.getProperty("user.home") + File.separator + ".shellyScanner").load(true);
//		DialogNetworkIPScanSelection p = new DialogNetworkIPScanSelection(null);
//		p.setVisible(true);
//	}
}