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
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
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
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.mvc.singlewindow.MainWindow;
import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.BackupAction;
import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.controller.RestoreAction;
import it.usna.shellyscan.controller.SelectionAction;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaSelectedAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.blu.AbstractBlueDevice;
import it.usna.shellyscan.model.device.blu.BTHomeDevice;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g3.AbstractG3Device;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.shellyscan.view.chart.MeasuresChart;
import it.usna.shellyscan.view.devsettings.DialogDeviceSettings;
import it.usna.shellyscan.view.scripts.DialogDeviceScripts;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.shellyscan.view.util.ScannerProperties.PropertyEvent;
import it.usna.swing.UsnaPopupMenu;
import it.usna.swing.table.UsnaTableModel;
import it.usna.swing.texteditor.TextDocumentListener;
import it.usna.util.AppProperties;
import it.usna.util.IOFile;
import it.usna.util.UsnaEventListener;

public class MainView extends MainWindow implements UsnaEventListener<Devices.EventType, Integer>, ScannerProperties.AppPropertyListener {
	private static final long serialVersionUID = 1L;
	public final static int SHORTCUT_KEY = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	private final static Logger LOG = LoggerFactory.getLogger(MainWindow.class);
	private final static ImageIcon DEFERRED_ICON = new ImageIcon(MainView.class.getResource("/images/deferred_list.png"));
	private final static ImageIcon DEFERRED_ICON_FAIL = new ImageIcon(MainView.class.getResource("/images/deferred_list_fail.png"));
	private final static ImageIcon DEFERRED_ICON_OK = new ImageIcon(MainView.class.getResource("/images/deferred_list_ok.png"));
	private ListSelectionListener tableSelectionListener;
	private AppProperties appProp;
	private AppProperties temporaryProp = new AppProperties(); // normal view properties stored here on detailed view
	private JLabel statusLabel = new JLabel();
	private boolean statusLineReserved = false;
	private JTextField textFieldFilter = new JTextField();
	private Devices model;
	private JToolBar toolBar = new JToolBar();
	private DialogDeferrables dialogDeferrables;
	private final UsnaTableModel tabModel = new UsnaTableModel(
			"",
			LABELS.getString("col_type"),
			LABELS.getString("col_device"),
			LABELS.getString("col_device_name"),
			LABELS.getString("col_keyword"),
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
	private final DevicesTable devicesTable = new DevicesTable(tabModel);

	private Action infoAction = new UsnaSelectedAction(this, devicesTable, "action_info_name", "action_info_tooltip", "/images/Bubble3_16.png", "/images/Bubble3.png",
			i -> new DialogDeviceInfo(MainView.this, model, i) );

	private Action infoLogAction = new UsnaSelectedAction(this, devicesTable, "action_info_log_name", "action_info_log_tooltip", null, "/images/Document2.png", i -> {
		if(model.get(i) instanceof AbstractG2Device) {
			new DialogDeviceLogsG2(MainView.this, model, i, AbstractG2Device.LOG_VERBOSE);
		} else if(model.get(i) instanceof AbstractBlueDevice blu) {
			new DialogDeviceLogsG2(MainView.this, model, model.getIndex(blu.getParent()), AbstractG2Device.LOG_VERBOSE);
		} else { // G1
			new DialogDeviceLogsG1(this, model.get(i));
		}
	});

	private Action rescanAction = new UsnaAction(null, "action_scan_name", "action_scan_tooltip", null, "/images/73-radar.png", e -> {
		if(devicesTable.isEditing()) {
			devicesTable.getCellEditor().stopCellEditing();
		}
		devicesTable.clearSelection();
		reserveStatusLine(true);
		setStatus(LABELS.getString("scanning_start"));
		SwingUtilities.invokeLater(() -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				model.rescan(appProp.getBoolProperty(ScannerProperties.PROP_USE_ARCHIVE, true));
				Thread.sleep(500); // too many call disturb some devices
			} catch (IOException e1) {
				Msg.errorMsg(this, e1);
			} catch (InterruptedException e1) {
			} finally {
				reserveStatusLine(false);
				setCursor(Cursor.getDefaultCursor());
			}
		});
	});

	private Action refreshAction = new UsnaAction(null, "action_refresh_name", "action_refresh_tooltip", null, "/images/Refresh.png", e -> {
		SwingUtilities.invokeLater(() -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			setEnabled(false);
			devicesTable.removeEditor(); //	devicesTable.stopCellEditing();
			for(int i = 0; i < tabModel.getRowCount(); i++) {
				if(model.get(i) instanceof GhostDevice == false) {
					tabModel.setValueAt(DevicesTable.UPDATING_BULLET, i, DevicesTable.COL_STATUS_IDX);
					model.refresh(i, false);
				} else {
					update(Devices.EventType.UPDATE, i); // keyword
				}
			}
			// too many call disturb some devices especially gen1
			try { Thread.sleep(250); } catch (InterruptedException e1) {}
			devicesTable.resetRowsComputedHeight();
			setEnabled(true);
			setCursor(Cursor.getDefaultCursor());
		});
	});
	
	private Action rebootAction = new UsnaSelectedAction(this, devicesTable, "action_reboot_name", "action_reboot_tooltip", null, "/images/nuke.png", () -> {
		final String cancel = UIManager.getString("OptionPane.cancelButtonText");
		return JOptionPane.showOptionDialog(
				MainView.this, LABELS.getString("action_reboot_confirm"), LABELS.getString("action_reboot_tooltip"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new Object[] {LABELS.getString("action_reboot_name"), cancel}, cancel) == 0;
	}, modelRow -> {
		ShellyAbstractDevice d = model.get(modelRow);
		d.setStatus(Status.READING);
		tabModel.setValueAt(DevicesTable.UPDATING_BULLET, modelRow, DevicesTable.COL_STATUS_IDX);
		SwingUtilities.invokeLater(() -> model.reboot(modelRow));
	});
	
	private Action checkListAction = new UsnaAction(this, "action_checklist_name", "action_checklist_tooltip", null, "/images/Ok.png", e -> {
		List<? extends RowSorter.SortKey> k = devicesTable.getRowSorter().getSortKeys();
		new CheckList(this, model, devicesTable.getSelectedModelRows(), k.get(0).getColumn() == DevicesTable.COL_IP_IDX ? k.get(0).getSortOrder() : SortOrder.UNSORTED);
		// too many call disturb some devices (especially gen1)
		try { Thread.sleep(250); } catch (InterruptedException e1) {}
	});
	
	private Action browseAction = new UsnaSelectedAction(this, devicesTable, "action_web_name", "action_web_tooltip", "/images/Computer16.png", "/images/Computer.png", i -> {
		try {
			Desktop.getDesktop().browse(URI.create("http://" + model.get(i).getAddressAndPort().getRepresentation()));
		} catch (IOException | UnsupportedOperationException e) { // browserSupported = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
			Msg.errorMsg(this, e);
		}
	});
	
	private Action aboutAction = new UsnaAction(this, "action_about_name", "action_about_tooltip", null, "/images/question.png", e -> DialogAbout.show(MainView.this));
	
	// also asks for credential if needed (login action)
	private UsnaAction reloadAction = new UsnaSelectedAction(this, devicesTable, "action_name_reload", null, "/images/Loop16.png", null, i -> {
		final ShellyAbstractDevice d = model.get(i);
		model.create(d.getAddressAndPort().getAddress(), d.getAddressAndPort().getPort(), d instanceof BTHomeDevice blu ? blu.getParent().getHostname() : d.getHostname(), false);
	});

	private Action backupAction;

	private Action restoreAction;
	
	private Action chartAction = new UsnaAction(this, "action_chart_name", "action_chart_tooltip", null, "/images/Stats2.png",
			e -> new MeasuresChart(this, model, devicesTable.getSelectedModelRows(), appProp) );
	
	private Action scriptManagerAction = new UsnaSelectedAction(this, devicesTable, "action_script_name", "action_script_tooltip", null, "/images/Movie.png",
			i -> new DialogDeviceScripts(MainView.this, model, i) );

	private Action notesAction = new UsnaSelectedAction(null, devicesTable, "action_notes_name", "action_notes_tooltip", "/images/Write2-16.png", "/images/Write2.png",
			i -> new NotesEditor(this, model.getGhost(i)) );
	
	private Action eraseGhostAction = new UsnaAction(this, "action_name_delete_ghost", null, "/images/Minus16.png", null, e -> {
		boolean delete = true;
		for(int idx: devicesTable.getSelectedRows()) {
			if(model.getGhost(devicesTable.convertRowIndexToModel(idx)).getNote().trim().length() > 0) {
				delete = (JOptionPane.showConfirmDialog(MainView.this, LABELS.getString("action_name_delete_ghost_confirm"), 
						LABELS.getString("action_name_delete_ghost"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION);
				break;
			}
		}
		if(delete) {
			Arrays.stream(devicesTable.getSelectedRows()).map(devicesTable::convertRowIndexToModel).boxed().sorted(Collections.reverseOrder()).forEach(i-> model.remove(i));
		}
	});
	
	private UsnaToggleAction detailedViewAction;
	
	private Action appSettingsAction = new UsnaAction(this, "action_appsettings_name", "action_appsettings_tooltip", null, "/images/Gear.png",
			e -> new DialogAppSettings(MainView.this, devicesTable, model, detailedViewAction.isSelected(), appProp) );
	
	private Action printAction = new UsnaAction(this, "action_print_name", "action_print_tooltip", null, "/images/Printer.png", e -> {
		try {
			devicesTable.clearSelection();
			devicesTable.print(JTable.PrintMode.FIT_WIDTH);
		} catch (java.awt.print.PrinterException ex) {
			Msg.errorMsg(this, ex);
		}
	});
	
	private Action csvExportAction = new UsnaAction(this, "action_csv_name", "action_csv_tooltip", null, "/images/Table.png", e -> {
		final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
		fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_csv_desc"), "csv"));
		if(fc.showSaveDialog(MainView.this) == JFileChooser.APPROVE_OPTION) {
			Path outPath = IOFile.addExtension(fc.getSelectedFile().toPath(), "csv");
			try (BufferedWriter writer = Files.newBufferedWriter(outPath)) {
				devicesTable.csvExport(writer, appProp.getProperty(ScannerProperties.PROP_CSV_SEPARATOR, ScannerProperties.PROP_CSV_SEPARATOR_DEFAULT));
				JOptionPane.showMessageDialog(MainView.this, LABELS.getString("msgFileSaved"), Main.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException ex) {
				Msg.errorMsg(this, ex);
			}
			appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
		}
	});

	private Action devicesSettingsAction = new UsnaAction(this, "action_general_conf_name", "action_general_conf_tooltip", null, "/images/Tool.png", e -> {
		new DialogDeviceSettings(this, model, devicesTable.getSelectedModelRows());
	});
	
	private Action eraseFilterAction = new UsnaAction(null, null, "/images/erase-9-16.png", e -> {
		textFieldFilter.setText("");
		textFieldFilter.requestFocusInWindow();
//		if(devicesTable.isEditing()) {
//			devicesTable.getCellEditor().stopCellEditing();
//		}
//		devicesTable.clearSelection();
		displayStatus();
	});

	public MainView(final Devices model, final ScannerProperties appProp) {
		this.model = model;
		this.appProp = appProp;
		model.addListener(this);
		appProp.addListener(this);
		
		backupAction = new BackupAction(this, devicesTable, appProp, model);
		restoreAction = new RestoreAction(this, devicesTable, appProp, model);

		loadProperties(appProp, 0.66f, 0.5f);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setIconImage(Main.ICON);
		setTitle(Main.APP_NAME + " " + Main.VERSION);

		// Status bar
		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout(6, 0));
		statusPanel.add(statusLabel, BorderLayout.CENTER);
		
		JPanel statusLeftPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
		statusLeftPanel.setOpaque(false);

		JButton btnshowDeferrables = new JButton("0", DEFERRED_ICON);
		btnshowDeferrables.addActionListener(new UsnaAction(this, "labelShowDeferrables", e -> {
			if(dialogDeferrables == null) { // single dialog
				dialogDeferrables = new DialogDeferrables();
				dialogDeferrables.setLocationRelativeTo(this);
			}
			if(dialogDeferrables.getExtendedState() == JFrame.ICONIFIED) {
				dialogDeferrables.setExtendedState(JFrame.NORMAL);
			} else {
				dialogDeferrables.setVisible(true);
			}
			btnshowDeferrables.setIcon(DEFERRED_ICON); // reset button icon (not success/fail)
		}));
		btnshowDeferrables.setBorder(BorderFactory.createEmptyBorder(4, 9, 3, 7));
		statusLeftPanel.add(btnshowDeferrables);
		statusPanel.add(statusLeftPanel, BorderLayout.WEST);
		
		JPanel statusFilterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
		statusFilterPanel.setOpaque(false);
		statusFilterPanel.add(new JLabel(LABELS.getString("lblFilter")));
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		textFieldFilter.setColumns(16);
		
		JComboBox<String> comboFilterCol = new JComboBox<>();
		comboFilterCol.addItem(LABELS.getString("lblFilterFull"));
		comboFilterCol.addItem(LABELS.getString("col_type"));
		comboFilterCol.addItem(LABELS.getString("col_device"));
		comboFilterCol.addItem(LABELS.getString("col_device_name"));
		comboFilterCol.addItem(LABELS.getString("col_keyword"));
		comboFilterCol.addActionListener( event -> setColFilter(comboFilterCol) );
		comboFilterCol.setSelectedIndex(appProp.getIntProperty(ScannerProperties.PROP_DEFAULT_FILTER_IDX));
		statusFilterPanel.add(comboFilterCol);
		statusFilterPanel.add(textFieldFilter);
		
		textFieldFilter.getDocument().addDocumentListener( (TextDocumentListener)event -> setColFilter(comboFilterCol) );
		
		JButton eraseFilterButton = new JButton(eraseFilterAction);
		eraseFilterButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		eraseFilterButton.setContentAreaFilled(false);
		eraseFilterButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, SHORTCUT_KEY), "find_erase");
		eraseFilterButton.getActionMap().put("find_erase", eraseFilterAction);
		statusFilterPanel.add(eraseFilterButton);

		Action selectAll = new UsnaAction(null, "labelSelectAll", null, "/images/list_unordered.png", null, e -> devicesTable.selectAll());
		Action selectOnLine = new SelectionAction(devicesTable, "labelSelectOnLine", null, "/images/list_online.png", i -> model.get(i).getStatus() == Status.ON_LINE);
		
		JButton btnSelectAll = new JButton(selectAll);
		btnSelectAll.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 8));
		statusFilterPanel.add(btnSelectAll);
		
		JButton btnSelectOnline = new JButton(selectOnLine);
		btnSelectOnline.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 8));
		statusFilterPanel.add(btnSelectOnline);
		
		// Selection popup
		JButton btnSelectCombo = new JButton(new ImageIcon(MainView.class.getResource("/images/expand-more.png")));
		btnSelectCombo.setContentAreaFilled(false);
		btnSelectCombo.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 3));
		statusFilterPanel.add(btnSelectCombo);

		UsnaPopupMenu selectionPopup = new UsnaPopupMenu(selectAll, selectOnLine,
				new SelectionAction(devicesTable, "labelSelectOnLineReboot", null, null, i -> model.get(i).getStatus() == Status.ON_LINE && model.get(i).rebootRequired()),
				new SelectionAction(devicesTable, "labelSelectG1", null, null, i -> model.get(i) instanceof AbstractG1Device),
				new SelectionAction(devicesTable, "labelSelectG2", null, null, i -> model.get(i) instanceof AbstractG2Device && model.get(i) instanceof AbstractG3Device == false),
				new SelectionAction(devicesTable, "labelSelectG3", null, null, i -> model.get(i) instanceof AbstractG3Device),
				new SelectionAction(devicesTable, "labelSelectWIFI", null, null, i -> model.get(i) instanceof AbstractG1Device || model.get(i) instanceof AbstractG2Device),
				new SelectionAction(devicesTable, "labelSelectBLU", null, null, i -> model.get(i) instanceof AbstractBlueDevice),
				new SelectionAction(devicesTable, "labelSelectGhosts", null, null, i -> model.get(i) instanceof GhostDevice) );

		btnSelectCombo.addActionListener(e -> selectionPopup.show(btnSelectCombo, 0, 0));
		
		statusPanel.add(statusFilterPanel, BorderLayout.EAST);
		statusPanel.setBackground(Main.STATUS_LINE);
		getContentPane().add(statusPanel, BorderLayout.SOUTH);
		
		detailedViewAction = new UsnaToggleAction(null, "action_show_detail_name", "action_show_detail_tooltip", "action_show_detail_tooltip", "/images/Plus.png", "/images/Minus.png",
				e -> SwingUtilities.invokeLater(() -> detailedView(detailedViewAction.isSelected()) ) );
		
		// Table
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		devicesTable.sortByColumn(DevicesTable.COL_IP_IDX, SortOrder.ASCENDING);
		devicesTable.loadColPos(appProp);
		devicesTable.setUptimeRenderMode(appProp.getProperty(ScannerProperties.PROP_UPTIME_MODE, ScannerProperties.PROP_UPTIME_MODE_DEFAULT));
		
		scrollPane.setViewportView(devicesTable);
		scrollPane.getViewport().setBackground(Main.BG_COLOR);

		// Toolbar
		toolBar.setBorder(BorderFactory.createEmptyBorder());
		
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
		toolBar.add(detailedViewAction);
		toolBar.add(csvExportAction);
		toolBar.add(printAction);
		toolBar.addSeparator();
		toolBar.add(appSettingsAction);
		toolBar.add(aboutAction);
		updateHideCaptions();
		getContentPane().add(toolBar, BorderLayout.NORTH);

		// devices popup
		UsnaPopupMenu tablePopup = new UsnaPopupMenu(infoAction, browseAction, backupAction, restoreAction, notesAction, reloadAction/*, loginAction*/);
		UsnaPopupMenu ghostDevPopup = new UsnaPopupMenu(reloadAction, notesAction, eraseGhostAction);

		// devices popup & double click
		devicesTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
		        if (e.getClickCount() == 2 && devicesTable.getSelectedRow() >= 0 && devicesTable.isCellEditable(devicesTable.getSelectedRow(), devicesTable.getSelectedColumn()) == false) {
		        	if(appProp.getProperty(ScannerProperties.PROP_DCLICK_ACTION, ScannerProperties.PROP_DCLICK_ACTION_DEFAULT).equals("DET") && infoAction.isEnabled()) {
		        		infoAction.actionPerformed(null);
		        	} else if(browseAction.isEnabled()) {
		        		browseAction.actionPerformed(null);
		        	}
		        } else {
		        	doPopup(e);
		        }
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
							notLogged = false;
						} else {
							notGhost = true;
							notLogged &= (d.getStatus() == Status.NOT_LOOGGED);
						}
					}
					if(ghost && notGhost == false) {
						reloadAction.setEnabled(true);
						ghostDevPopup.show(devicesTable, evt.getX(), evt.getY());
					} else {
						reloadAction.setName(notLogged ? "action_name_login" : "action_name_reload"); // reload and login are actually the same action
						reloadAction.setSmallIcon(notLogged ? "/images/Key16.png" : "/images/Loop16.png");
						tablePopup.show(devicesTable, evt.getX(), evt.getY());
					}
				}
			}
		});
		
		// Deferrables listener
		final DeferrablesContainer dc = DeferrablesContainer.getInstance();
		dc.addListener(new UsnaEventListener<DeferrableTask.Status, Integer>() {
			@Override
			public void update(DeferrableTask.Status mesgType, Integer idx) {
				if(mesgType != DeferrableTask.Status.RUNNING) {
					btnshowDeferrables.setText(dc.countWaiting() + "");
					if(mesgType == DeferrableTask.Status.FAIL && (dialogDeferrables == null || dialogDeferrables.isVisible() == false || dialogDeferrables.getExtendedState() == JFrame.ICONIFIED)) {
						btnshowDeferrables.setIcon(DEFERRED_ICON_FAIL);
					} else if(mesgType == DeferrableTask.Status.SUCCESS && btnshowDeferrables.getIcon() != DEFERRED_ICON_FAIL && (dialogDeferrables == null || dialogDeferrables.isVisible() == false || dialogDeferrables.getExtendedState() == JFrame.ICONIFIED)) {
						btnshowDeferrables.setIcon(DEFERRED_ICON_OK);
					}
				}
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				MainView.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				appProp.removeListeners();
				model.removeListeners(); // immediate; before subsequent model.close();
				storeProperties();
				dispose();
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
				model.close();
				System.exit(0);
			}
		});
		
		textFieldFilter.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, SHORTCUT_KEY), "find_focus_mw");
		textFieldFilter.getActionMap().put("find_focus_mw", new UsnaAction(e -> textFieldFilter.requestFocus()));

		comboFilterCol.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_KEY), "find_combo_mw");
		comboFilterCol.getActionMap().put("find_combo_mw", new UsnaAction(e -> {
			int selected = comboFilterCol.getSelectedIndex();
			comboFilterCol.setSelectedIndex(++selected >= comboFilterCol.getItemCount() ? 0 : selected);
			textFieldFilter.requestFocus();
		}));

		rescanAction.setEnabled(false);
		refreshAction.setEnabled(false);
		statusLabel.setText(LABELS.getString("scanning_start"));
		rowsSelectionManager();
	}
	
	private void updateHideCaptions() {
		boolean en = appProp.getBoolProperty(ScannerProperties.PROP_TOOLBAR_CAPTIONS, true) == false;
		Stream.of(toolBar.getComponents()).filter(c -> c instanceof AbstractButton).forEach(b -> ((AbstractButton)b).setHideActionText(en));
	}
	
	private void setColFilter(JComboBox<?> combo) {
		final int[] cols = switch(combo.getSelectedIndex()) {
		default -> new int[] {DevicesTable.COL_TYPE, DevicesTable.COL_DEVICE, DevicesTable.COL_NAME, DevicesTable.COL_KEYWORD, DevicesTable.COL_IP_IDX};
		case 1 -> new int[] {DevicesTable.COL_TYPE};
		case 2 -> new int[] {DevicesTable.COL_DEVICE};
		case 3 -> new int[] {DevicesTable.COL_NAME};
		case 4 -> new int[] {DevicesTable.COL_KEYWORD};
		};
		devicesTable.setRowFilter(textFieldFilter.getText(), cols);
		displayStatus();
	}
	
	private void rowsSelectionManager() {
		tableSelectionListener = e -> {
			if(e.getValueIsAdjusting() == false) {
				boolean singleSelection, singleSelectionNoGhost, selection, selectionNoGhost, selectionNoBLU;
				int selectedRows = devicesTable.getSelectedRowCount();
				singleSelection = singleSelectionNoGhost = selectedRows == 1;
				selection = selectionNoGhost = selectionNoBLU = selectedRows > 0;
				ShellyAbstractDevice d = null;
				for(int idx: devicesTable.getSelectedRows()) {
					d = model.get(devicesTable.convertRowIndexToModel(idx));
					if(d instanceof GhostDevice) {
						selectionNoGhost = singleSelectionNoGhost = false;
					} else if(d instanceof AbstractBlueDevice) {
						selectionNoBLU = false;
					}
				}
				infoAction.setEnabled(singleSelection);
				infoLogAction.setEnabled(singleSelectionNoGhost);
				checkListAction.setEnabled(selectionNoGhost);
				rebootAction.setEnabled(selectionNoGhost && selectionNoBLU);
				browseAction.setEnabled(selectionNoGhost /*&& browserSupported*/);
				backupAction.setEnabled(selection /*&& selectionNoBLU*/);
				restoreAction.setEnabled(singleSelection /*&& selectionNoBLU*/ /*&& d.getStatus() != Status.NOT_LOOGGED*/);
				devicesSettingsAction.setEnabled(selection && selectionNoBLU);
				chartAction.setEnabled(selectionNoGhost);
				scriptManagerAction.setEnabled(singleSelectionNoGhost && d instanceof AbstractG2Device);
				notesAction.setEnabled(singleSelection && appProp.getBoolProperty(ScannerProperties.PROP_USE_ARCHIVE, true));
				
				displayStatus();
			}
		};
		devicesTable.getSelectionModel().addListSelectionListener(tableSelectionListener);
		tableSelectionListener.valueChanged(new ListSelectionEvent(devicesTable, -1, -1, false));
	}
	
	private void detailedView(boolean detailed) {
		if(detailed) {
			// store normal view preferences
			super.storeProperties(appProp);
			devicesTable.saveColPos(appProp, DevicesTable.STORE_PREFIX);
			devicesTable.saveColWidth(temporaryProp, DevicesTable.STORE_PREFIX);
			final int normColCount = devicesTable.getColumnCount();
			// load extended view preferences
			devicesTable.restoreColumns();
			devicesTable.resetRowsComputedHeight();
			devicesTable.loadColPos(appProp, DevicesTable.STORE_EXT_PREFIX);
			
			String detScreenMode = appProp.getProperty(ScannerProperties.PROP_DETAILED_VIEW_SCREEN, ScannerProperties.PROP_DETAILED_VIEW_SCREEN_DEFAULT);
			if(getExtendedState() != JFrame.MAXIMIZED_BOTH) {
				if(detScreenMode.equals(ScannerProperties.PROP_DETAILED_VIEW_SCREEN_FULL)) {
					setExtendedState(JFrame.MAXIMIZED_BOTH);
				} else if(detScreenMode.equals(ScannerProperties.PROP_DETAILED_VIEW_SCREEN_HORIZONTAL)) {
					setExtendedState(JFrame.MAXIMIZED_HORIZ);
				} else if(detScreenMode.equals(ScannerProperties.PROP_DETAILED_VIEW_SCREEN_ESTIMATE)) {
					Rectangle screen = getCurrentScreenBounds();
					Rectangle current = getBounds();
					current.width = current.width * devicesTable.getColumnCount() / normColCount;
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
		if(appProp.getBoolProperty(ScannerProperties.PROP_USE_ARCHIVE, true)) {
			try {
				model.saveToStore(Paths.get(appProp.getProperty(ScannerProperties.PROP_ARCHIVE_FILE, ScannerProperties.PROP_ARCHIVE_FILE_DEFAULT)));
			} catch (IOException | RuntimeException ex) {
				LOG.error("Unexpected", ex);
				Msg.errorMsg(this, "Error storing archive");
			}
		}
		if(detailedViewAction.isSelected()) { // else -> normal view values stored on detailedView(true)
			devicesTable.saveColPos(appProp, DevicesTable.STORE_EXT_PREFIX);
			// no position/window size stored for detailed view
		} else {
			devicesTable.saveColPos(appProp, DevicesTable.STORE_PREFIX);
			super.storeProperties(appProp);
		}
		try {
			appProp.store(false);
		} catch (IOException | RuntimeException ex) {
			LOG.error("Unexpected", ex);
		}
	}

	@Override
	public void update(Devices.EventType mesgType, Integer msgBody) {
		SwingUtilities.invokeLater(() -> {
			try {
				if(mesgType == Devices.EventType.UPDATE) {
					int modelIndex = msgBody;
					ShellyAbstractDevice d = model.get(modelIndex);
					devicesTable.updateRow(d, model.getGhost(d, modelIndex), modelIndex);
				} else if(mesgType == Devices.EventType.ADD) {
					devicesTable.addRow(model.get(msgBody), model.getGhost(msgBody));
					displayStatus();
				} else if(mesgType == Devices.EventType.SUBSTITUTE) {
					devicesTable.updateRow(model.get(msgBody), model.getGhost(msgBody), msgBody);
//					devicesTable.resetRowComputedHeight(msgBody);
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
	
	@Override
	public void update(PropertyEvent e, String propKey) {
		if(ScannerProperties.PROP_TOOLBAR_CAPTIONS.equals(propKey)) {
			updateHideCaptions();
		} else if(ScannerProperties.PROP_UPTIME_MODE.equals(propKey)) {
			devicesTable.setUptimeRenderMode(appProp.getProperty(ScannerProperties.PROP_UPTIME_MODE));
			((UsnaTableModel)devicesTable.getModel()).fireTableDataChanged();
			devicesTable.columnsWidthAdapt();
		} else if(ScannerProperties.PROP_USE_ARCHIVE.equals(propKey)) {
			devicesTable.clearSelection();
		}
	}
	
	public synchronized void reserveStatusLine(boolean r) {
		statusLineReserved = r;
		if(r == false) displayStatus();
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
} //557 - 614 - 620 - 669 - 705 - 727 - 699 - 760 - 782 - 811 - 805 - 646 - 699 - 706 - 688 - 698 - 707