package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Cursor;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.Msg;
import it.usna.util.AppProperties;

public class BackupAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(BackupAction.class);

	public BackupAction(MainView mainView, DevicesTable devicesTable, AppProperties appProp, Devices model) {
		super(mainView, "action_back_name", "action_back_tooltip", "/images/Download16.png", "/images/Download.png");

		setActionListener(e -> {
			final int[] ind = devicesTable.getSelectedRows();
			final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));

			class BackWorker extends SwingWorker<String, Object> {
				@Override
				protected String doInBackground() {
					mainView.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					mainView.reserveStatusLine(true);
					String res = "<html>";
					for(int j = 0; j < ind.length; j++) {
						final int modelRow = devicesTable.convertRowIndexToModel(ind[j]);
						final ShellyAbstractDevice d = model.get(modelRow);
						final String hostName = d.getHostname();
						mainView.setStatus(String.format(LABELS.getString("statusBackup"), j + 1, ind.length, hostName));
						final File outFile = (ind.length > 1) ?
								new File(fc.getSelectedFile(), hostName.replaceAll("[^\\w_-]+", "_") + "." + Main.BACKUP_FILE_EXT) : fc.getSelectedFile();
						try {
							final boolean connected = d.backup(outFile);
							res += String.format(LABELS.getString(connected ? "dlgSetMultiMsgOk" : "dlgSetMultiMsgStored"), hostName) + "<br>";
						} catch (IOException | RuntimeException e1) {
							if(d.getStatus() == Status.OFF_LINE || d instanceof GhostDevice) { // if error happened because the device is off-line -> try to queue action in DeferrablesContainer
								LOG.debug("Interactive Backup error {}", d);
								
								String taskDescription = LABELS.getString("action_back_tooltip");
								DeferrablesContainer dc = DeferrablesContainer.getInstance(model);
								if(dc.indexOf(modelRow, taskDescription) < 0) {
									dc.add(modelRow, taskDescription, (def, dev) -> {
										dev.backup(outFile);
										return null;
									});
								}
								res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), hostName) + "<br>";
							} else {
								LOG.debug("Backup error {}", d.getHostname(), e1);
								res += String.format(LABELS.getString("dlgSetMultiMsgFail"), hostName) + "<br>";	
							}
						}
					}
					return res;
				}

				@Override
				protected void done() {
					try {
						mainView.reserveStatusLine(false);
						Msg.showHtmlMessageDialog(mainView, get(), LABELS.getString("titleBackupDone"), JOptionPane.INFORMATION_MESSAGE);
					} catch (Exception e) {
						Msg.errorMsg(e);
					} finally {
						mainView.getRootPane().setCursor(Cursor.getDefaultCursor());
					}
				}
			}

			if(ind.length > 1) {
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if(fc.showSaveDialog(mainView) == JFileChooser.APPROVE_OPTION) {
					new BackWorker().execute();
					appProp.setProperty("LAST_PATH", fc.getSelectedFile().getPath());
				}
			} else if(ind.length == 1) {
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
				ShellyAbstractDevice device = model.get(devicesTable.convertRowIndexToModel(ind[0]));
				String fileName = device.getHostname().replaceAll("[^\\w_-]+", "_") + "." + Main.BACKUP_FILE_EXT;
				fc.setSelectedFile(new File(fileName));
				if(fc.showSaveDialog(mainView) == JFileChooser.APPROVE_OPTION) {
					new BackWorker().execute();
					appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
				}
			}
		});
	}
}