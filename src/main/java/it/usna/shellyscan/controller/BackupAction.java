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
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.Msg;
import it.usna.util.AppProperties;

public class BackupAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(BackupAction.class);

	public BackupAction(MainView w, DevicesTable devicesTable, AppProperties appProp, Devices model) {
		super(w, "action_back_name", "action_back_tooltip", "/images/Download16.png", "/images/Download.png");

		setActionListener(e -> {
			int[] ind = devicesTable.getSelectedRows();
			final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));

			class BackWorker extends SwingWorker<String, Object> {
				@Override
				protected String doInBackground() {
					w.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					w.reserveStatusLine(true);
					String res = "<html>";
					for(int j = 0; j < ind.length; j++) {
						ShellyAbstractDevice d = model.get(devicesTable.convertRowIndexToModel(ind[j]));
						String hostName = d.getHostname();
						w.setStatus(String.format(LABELS.getString("statusBackup"), j + 1, ind.length, hostName));
						try {
							final boolean connected;
							if(ind.length > 1) {
								connected = d.backup(new File(fc.getSelectedFile(), hostName.replaceAll("[^\\w_-]+", "_") + "." + Main.BACKUP_FILE_EXT));
							} else {
								connected = d.backup(fc.getSelectedFile());
							}
							res += String.format(LABELS.getString(connected ? "dlgSetMultiMsgOk" : "dlgSetMultiMsgStored"), hostName) + "<br>";
						} catch (IOException | RuntimeException e1) {
							res += String.format(LABELS.getString("dlgSetMultiMsgFail"), hostName) + "<br>";
							LOG.debug("{}", d.getHostname(), e1);
						}
					}
					return res;
				}

				@Override
				protected void done() {
					try {
						w.reserveStatusLine(false);
						Msg.showHtmlMessageDialog(w, get(), LABELS.getString("titleBackupDone"), JOptionPane.INFORMATION_MESSAGE);
					} catch (Exception e) {
						Msg.errorMsg(e);
					} finally {
						w.getRootPane().setCursor(Cursor.getDefaultCursor());
					}
				}
			}

			if(ind.length > 1) {
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if(fc.showSaveDialog(w) == JFileChooser.APPROVE_OPTION) {
					new BackWorker().execute();
					appProp.setProperty("LAST_PATH", fc.getSelectedFile().getPath());
				}
			} else if(ind.length == 1) {
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
				ShellyAbstractDevice device = model.get(devicesTable.convertRowIndexToModel(ind[0]));
				String fileName = device.getHostname().replaceAll("[^\\w_-]+", "_") + ".sbk";
				fc.setSelectedFile(new File(fileName));
				if(fc.showSaveDialog(w) == JFileChooser.APPROVE_OPTION) {
					new BackWorker().execute();
					appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
				}
			}
		});
	}
}