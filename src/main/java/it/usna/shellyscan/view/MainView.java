package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.mvc.singlewindow.MainWindow;
import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.BackupAction;
import it.usna.shellyscan.controller.RestoreAction;
import it.usna.shellyscan.controller.SelectionAction;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaSelectedAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.shellyscan.view.chart.MeasuresChart;
import it.usna.shellyscan.view.devsettings.DialogDeviceSettings;
import it.usna.shellyscan.view.scripts.DialogDeviceScriptsG2;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.UsnaPopupMenu;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.AppProperties;
import it.usna.util.UsnaEventListener;

public class MainView extends MainWindow implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	public final static int SHORTCUT_KEY = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	private final static Logger LOG = LoggerFactory.getLogger(MainWindow.class);
	private ListSelectionListener tableSelectionListener;
	private boolean browserSupported = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);

	private AppProperties appProp;
	private JLabel statusLabel = new JLabel();
	private boolean statusLineReserved = false;
	private JTextField textFieldFilter = new JTextField();
	private Devices model;
	private UsnaTableModel tabModel = new UsnaTableModel(
			"",
			LABELS.getString("col_type"),
			LABELS.getString("col_device"),
			LABELS.getString("col_device_name"),
			LABELS.getString("col_mac"),
			LABELS.getString("col_ip"),
			LABELS.getString("col_ssid"),
			LABELS.getString("col_rssi"),
			LABELS.getString("col_cloud"),
			LABELS.getString("col_mqtt"),
			LABELS.getString("col_uptime"),
			LABELS.getString("col_intTemp"),
			LABELS.getString("col_measures"),
			LABELS.getString("col_debug"),
			LABELS.getString("col_source"),
			LABELS.getString("col_relay"));
	private DevicesTable devicesTable = new DevicesTable(tabModel);
	
	private JToggleButton details;
	private AppProperties temporaryProp = new AppProperties();

	private Action infoAction = new UsnaSelectedAction(this, devicesTable, "action_info_name", "action_info_tooltip", "/images/Bubble3_16.png", "/images/Bubble3.png",
			i -> new DialogDeviceInfo(MainView.this, true, model.get(i), model.get(i).getInfoRequests()) );

	private Action infoLogAction = new UsnaSelectedAction(this, devicesTable, "/images/Document2.png", "action_info_log_tooltip", i -> {
		if(model.get(i) instanceof AbstractG2Device) {
			new DialogDeviceLogsG2(MainView.this, model, i, AbstractG2Device.LOG_VERBOSE);
		} else {
			new DialogDeviceInfo(MainView.this, false, model.get(i), new String[]{"/debug/log", "/debug/log1"});
		}
	});

	private Action rescanAction = new UsnaAction(null, "/images/73-radar.png", "action_scan_tooltip", e -> {
		devicesTable.clearSelection();
		reserveStatusLine(true);
		setStatus(LABELS.getString("scanning_start"));
		SwingUtilities.invokeLater(() -> {
			MainView.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				model.rescan(appProp.getBoolProperty(DialogAppSettings.PROP_USE_ARCHIVE, true));
				Thread.sleep(500); // too many call disturb some devices
			} catch (IOException e1) {
				Msg.errorMsg(e1);
			} catch (InterruptedException e1) {
			} finally {
				reserveStatusLine(false);
				MainView.this.setCursor(Cursor.getDefaultCursor());
			}
		});
	});

	private Action refreshAction = new UsnaAction(this, "/images/Refresh.png", "action_refresh_tooltip", e -> {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		setEnabled(false);
		devicesTable.stopCellEditing();
		for(int i = 0; i < tabModel.getRowCount(); i++) {
			if(model.get(i) instanceof GhostDevice == false) {
				tabModel.setValueAt(DevicesTable.UPDATING_BULLET, i, DevicesTable.COL_STATUS_IDX);
				model.refresh(i, false);
			}
		}
		try {
			Thread.sleep(250); // too many call disturb some devices expecially gen1
		} catch (InterruptedException e1) {}
		devicesTable.resetRowsComputedHeight();
		setEnabled(true);
		setCursor(Cursor.getDefaultCursor());
	});
	
	private Action rebootAction = new UsnaAction(this, "/images/nuke.png", "action_reboot_tooltip"/*"Reboot"*/, e -> {
		final String cancel = UIManager.getString("OptionPane.cancelButtonText");
		if(JOptionPane.showOptionDialog(
				MainView.this, LABELS.getString("action_reboot_confirm"), LABELS.getString("action_reboot_tooltip"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new Object[] {LABELS.getString("action_reboot_name"), cancel}, cancel) == 0) {
			for(int ind: devicesTable.getSelectedRows()) {
				int modelRow = devicesTable.convertRowIndexToModel(ind);
				ShellyAbstractDevice d = model.get(modelRow);
				d.setStatus(Status.READING);
				tabModel.setValueAt(DevicesTable.UPDATING_BULLET, modelRow, DevicesTable.COL_STATUS_IDX);
				SwingUtilities.invokeLater(() -> model.reboot(modelRow));
			}
		}
	});
	
	private Action checkListAction = new UsnaAction(this, "/images/Ok.png", "action_checklist_tooltip", e -> {
		List<? extends RowSorter.SortKey> k = devicesTable.getRowSorter().getSortKeys();
		new DialogDeviceCheckList(this, model, devicesTable.getSelectedModelRows(), k.get(0).getColumn() == DevicesTable.COL_IP_IDX ? k.get(0).getSortOrder() : SortOrder.UNSORTED);
		try {
			Thread.sleep(250); // too many call disturb some devices (especially gen1)
		} catch (InterruptedException e1) {}
	});
	
	private Action browseAction = new UsnaSelectedAction(this, devicesTable, "action_web_name", "action_web_tooltip", "/images/Computer16.png", "/images/Computer.png", i -> {
		try {
			Desktop.getDesktop().browse(new URI("http://" + InetAddressAndPort.toString(model.get(i))));
		} catch (IOException | URISyntaxException e) {
			Msg.errorMsg(e);
		}
	});
	
	private Action aboutAction = new UsnaAction(this, "/images/question.png", null/*"About"*/, e -> DialogAbout.show(MainView.this));
	
	private Action loginAction = new UsnaSelectedAction(this, devicesTable, "action_name_login", null, "/images/Key16.png", null,
			i -> model.create(model.get(i).getAddress(), model.get(i).getPort(), model.get(i).getHostname(), true) );
	
	private Action reloadAction = new UsnaSelectedAction(this, devicesTable, "action_name_reload", null, "/images/Loop16.png", null,
			i -> model.create(model.get(i).getAddress(), model.get(i).getPort(), model.get(i).getHostname(), false) );

	private Action backupAction;

	private Action restoreAction;
	
	private Action chartAction = new UsnaAction(this, "/images/Stats2.png", "action_chart_tooltip",
			e -> new MeasuresChart(this, model, devicesTable.getSelectedModelRows(), appProp) );
	
	private Action appSettingsAction = new UsnaAction(this, "/images/Gear.png", "action_appsettings_tooltip",
			e -> new DialogAppSettings(MainView.this, devicesTable, model, details.isSelected(), appProp) );
	
	private Action scriptManagerAction = new UsnaSelectedAction(this, devicesTable, "/images/Movie.png", "action_script_tooltip",
			i -> new DialogDeviceScriptsG2(MainView.this, model, i) );
	
	private Action detailedViewAction = new UsnaAction(this, "/images/Plus.png", "action_show_detail_tooltip",
			e -> SwingUtilities.invokeLater(() -> detailedView(((JToggleButton)e.getSource()).isSelected()) ) );
	
	private Action notesAction = new UsnaSelectedAction(this, devicesTable, "/images/Write2.png", "action_notes_tooltip",
			i -> new NotesEditor(MainView.this, model.getGhost(i)) );
	
//	private Action eraseGhostAction = new UsnaAction(this, "action_name_delete_ghost", null, "/images/Minus16.png", null, e -> model.remove(devicesTable.getSelectedModelRow()));
	private Action eraseGhostAction = new UsnaAction(this, "action_name_delete_ghost", null, "/images/Minus16.png", null, e -> {
		boolean delete = true;
		for(int idx: devicesTable.getSelectedRows()) {devicesTable.getSelectionModel().getLeadSelectionIndex();
			if(model.getGhost(devicesTable.convertRowIndexToModel(idx)).getNote().trim().length() > 0) {
				delete = JOptionPane.showConfirmDialog(
					MainView.this, LABELS.getString("action_name_delete_ghost_confirm"), LABELS.getString("action_name_delete_ghost"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
				break;
			}
		}
		if(delete) {
			Arrays.stream(devicesTable.getSelectedRows()).map(i -> devicesTable.convertRowIndexToModel(i)).boxed().
			sorted(Collections.reverseOrder()).forEach(i-> model.remove(i));
		}
	});
	
	private Action printAction = new UsnaAction(this, "/images/Printer.png", "action_print_tooltip", e -> {
		try {
			devicesTable.clearSelection();
			devicesTable.print(JTable.PrintMode.FIT_WIDTH);
		} catch (java.awt.print.PrinterException ex) {
			Msg.errorMsg(ex);
		}
	});
	
	private Action csvExportAction = new UsnaAction(this, "/images/Table.png", "action_csv_tooltip", e -> {
		final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
		fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_csv_desc"), "csv"));
		if(fc.showSaveDialog(MainView.this) == JFileChooser.APPROVE_OPTION) {
			File out = fc.getSelectedFile();
			if(out.getName().contains(".") == false) {
				out = new File(out.getParentFile(), out.getName() + ".csv");
			}
			try (BufferedWriter w = Files.newBufferedWriter(out.toPath())) {
				devicesTable.csvExport(w, appProp.getProperty(DialogAppSettings.PROP_CSV_SEPARATOR, DialogAppSettings.PROP_CSV_SEPARATOR_DEFAULT));
				JOptionPane.showMessageDialog(MainView.this, LABELS.getString("msgFileSaved"), Main.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException ex) {
				Msg.errorMsg(ex);
			}
			appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
		}
	});

	private Action devicesSettingsAction = new UsnaAction(this, "/images/Tool.png", "action_general_conf_tooltip", e -> {
		new DialogDeviceSettings(MainView.this, model, devicesTable.getSelectedModelRows());
	});
	
	private Action eraseFilterAction = new UsnaAction(this, "/images/erase-9-16.png", null, e -> {
		textFieldFilter.setText("");
		textFieldFilter.requestFocusInWindow();
		devicesTable.clearSelection();
		displayStatus();
	});

//	private Action copyHostAction = new UsnaSelectedAction(this, devicesTable, "action_copy_hostname", null, "/images/Clipboard_Copy_16.png", null, i -> {
//		StringSelection ss = new StringSelection(model.get(i).getHostname());
//		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
//	});

	public MainView(final Devices model, final AppProperties appProp) {
		this.model = model;
		this.appProp = appProp;
		model.addListener(this);
		
		backupAction = new BackupAction(this, devicesTable, appProp, model);
		restoreAction = new RestoreAction(this, devicesTable, appProp, model);

		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setHgap(2);
		loadProperties(appProp);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(Main.ICON)));
		setTitle(Main.APP_NAME + " v." + Main.VERSION);

		// Status bar
		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		statusPanel.setLayout(new BorderLayout());
		statusPanel.add(statusLabel, BorderLayout.WEST);
		
		JPanel statusButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
		statusButtonPanel.setOpaque(false);
		statusButtonPanel.add(new JLabel(LABELS.getString("lblFilter")));
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		textFieldFilter.setColumns(16);
		
		JComboBox<String> comboFilterCol = new JComboBox<>();
		comboFilterCol.addItem(LABELS.getString("lblFilterFull"));
		comboFilterCol.addItem(LABELS.getString("col_type"));
		comboFilterCol.addItem(LABELS.getString("col_device"));
		comboFilterCol.addItem(LABELS.getString("col_device_name"));
		comboFilterCol.addActionListener(event -> {
			setColFilter(comboFilterCol);
			displayStatus();
		});
		statusButtonPanel.add(comboFilterCol);
		statusButtonPanel.add(textFieldFilter);
		
		textFieldFilter.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setColFilter(comboFilterCol);
				displayStatus();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				setColFilter(comboFilterCol);
				displayStatus();
			}
		});
		
		JButton eraseFilterButton = new JButton(eraseFilterAction);
		eraseFilterButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		eraseFilterButton.setContentAreaFilled(false);
		eraseFilterButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, SHORTCUT_KEY), "find_erase");
		eraseFilterButton.getActionMap().put("find_erase", eraseFilterAction);
		statusButtonPanel.add(eraseFilterButton);

		Action selectAll = new UsnaAction(null, "labelSelectAll", null, "/images/list_unordered.png", null, e -> devicesTable.selectAll());
		Action selectOnLine = new SelectionAction(devicesTable, "labelSelectOnLine", null, "/images/list_online.png", i -> model.get(i).getStatus() == Status.ON_LINE);
		
		JButton btnSelectAll = new JButton(selectAll);
		btnSelectAll.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 8));
		statusButtonPanel.add(btnSelectAll);
		
		JButton btnSelectOnline = new JButton(selectOnLine);
		btnSelectOnline.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 8));
		statusButtonPanel.add(btnSelectOnline);
		
		// Selection popup
		JButton btnSelectCombo = new JButton(new ImageIcon(MainView.class.getResource("/images/expand-more.png")));
		btnSelectCombo.setContentAreaFilled(false);
		btnSelectCombo.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 2));
		statusButtonPanel.add(btnSelectCombo);

		UsnaPopupMenu selectionPopup = new UsnaPopupMenu(selectAll, selectOnLine,
				new SelectionAction(devicesTable, "labelSelectOnLineReboot", null, /*"/images/bullet_yes_reboot.png"*/null, i -> model.get(i).getStatus() == Status.ON_LINE && model.get(i).rebootRequired()),
				new SelectionAction(devicesTable, "labelSelectG1", null, null, i -> model.get(i) instanceof AbstractG1Device),
				new SelectionAction(devicesTable, "labelSelectG2", null, null, i -> model.get(i) instanceof AbstractG2Device),
				new SelectionAction(devicesTable, "labelSelectGhosts", null, null, i -> model.get(i) instanceof GhostDevice)
				);
