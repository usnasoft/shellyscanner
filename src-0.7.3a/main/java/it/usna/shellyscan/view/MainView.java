package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.function.BiConsumer;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.mvc.singlewindow.MainWindow;
import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.ModelMessage;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.ShellyUnmanagedDevice;
import it.usna.shellyscan.model.device.g1.ShellyDW;
import it.usna.shellyscan.model.device.g1.ShellyFlood;
import it.usna.shellyscan.model.device.g1.modules.LightBulbRGBCommander;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.ActionsCommander;
import it.usna.shellyscan.model.device.modules.RGBWCommander;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RollerCommander;
import it.usna.shellyscan.model.device.modules.WhiteCommander;
import it.usna.swing.UsnaPopupMenu;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.AppProperties;

public class MainView extends MainWindow implements Observer {
	private static final long serialVersionUID = 1L;
	final static ImageIcon ONLINE_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_yes.png"), LABELS.getString("labelDevOnLIne"));
	private final static ImageIcon OFFLINE_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_stop.png"), LABELS.getString("labelDevOffLIne"));
	private final static ImageIcon LOGIN_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_star_yellow.png"), LABELS.getString("labelDevNotLogged"));
	private final static ImageIcon UPDATING_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_refresh.png"), LABELS.getString("labelDevUpdating"));
	private final static ImageIcon ERROR_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_error.png"), LABELS.getString("labelDevError"));
	
	private final static int SHORTCUT_KEY = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	private final static Logger LOG = LoggerFactory.getLogger(MainWindow.class);
	private final static String YES = LABELS.getString("true_yn");
	private final static String NO = LABELS.getString("false_yn");

	private AppProperties appProp;
	private JLabel statusLabel = new JLabel();
	private JTextField textFieldFilter;
	private Devices model;
	private DevicesTable devicesTable;
	private UsnaTableModel tModel = new UsnaTableModel(
			"",
			LABELS.getString("col_device"),
			LABELS.getString("col_device_name"),
			LABELS.getString("col_ip"),
			LABELS.getString("col_rssi"),
			LABELS.getString("col_cloud"),
			LABELS.getString("col_uptime"),
			LABELS.getString("col_intTemp"),
			LABELS.getString("col_debug"),
			LABELS.getString("col_relay"));

	private class UsnaAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		protected ActionListener onActionPerformed;
		
		public UsnaAction(String nameId, String tooltipId, String smallIcon, String largeIcon, final ActionListener a) {
			this(largeIcon, tooltipId, a);
			putValue(NAME, LABELS.getString(nameId));
			if(smallIcon != null) {
				putValue(SMALL_ICON, new ImageIcon(getClass().getResource(smallIcon)));
			}
		}