//		MouseListener popupListener = selectionPopup.getMouseListener();
//		btnSelectCombo.addMouseListener(popupListener);
//		btnSelectOnline.addMouseListener(popupListener);
//		btnSelectAll.addMouseListener(popupListener);

		btnSelectCombo.addActionListener(e -> selectionPopup.show(btnSelectCombo, 0, 0));
		
		statusPanel.add(statusButtonPanel, BorderLayout.EAST);
		statusPanel.setBackground(Main.STATUS_LINE);
		getContentPane().add(statusPanel, BorderLayout.SOUTH);

		// Table
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		devicesTable.sortByColumn(DevicesTable.COL_IP_IDX, SortOrder.ASCENDING);
		devicesTable.loadColPos(appProp);
		
		scrollPane.setViewportView(devicesTable);
		scrollPane.getViewport().setBackground(Main.BG_COLOR);

		// Toolbar
		JToolBar toolBar = new JToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);
		details = new JToggleButton(detailedViewAction);
		details.setSelectedIcon(new ImageIcon(getClass().getResource("/images/Minus.png")));;
		details.setRolloverIcon(new ImageIcon(getClass().getResource("/images/Plus.png")));
		details.setRolloverSelectedIcon(new ImageIcon(getClass().getResource("/images/Minus.png")));
		toolBar.add(rescanAction);
		toolBar.add(refreshAction);
		toolBar.addSeparator();
		toolBar.add(infoAction);
		toolBar.add(infoLogAction);
		toolBar.add(chartAction);
		toolBar.add(checkListAction);
		toolBar.add(browseAction);
		toolBar.addSeparator();
		toolBar.add(backupAction);
		toolBar.add(restoreAction);
		toolBar.addSeparator();
		toolBar.add(devicesSettingsAction);
		toolBar.add(scriptManagerAction);
		toolBar.add(rebootAction);
		toolBar.addSeparator();
		toolBar.add(notesAction);
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(details);
		toolBar.add(csvExportAction);
		toolBar.add(printAction);
		toolBar.addSeparator();
		toolBar.add(appSettingsAction);
		toolBar.add(aboutAction);

		// devices popup
		UsnaPopupMenu tablePopup = new UsnaPopupMenu(infoAction, browseAction, backupAction, restoreAction, reloadAction, loginAction);
		UsnaPopupMenu ghostDevPopup = new UsnaPopupMenu(reloadAction, eraseGhostAction);

		devicesTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
				doPopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				doPopup(e);
			}
			private void doPopup(MouseEvent evt) {
				final int r;
				if (evt.isPopupTrigger() && (r = devicesTable.rowAtPoint(evt.getPoint())) >= 0) {
					if(devicesTable.isRowSelected(r) == false) {
						devicesTable.setRowSelectionInterval(r, r);
					}
					boolean ghost = false;
					boolean notGhost = false;
					boolean notLogged = true;
					for(int idx: devicesTable.getSelectedRows()) {
						ShellyAbstractDevice d = model.get(devicesTable.convertRowIndexToModel(idx));
						if(d instanceof GhostDevice) {
							ghost = true;
						} else {
							notGhost = true;
							notLogged &= (d.getStatus() == Status.NOT_LOOGGED);
						}
					}
//					ShellyAbstractDevice d = model.get(devicesTable.convertRowIndexToModel(r));
					if(/*d instanceof GhostDevice*/ghost && notGhost == false) {
						reloadAction.setEnabled(true);
						ghostDevPopup.show(devicesTable, evt.getX(), evt.getY());
					} else if(notGhost && ghost == false) {
//						boolean notLogged = d.getStatus() == Status.NOT_LOOGGED;
						loginAction.setEnabled(notLogged);
						reloadAction.setEnabled(notLogged == false);
						tablePopup.show(devicesTable, evt.getX(), evt.getY());
					}
				}
			}
		});
		
		// double click
		devicesTable.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent evt) {
		        if (evt.getClickCount() == 2 && devicesTable.getSelectedRow() >= 0 && devicesTable.isCellEditable(devicesTable.getSelectedRow(), devicesTable.getSelectedColumn()) == false) {
		        	if(appProp.getProperty(DialogAppSettings.PROP_DCLICK_ACTION, DialogAppSettings.PROP_DCLICK_ACTION_DEFAULT).equals("DET") && infoAction.isEnabled()) {
		        		infoAction.actionPerformed(null);
		        	} else if(browseAction.isEnabled()) {
		        		browseAction.actionPerformed(null);
		        	}
		        }
		    }
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				MainView.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
		getRootPane().registerKeyboardAction(e -> {
			int selected = comboFilterCol.getSelectedIndex();
			comboFilterCol.setSelectedIndex(++selected >= comboFilterCol.getItemCount() ? 0 : selected);
			textFieldFilter.requestFocus();
		} , KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);

		rescanAction.setEnabled(false);
		refreshAction.setEnabled(false);
		statusLabel.setText(LABELS.getString("scanning_start"));
		manageRowsSelection();
	}
	
	private void setColFilter(JComboBox<?> combo) {
		int sel = combo.getSelectedIndex();
		final int[] cols;
		if(sel == 0) cols = new int[] {DevicesTable.COL_TYPE, DevicesTable.COL_DEVICE, DevicesTable.COL_NAME, DevicesTable.COL_IP_IDX/*, DevicesTable.COL_COMMAND_IDX*/};
		else if(sel == 1) cols = new int[] {DevicesTable.COL_TYPE};
		else if(sel == 2) cols = new int[] {DevicesTable.COL_DEVICE};
		else cols = new int[] {DevicesTable.COL_NAME};
		devicesTable.setRowFilter(textFieldFilter.getText(), cols);
	}
	
	private void manageRowsSelection() {
		tableSelectionListener = e -> {
			if(e.getValueIsAdjusting() == false) {
				boolean singleSelection, singleSelectionNoGhost;
				singleSelection = singleSelectionNoGhost = devicesTable.getSelectedRowCount() == 1;
				boolean selectionNoGhost = devicesTable.getSelectedRowCount() > 0;
				ShellyAbstractDevice d = null;
				for(int idx: devicesTable.getSelectedRows()) {
					d = model.get(devicesTable.convertRowIndexToModel(idx));
					if(d instanceof GhostDevice) {
						selectionNoGhost = singleSelectionNoGhost = false; // cannot operate ghost devices
						break;
					}
				}
				infoAction.setEnabled(singleSelectionNoGhost);
				infoLogAction.setEnabled(singleSelectionNoGhost);
				checkListAction.setEnabled(selectionNoGhost);
				rebootAction.setEnabled(selectionNoGhost);
				browseAction.setEnabled(selectionNoGhost && browserSupported);
				backupAction.setEnabled(selectionNoGhost);
				restoreAction.setEnabled(singleSelectionNoGhost);
				devicesSettingsAction.setEnabled(selectionNoGhost);
				chartAction.setEnabled(selectionNoGhost);
				scriptManagerAction.setEnabled(singleSelectionNoGhost && d instanceof AbstractG2Device);
				notesAction.setEnabled(singleSelection && appProp.getBoolProperty(DialogAppSettings.PROP_USE_ARCHIVE, true));
				
				displayStatus();
			}
		};
		devicesTable.getSelectionModel().addListSelectionListener(tableSelectionListener);
		tableSelectionListener.valueChanged(new ListSelectionEvent(devicesTable, -1, -1, false));
	}
	
	private void detailedView(boolean detailed) {
//		details.setSelected(detailed);
		if(detailed) {
			// store normal view preferences
			super.storeProperties(appProp);
			devicesTable.saveColPos(appProp, DevicesTable.STORE_PREFIX);
			devicesTable.saveColWidth(temporaryProp, DevicesTable.STORE_PREFIX);
			final int visible = devicesTable.getColumnCount();
			// load extended view preferences
			devicesTable.restoreColumns();
			devicesTable.resetRowsComputedHeight();
			devicesTable.loadColPos(appProp, DevicesTable.STORE_EXT_PREFIX);
			
			String detScreenMode = appProp.getProperty(DialogAppSettings.PROP_DETAILED_VIEW_SCREEN, DialogAppSettings.PROP_DETAILED_VIEW_SCREEN_DEFAULT);
			if(getExtendedState() != JFrame.MAXIMIZED_BOTH) {
				if(detScreenMode.equals(DialogAppSettings.PROP_DETAILED_VIEW_SCREEN_FULL)) {
					setExtendedState(JFrame.MAXIMIZED_BOTH);
				} else if(detScreenMode.equals(DialogAppSettings.PROP_DETAILED_VIEW_SCREEN_HORIZONTAL)) {
					setExtendedState(JFrame.MAXIMIZED_HORIZ);
				} else if(detScreenMode.equals(DialogAppSettings.PROP_DETAILED_VIEW_SCREEN_ESTIMATE)) {
					Rectangle screen = getCurrentScreenBounds();
					Rectangle current = this.getBounds();
					current.width = current.width * /*total*/tabModel.getColumnCount() / visible;
					if(current.x + current.width > screen.x + screen.width) { // out of right margin
						current.x = screen.x + screen.width - current.width;
					}
					if(current.x < screen.x) { // too wide; larger than screen
						setExtendedState(JFrame.MAXIMIZED_HORIZ);
					} else {
						setBounds(current);
					}
				} // else do not change size
			}
			if(devicesTable.loadColWidth(temporaryProp, DevicesTable.STORE_EXT_PREFIX) == false) { // SUBSTITUTE || ADD || ...
				devicesTable.columnsWidthAdapt();
			}
		} else {
			// store extended view preferences
			devicesTable.saveColPos(appProp, DevicesTable.STORE_EXT_PREFIX);
			devicesTable.saveColWidth(temporaryProp, DevicesTable.STORE_EXT_PREFIX);
			// load normal view preferences
			devicesTable.restoreColumns();
			devicesTable.resetRowsComputedHeight();
			devicesTable.loadColPos(appProp, DevicesTable.STORE_PREFIX);
			if(devicesTable.loadColWidth(temporaryProp, DevicesTable.STORE_PREFIX) == false) { // SUBSTITUTE || ADD || ...
				devicesTable.columnsWidthAdapt();
			}
			super.loadProperties(appProp);
		}
	}

	private void storeProperties() {
		if(appProp.getBoolProperty(DialogAppSettings.PROP_USE_ARCHIVE, true)) {
			try {
				model.saveToStore(Paths.get(appProp.getProperty(DialogAppSettings.PROP_ARCHIVE_FILE, DialogAppSettings.PROP_ARCHIVE_FILE_DEFAULT)));
			} catch (IOException | RuntimeException ex) {
				LOG.error("Unexpected", ex);
				Msg.errorMsg(this, "Error storing archive");
			}
		}
		if(details.isSelected()) { // else -> normal view values stored on detailedView(true)
			devicesTable.saveColPos(appProp, DevicesTable.STORE_EXT_PREFIX);
			// no position/size stored for detailed view
		} else {
			devicesTable.saveColPos(appProp, DevicesTable.STORE_PREFIX);
			super.storeProperties(appProp);
		}
		try {
			appProp.store(false);
		} catch (IOException | RuntimeException ex) {
			LOG.error("Unexpected", ex);
			Msg.errorMsg(this, "Error on exit");
		}
	}

	@Override
	public void update(Devices.EventType mesgType, Integer msgBody) {
		SwingUtilities.invokeLater(() -> {
			try {
				if(mesgType == Devices.EventType.UPDATE) {
					devicesTable.updateRow(model.get(msgBody), msgBody);
				} else if(mesgType == Devices.EventType.ADD) {
					devicesTable.addRow(model.get(msgBody));
					displayStatus();
//				} else if(mesgType == Devices.EventType.REMOVE) { tabModel.setValueAt(DevicesTable.OFFLINE_BULLET, msgBody, DevicesTable.COL_STATUS_IDX);
				} else if(mesgType == Devices.EventType.SUBSTITUTE) {
					devicesTable.updateRow(model.get(msgBody), msgBody);
					devicesTable.columnsWidthAdapt();
					tableSelectionListener.valueChanged(new ListSelectionEvent(devicesTable, -1, -1, false));
				} else if(mesgType == Devices.EventType.READY) {
					displayStatus();
					rescanAction.setEnabled(true);
					refreshAction.setEnabled(true);
				} else if(mesgType == Devices.EventType.CLEAR) {
					tabModel.clear();
				} else if(mesgType == Devices.EventType.DELETE) {
					tabModel.removeRow(msgBody);
				}
			} catch (IndexOutOfBoundsException ex) {
				LOG.debug("Unexpected", ex); // rescan/shutdown
			} catch (Throwable ex) {
				LOG.error("Unexpected", ex);
			}
		});
	}
	
	public synchronized void reserveStatusLine(boolean r) {
		statusLineReserved = r;
		if(r == false) {
			displayStatus();
		}
	}
	
	public synchronized void setStatus(String status) {
		statusLabel.setText(status);
	}
	
	private synchronized void displayStatus() {
		if(statusLineReserved == false) {
			if(textFieldFilter.getText().length() > 0) {
				statusLabel.setText(String.format(LABELS.getString("filter_status"), model.size(), devicesTable.getRowCount(), devicesTable.getSelectedRowCount()));
			} else {
				statusLabel.setText(String.format(LABELS.getString("scanning_end"), model.size(), devicesTable.getSelectedRowCount()));
			}
		}
	}
} //557 - 614 - 620 - 669 - 705 - 727 - 699 - 760 - 782 - 811 - 805 - 646