		public UsnaAction(String icon, String tooltipId, final ActionListener a) {
			if(icon != null) {
				putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(icon)));
			}
			if(tooltipId != null) {
				putValue(SHORT_DESCRIPTION, LABELS.getString(tooltipId));
			}
			this.onActionPerformed = a;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // usefull if SwingUtilities.invokeLater(...) is not used inside "onActionPerformed"
				onActionPerformed.actionPerformed(e);
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	private class ViewSelectedAction extends UsnaAction {
		private static final long serialVersionUID = 1L;
		
		public ViewSelectedAction(String nameId, String tooltipId, String smallIcon, String largeIcon, BiConsumer<Integer, ShellyAbstractDevice> c) {
			this(largeIcon, tooltipId, c);
			putValue(NAME, LABELS.getString(nameId));
			if(smallIcon != null) {
				putValue(SMALL_ICON, new ImageIcon(getClass().getResource(smallIcon)));
			}
		}

		public ViewSelectedAction(String icon, String tooltipId, BiConsumer<Integer, ShellyAbstractDevice> c) {
			super(icon, tooltipId, null);
			onActionPerformed = e -> {
				for(int ind: devicesTable.getSelectedRows()) {
					int modelRow = devicesTable.convertRowIndexToModel(ind);
					ShellyAbstractDevice d = model.get(modelRow);
					c.accept(modelRow, d);
				}
			};
		}
	}

	private Action infoAction = new ViewSelectedAction("action_info_name", "action_info_tooltip", "/images/Bubble3_16.png", "/images/Bubble3.png", (i, d) -> {
		new DialogDeviceInfo(MainView.this, true, d, d.getInfoRequests());
	});

	private Action infoLogAction = new ViewSelectedAction("/images/Document2.png", "action_info_log_tooltip", (i, d) -> {
		if(d instanceof AbstractG2Device) {
			new DialogDeviceLogG2(MainView.this, model, i);
		} else {
			new DialogDeviceInfo(MainView.this, false, d, new String[]{"/debug/log", "/debug/log1"});
		}
	});

	private Action rescanAction = new UsnaAction("/images/73-radar.png", "action_scan_tooltip", e -> {
		statusLabel.setText(LABELS.getString("scanning_start"));
		SwingUtilities.invokeLater(() -> {
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				model.scan();
				Thread.sleep(250); // too many call disturb some devices at least (2.5)
			} catch (IOException e1) {
				Main.errorMsg(e1);
			} catch (InterruptedException e1) {
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
		});
	});

	private Action refreshAction = new UsnaAction("/images/Refresh.png", "action_refresh_tooltip", e -> {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		setEnabled(false);
		devicesTable.stopCellEditing();
		for(int i = 0; i < tModel.getRowCount(); i++) {
			tModel.setValueAt(UPDATING_BULLET, i, DevicesTable.COL_STATUS_IDX);
			model.refresh(i, false);
		}
		try {
			Thread.sleep(250); // too many call disturb some devices at least (2.5)
		} catch (InterruptedException e1) {}
		setEnabled(true);
		setCursor(Cursor.getDefaultCursor());
	});
	
	private Action rebootAction = new UsnaAction("/images/nuke.png", "action_reboot_tooltip"/*"Reboot"*/, e -> {
		final String cancel = UIManager.getString("OptionPane.cancelButtonText");
		if(JOptionPane.showOptionDialog(
				MainView.this, LABELS.getString("action_reboot_confirm"), LABELS.getString("action_reboot_tooltip"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new Object[] {UIManager.getString("OptionPane.yesButtonText"), cancel}, cancel) == 0) {
			for(int ind: devicesTable.getSelectedRows()) {
				int modelRow = devicesTable.convertRowIndexToModel(ind);
				ShellyAbstractDevice d = model.get(modelRow);
				d.setStatus(Status.READING);
				tModel.setValueAt(UPDATING_BULLET, modelRow, DevicesTable.COL_STATUS_IDX);
				SwingUtilities.invokeLater(() -> model.reboot(modelRow));
			}
		}
	});
	
	private Action browseAction = new ViewSelectedAction("action_web_name", "action_web_tooltip", "/images/Computer16.png", "/images/Computer.png", (i, d) -> {
		try {
			Desktop.getDesktop().browse(new URI(d.getHttpHost().getSchemeName() + "://" + d.getHttpHost().getAddress().getHostAddress()));
		} catch (IOException | URISyntaxException e) {
			Main.errorMsg(e);
		}
	});
	
	private Action aboutAction = new UsnaAction("/images/question.png", null/*"About"*/, e -> {
		JEditorPane ep = new JEditorPane("text/html", "<html><h1><font color=#00005a>" + Main.APP_NAME + " " + Main.VERSION + "</h1></font><p>" + LABELS.getString("aboutApp") + "</html>");
		ep.setEditable(false);
		ep.addHyperlinkListener(ev -> {
			try {
				if(ev.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
						Desktop.getDesktop().browse(new URI(ev.getURL().toString()));
					} else {
						JOptionPane.showMessageDialog(this, ev.getURL(), "", JOptionPane.PLAIN_MESSAGE);
					}
				}
			} catch (IOException | URISyntaxException ex) {}
		});
		JOptionPane.showMessageDialog(MainView.this, ep, Main.APP_NAME, JOptionPane.INFORMATION_MESSAGE, new ImageIcon(Main.class.getResource("/images/ShSc.png")));
	});

	private Action backupAction = new UsnaAction("action_back_name", "action_back_tooltip", "/images/Download16.png", "/images/Download.png", e -> {
		final JFileChooser fc = new JFileChooser();
		final String path = appProp.getProperty("LAST_PATH");
		if(path != null) {
			fc.setCurrentDirectory(new File(path));
		}
		int[] ind = devicesTable.getSelectedRows();
		if(ind.length > 1) {
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if(fc.showSaveDialog(MainView.this) == JFileChooser.APPROVE_OPTION) {
				String res = "<html>";
				for(int i: ind) {
					ShellyAbstractDevice d = model.get(devicesTable.convertRowIndexToModel(i));
					try {
						String fileName = d.getHostname().replaceAll("[^\\w_-]+", "_") + ".sbk";
						d.backup(new File(fc.getSelectedFile(), fileName));
						res += String.format(LABELS.getString("dlgSetMultiMsgOk"), d.getHostname()) + "<br>";
					} catch (IOException | RuntimeException e1) {
						res += String.format(LABELS.getString("dlgSetMultiMsgFail"), d.getHostname()) + "<br>";
						LOG.debug("{}", d.getHostname(), e1);
					}
				}
				JOptionPane.showMessageDialog(MainView.this, res, LABELS.getString("titleBackupDone"), JOptionPane.INFORMATION_MESSAGE);
				appProp.setProperty("LAST_PATH", fc.getSelectedFile().getPath());
			}
		} else if(ind.length == 1) {
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), "sbk"));
			ShellyAbstractDevice device = model.get(devicesTable.convertRowIndexToModel(ind[0]));
			String fileName = device.getHostname().replaceAll("[^\\w_-]+", "_") + ".sbk";
			fc.setSelectedFile(new File(fileName));
			if(fc.showSaveDialog(MainView.this) == JFileChooser.APPROVE_OPTION) {
				try {
					device.backup(fc.getSelectedFile());
					JOptionPane.showMessageDialog(MainView.this, String.format("<html>" + LABELS.getString("dlgSetMultiMsgOk"), device.getHostname()), LABELS.getString("titleBackupDone"), JOptionPane.INFORMATION_MESSAGE);
					appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
				} catch (IOException | RuntimeException e1) {
					Main.errorMsg(e1);
				}
			}
		}
	});

	private Action restoreAction = new UsnaAction("action_restore_name", "action_restore_tooltip", "/images/Upload16.png", "/images/Upload.png", e -> {
		int selectedInd = devicesTable.getSelectedRow();
		if(selectedInd >= 0) {
			final JFileChooser fc = new JFileChooser();
			try {
				final String path = appProp.getProperty("LAST_PATH");
				if(path != null) {
					fc.setCurrentDirectory(new File(path));
				}
				fc.setAcceptAllFileFilterUsed(false);
				fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), "sbk"));
				final int modelRow = devicesTable.convertRowIndexToModel(selectedInd);
				final ShellyAbstractDevice device = model.get(modelRow);
				final String fileName = device.getHostname().replaceAll("[^\\w_-]+", "_") + ".sbk";
				fc.setSelectedFile(new File(fileName));
				if(fc.showOpenDialog(MainView.this) == JFileChooser.APPROVE_OPTION) {
					String msg = device.restore(fc.getSelectedFile(), false);
					if(ShellyAbstractDevice.ERR_RESTORE_HOST.equals(msg)) {
						if(JOptionPane.showConfirmDialog(MainView.this, LABELS.getString(ShellyAbstractDevice.ERR_RESTORE_HOST),
								LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
							msg = device.restore(fc.getSelectedFile(), true);
						} else {
							return;
						}
					} 
					device.refreshSettings();
					device.refreshStatus();
					update(null, new ModelMessage(ModelMessage.Type.UPDATE, modelRow));
					if(ShellyAbstractDevice.ERR_RESTORE_MODEL.equals(msg)) {
						Main.errorMsg(LABELS.getString(ShellyAbstractDevice.ERR_RESTORE_MODEL));
					} else if(msg != null && msg.length() > 0) {
						Main.errorMsg(LABELS.containsKey(msg)? LABELS.getString(msg) : msg);
					} else {
						JOptionPane.showMessageDialog(MainView.this, LABELS.getString("msgRestoreSuccess"), device.getHostname(), JOptionPane.INFORMATION_MESSAGE);
					}
					appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getCanonicalPath());
				}
			} catch (FileNotFoundException e1) {
				Main.errorMsg(String.format(LABELS.getString("action_restore_error_file"), fc.getSelectedFile().getName()));
			} catch (IOException | RuntimeException e1) {
				Main.errorMsg(e1);
			}
		}
	});
	
	private Action appSettingsAction = new UsnaAction("/images/Gear.png", "action_appsettings_tooltip", e -> {
		new DialogAppSettings(MainView.this, model, appProp);
	});
	
	@SuppressWarnings("restriction")
	private Action printAction = new UsnaAction("/images/Printer.png", "action_print_tooltip", e -> {
		try {
			devicesTable.clearSelection();
			PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(javax.print.attribute.standard.DialogTypeSelection.NATIVE);
			try {
				aset.add(new sun.print.DialogOwner(MainView.this));
			} catch (Throwable t) {}
			devicesTable.print(JTable.PrintMode.FIT_WIDTH, null, null, true, aset, true);
		} catch (java.awt.print.PrinterException ex) {
			Main.errorMsg(ex);
		}
	});
	
	private Action csvExportAction = new UsnaAction("/images/Table.png", "action_cvs_tooltip", e -> {
		final JFileChooser fc = new JFileChooser();
		final String path = appProp.getProperty("LAST_PATH");
		if(path != null) {
			fc.setCurrentDirectory(new File(path));
		}
		fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_csv_desc"), "csv"));
		if(fc.showSaveDialog(MainView.this) == JFileChooser.APPROVE_OPTION) {
			File out = fc.getSelectedFile();
			if(out.getName().contains(".") == false) {
				out = new File(out.getParentFile(), out.getName() + ".csv");
			}
			try (FileWriter w = new FileWriter(out)) {
				devicesTable.csvExport(w, appProp.getProperty(DialogAppSettings.PROP_CSV_SEPARATOR, DialogAppSettings.PROP_CSV_SEPARATOR_DEFAULT));
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
			appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
		}
	});

	private Action settingsAction = new UsnaAction("/images/Tool.png", "action_general_conf_tooltip", e -> {
		ArrayList<ShellyAbstractDevice> devices = new ArrayList<>();
		for(int ind: devicesTable.getSelectedRows()) {
			int modelRow = devicesTable.convertRowIndexToModel(ind);
			devices.add(model.get(modelRow));
		}
		new DialogDeviceSettings(MainView.this, devices);
	});
	
	private Action loginAction = new ViewSelectedAction("action_nema_login", null, "/images/Key16.png", null, (i, d) -> {
		model.create(d.getHttpHost().getAddress(), d.getHostname());
	});
	
	private Action eraseFilterAction = new UsnaAction("/images/dialog-close.png", null, e -> {
		textFieldFilter.setText("");
		textFieldFilter.requestFocusInWindow();
		devicesTable.clearSelection();
	});

	public MainView(final Devices model, final AppProperties appProp) {
		this.model = model;
		this.appProp = appProp;
		model.addObserver(this);

		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setHgap(2);
		loadProperties(appProp);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(Main.ICON)));
		setTitle(Main.APP_NAME + " v." + Main.VERSION);

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout(0, 0));
		statusPanel.add(statusLabel, BorderLayout.WEST);
		JPanel statusButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		statusButtonPanel.setOpaque(false);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("lblFilter"));
		statusButtonPanel.add(lblNewLabel);
		
		textFieldFilter = new JTextField();
		textFieldFilter.setBorder(new EmptyBorder(0, 0, 0, 0));
		statusButtonPanel.add(textFieldFilter);
		textFieldFilter.setColumns(16);
		textFieldFilter.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				devicesTable.setRowFilter(textFieldFilter.getText());
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				devicesTable.setRowFilter(textFieldFilter.getText());
			}
		});
		
		JButton eraseFilterButton = new JButton(eraseFilterAction);
		eraseFilterButton.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		eraseFilterButton.setContentAreaFilled(false);
		statusButtonPanel.add(eraseFilterButton);
		eraseFilterButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, SHORTCUT_KEY), "find_erase");
		eraseFilterButton.getActionMap().put("find_erase", eraseFilterAction);
		
		JButton btnSelectAll = new JButton(LABELS.getString("labelSelectAll"));
		btnSelectAll.addActionListener(event -> devicesTable.selectAll());

		btnSelectAll.setBorder(new EmptyBorder(2, 7, 2, 7));
		statusButtonPanel.add(btnSelectAll);
		
		JButton btnSelectOnline = new JButton(LABELS.getString("labelSelectOnLine"));
		btnSelectOnline.addActionListener(event -> {
			devicesTable.clearSelection();
			for(int i = 0; i < model.size(); i++) {
				int row = devicesTable.convertRowIndexToView(i);
				if(row >= 0 && model.get(i).getStatus() == Status.ON_LINE) {
					devicesTable.addRowSelectionInterval(row, row);
				}
			}
		});
		btnSelectOnline.setBorder(new EmptyBorder(2, 7, 2, 7));
		statusButtonPanel.add(btnSelectOnline);
		
		statusPanel.add(statusButtonPanel, BorderLayout.EAST);
		
		statusPanel.setBackground(Main.STATUS_LINE);
		getContentPane().add(statusPanel, BorderLayout.SOUTH);

		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		devicesTable = new DevicesTable(tModel);
		devicesTable.sortByColumn(DevicesTable.COL_IP_IDX, true);
		scrollPane.setViewportView(devicesTable);
		scrollPane.getViewport().setBackground(Main.BG_COLOR);

		JToolBar toolBar = new JToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);

		rescanAction.setEnabled(false);
		refreshAction.setEnabled(false);
		toolBar.add(rescanAction);
		toolBar.add(refreshAction);
		toolBar.addSeparator();
		toolBar.add(infoAction);
		toolBar.add(infoLogAction);
		toolBar.add(browseAction);
		toolBar.addSeparator();
		toolBar.add(backupAction);
		toolBar.add(restoreAction);
		toolBar.addSeparator();
		toolBar.add(settingsAction);
		toolBar.add(rebootAction);
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(csvExportAction);
		toolBar.add(printAction);
		toolBar.addSeparator();
		toolBar.add(appSettingsAction);
		toolBar.add(aboutAction);
		
		UsnaPopupMenu tablePopup = new UsnaPopupMenu(infoAction, browseAction, backupAction, restoreAction, loginAction) {
			private static final long serialVersionUID = 1L;
			@Override
			protected void doPopup(MouseEvent evt) {
				final int r, c;
				if((r = devicesTable.rowAtPoint(evt.getPoint())) >= 0 && (c = devicesTable.columnAtPoint(evt.getPoint())) >= 0) {
					devicesTable.changeSelection(r, c, false, false);
					final int modelRow = devicesTable.convertRowIndexToModel(r);
					loginAction.setEnabled(model.get(modelRow).getStatus() == Status.NOT_LOOGGED);
					show(devicesTable, evt.getX(), evt.getY());
				}
			}
		};
		devicesTable.addMouseListener(tablePopup.getMouseListener());
		
		devicesTable.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent evt) {
		        if (evt.getClickCount() == 2 && devicesTable.getSelectedRow() != -1 && devicesTable.isCellEditable(devicesTable.rowAtPoint(evt.getPoint()), devicesTable.columnAtPoint(evt.getPoint())) == false) {
		        	if(appProp.getProperty(DialogAppSettings.PROP_DCLICK_ACTION, DialogAppSettings.PROP_DCLICK_ACTION_DEFAULT).equals("DET")) {
		        		infoAction.actionPerformed(null);
		        	} else {
		        		browseAction.actionPerformed(null);
		        	}
		        }
		    }
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				storeProperties();
				dispose();
			}
			@Override
			public void windowClosed(WindowEvent e) {
				model.close();
				System.exit(0);
			}
		});
		
		getRootPane().registerKeyboardAction(e -> textFieldFilter.requestFocus(), KeyStroke.getKeyStroke(KeyEvent.VK_F, SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		statusLabel.setText(LABELS.getString("scanning_start"));
		manageActions();
	}
	
	private void manageActions() {
		ListSelectionListener l = e -> {
			if(e == null || e.getValueIsAdjusting() == false) {
				final boolean selection = devicesTable.getSelectedRowCount() > 0;
				final boolean singleSelection = devicesTable.getSelectedRowCount() == 1;
				infoAction.setEnabled(singleSelection);
				infoLogAction.setEnabled(singleSelection);
				rebootAction.setEnabled(selection);
				browseAction.setEnabled(selection && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
				backupAction.setEnabled(selection);
				restoreAction.setEnabled(singleSelection);
				settingsAction.setEnabled(selection);
			}
		};

		devicesTable.getSelectionModel().addListSelectionListener(l);
		l.valueChanged(null);
	}

	private static Object[] generateRow(ShellyAbstractDevice d) {
		final Object row[] = new Object[DevicesTable.COL_COMMAND_IDX + 1];
		if(d.getStatus() == Status.ON_LINE) {
			row[DevicesTable.COL_STATUS_IDX] = ONLINE_BULLET;
		} else if(d.getStatus() == Status.OFF_LINE) {
			row[DevicesTable.COL_STATUS_IDX] = OFFLINE_BULLET;
		} else if(d.getStatus() == Status.READING) {
			row[DevicesTable.COL_STATUS_IDX] = UPDATING_BULLET;
		} else if(d.getStatus() == Status.ERROR) {
			row[DevicesTable.COL_STATUS_IDX] = ERROR_BULLET;
		} else { // Status.NOT_LOOGGED
			row[DevicesTable.COL_STATUS_IDX] = LOGIN_BULLET;
		}
		row[DevicesTable.COL_TYPE] = d.getTypeName() + " (" + d.getHostname() + ")";
		row[DevicesTable.COL_NAME] = d.getName();
		row[DevicesTable.COL_IP_IDX] = d.getHttpHost().getHostName(); //"d.getHttpHost().getAddress().getHostAddress()";
		if(d.getStatus() != Status.NOT_LOOGGED && (d instanceof ShellyUnmanagedDevice == false || ((ShellyUnmanagedDevice)d).geException() == null)) {
			row[DevicesTable.COL_RSSI_IDX] = d.getRssi();
			row[DevicesTable.COL_CLOUD] = (d.getCloudEnabled() ? YES : NO) + " / " + (d.getCloudConnected() ? YES : NO);
			row[DevicesTable.COL_UPTIME_IDX] = d.getUptime();
			row[DevicesTable.COL_INT_TEMP] = (d instanceof InternalTmpHolder) ? ((InternalTmpHolder)d).getInternalTmp() : null;
			row[DevicesTable.COL_DEBUG] = LABELS.getString("debug" + d.getDebugMode());
			if(d instanceof RelayCommander && ((RelayCommander)d).getRelayCount() > 0) {
				row[DevicesTable.COL_COMMAND_IDX] = ((RelayCommander)d).getRelays();
			} else if(d instanceof RollerCommander && ((RollerCommander)d).getRollerCount() > 0) {
				row[DevicesTable.COL_COMMAND_IDX] = ((RollerCommander)d).getRoller(0);
			} else if(d instanceof WhiteCommander && ((WhiteCommander)d).getWhiteCount() == 1) { // dimmer
				row[DevicesTable.COL_COMMAND_IDX] = ((WhiteCommander)d).getWhite(0);
//				row.add(new LightRGBW(d, 0));
			} else if(d instanceof LightBulbRGBCommander) {
				row[DevicesTable.COL_COMMAND_IDX] = ((LightBulbRGBCommander)d).getLight(0);
			} else if(d instanceof RGBWCommander && ((RGBWCommander)d).getColorCount() > 0) {
				row[DevicesTable.COL_COMMAND_IDX] = ((RGBWCommander)d).getColor(0);
			} else if(d instanceof WhiteCommander && ((WhiteCommander)d).getWhiteCount() > 1) {
				row[DevicesTable.COL_COMMAND_IDX] = ((WhiteCommander)d).getWhites();
//				row.add("<html>" + Arrays.stream(((WhiteCommander)d).getWhites()).map(l -> l.getLabel()).collect(Collectors.joining("<br>")) + "</html>");
			} else if(d instanceof ShellyDW) {
				row[DevicesTable.COL_COMMAND_IDX] = LABELS.getString("lableStatusOpen") + ": " + (((ShellyDW)d).isOpen() ? YES : NO);
			} else if(d instanceof ShellyFlood) {
				row[DevicesTable.COL_COMMAND_IDX] = LABELS.getString("lableStatusFlood") + ": " + (((ShellyFlood)d).flood() ? YES : NO);
			} else if(d instanceof ActionsCommander) {
				row[DevicesTable.COL_COMMAND_IDX] = ((ActionsCommander)d).getActionsGroups();
			} /*else {
				row.add(d.getName());
			}*/
			// test
//			Roller r = new Roller(d, 0);
//			r.setCalibrated(true);
//			row.add(r);
//			row.add(new RGBWColor(d, 0));
//			row.add(new RGBWWhite[] {new RGBWWhite(d, 0), new RGBWWhite(d, 1), new RGBWWhite(d, 2), new RGBWWhite(d, 3)});
		}
		return row;
	}

	private void storeProperties() {
		storeProperties(appProp);
		try {
			appProp.store(false);
		} catch (IOException | RuntimeException ex) {
			LOG.error("Unexpected", ex);
			JOptionPane.showMessageDialog(this, "Error on exit", LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void update(Observable o, Object msg) {
		SwingUtilities.invokeLater(() -> {
			ModelMessage mm = (ModelMessage)msg;
			try {
				if(mm.getType() == ModelMessage.Type.ADD) {
					devicesTable.resetRowsComputedHeight();
					tModel.addRow(generateRow(model.get((Integer)mm.getBody())));
					statusLabel.setText(String.format(LABELS.getString("scanning_end"), model.size()));
					devicesTable.columnsWidthAdapt();
					devicesTable.getRowSorter().allRowsChanged();
				} else if(mm.getType() == ModelMessage.Type.REMOVE) {
					int ind = (Integer)mm.getBody();
					tModel.setValueAt(OFFLINE_BULLET, ind, DevicesTable.COL_STATUS_IDX);
				} else if(mm.getType() == ModelMessage.Type.READY) {
					statusLabel.setText(String.format(LABELS.getString("scanning_end"), model.size()));
					rescanAction.setEnabled(true);
					refreshAction.setEnabled(true);
				} else if(mm.getType() == ModelMessage.Type.UPDATE) {
					devicesTable.resetRowsComputedHeight();
					int ind = (Integer)mm.getBody();
					Object[] row = generateRow(model.get(ind));
					tModel.setRow(ind, row);
					final ListSelectionModel lsm = devicesTable.getSelectionModel(); // allRowsChanged() do not preserve the selected cell; this mess the selection dragging the mouse
					final int i1 = lsm.getAnchorSelectionIndex();
					final int i2 = lsm.getLeadSelectionIndex();
					devicesTable.getRowSorter().allRowsChanged();
					lsm.setAnchorSelectionIndex(i1);
					lsm.setLeadSelectionIndex(i2);
				} else if(mm.getType() == ModelMessage.Type.CLEAR) {
					tModel.clear();
				}
			} catch (Exception ex) {
				LOG.error("Unexpected", ex);
			}
		});
	}
} //557 - 614 - 620 - 669