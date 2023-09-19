package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Cursor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Restore;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.DialogAuthentication;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.AppProperties;

public class RestoreAction extends UsnaSelectedAction {
	private static final long serialVersionUID = 1L;

	public RestoreAction(MainView w, JTable devicesTable, AppProperties appProp, Devices model) {
		super(w, "action_restore_name", "action_restore_tooltip", "/images/Upload16.png", "/images/Upload.png");
		
		setConsumer(devicesTable, modelRow -> {
			ShellyAbstractDevice device = model.get(modelRow);
			if(device.getStatus() == Status.NOT_LOOGGED) {
				Msg.errorMsg(w, LABELS.getString("msgRestoreLogin"));
				return;
			}
			final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
			try {
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
				final String fileName = device.getHostname().replaceAll("[^\\w_-]+", "_") + "." + Main.BACKUP_FILE_EXT;
				fc.setSelectedFile(new File(fileName));
				if(fc.showOpenDialog(w) == JFileChooser.APPROVE_OPTION) {
					w.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					final Map<String, JsonNode> backupJsons = UtilMiscellaneous.readBackupFile(fc.getSelectedFile());
					Map<Restore, String> test = device.restoreCheck(backupJsons);
					w.getContentPane().setCursor(Cursor.getDefaultCursor());
					Map<Restore, String> resData = new HashMap<>();
					if(test.containsKey(ShellyAbstractDevice.Restore.ERR_RESTORE_HOST) &&
							JOptionPane.showConfirmDialog(w, LABELS.getString("msgRestoreDifferent"),
							LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
						return;
					} else if(test.containsKey(ShellyAbstractDevice.Restore.ERR_RESTORE_MODEL)) {
						Msg.errorMsg(w, LABELS.getString("msgRestoreDifferentModel"));
						return;
					} else if(test.containsKey(ShellyAbstractDevice.Restore.ERR_RESTORE_CONF)) {
						Msg.errorMsg(w, LABELS.getString("msgRestoreConfigurationError"));
						return;
					} else if(test.containsKey(ShellyAbstractDevice.Restore.ERR_RESTORE_MSG)) {
						Msg.errorMsg(w, LABELS.getString(test.get(ShellyAbstractDevice.Restore.ERR_RESTORE_MSG)));
						return;
					} else {
						if(test.containsKey(ShellyAbstractDevice.Restore.WARN_RESTORE_MSG)) {
							Msg.warningMsg(w, LABELS.getString(test.get(ShellyAbstractDevice.Restore.WARN_RESTORE_MSG)));
						}
						if(test.containsKey(ShellyAbstractDevice.Restore.RESTORE_LOGIN)) {
							DialogAuthentication credentials = new DialogAuthentication(w,
									LABELS.getString("dlgAuthTitle"), device instanceof AbstractG1Device ? LABELS.getString("labelUser") : null,
											LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
							credentials.setUser(test.get(ShellyAbstractDevice.Restore.RESTORE_LOGIN));
							credentials.setMessage(LABELS.getString("msgRestoreEnterLogin"));
							credentials.editableUser(false);
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(ShellyAbstractDevice.Restore.RESTORE_LOGIN, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						if(test.containsKey(ShellyAbstractDevice.Restore.RESTORE_WI_FI1)) {
							DialogAuthentication credentials = new DialogAuthentication(w,
									LABELS.getString("dlgSetWIFI"), LABELS.getString("dlgSetSSID"), LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
							credentials.setUser(test.get(ShellyAbstractDevice.Restore.RESTORE_WI_FI1));
							credentials.setMessage(LABELS.getString("msgRestoreEnterWIFI1"));
							credentials.editableUser(false);
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(ShellyAbstractDevice.Restore.RESTORE_WI_FI1, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						if(test.containsKey(ShellyAbstractDevice.Restore.RESTORE_WI_FI2)) {
							DialogAuthentication credentials = new DialogAuthentication(w,
									LABELS.getString("dlgSetWIFIBackup"), LABELS.getString("dlgSetSSID"), LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
							credentials.setUser(test.get(ShellyAbstractDevice.Restore.RESTORE_WI_FI2));
							credentials.setMessage(LABELS.getString("msgRestoreEnterWIFI2"));
							credentials.editableUser(false);
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(ShellyAbstractDevice.Restore.RESTORE_WI_FI2, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						if(test.containsKey(ShellyAbstractDevice.Restore.RESTORE_WI_FI_AP)) {
							DialogAuthentication credentials = new DialogAuthentication(w,
									LABELS.getString("dlgSetWIFI_AP"), null, LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
							credentials.setUser(test.get(ShellyAbstractDevice.Restore.RESTORE_WI_FI_AP));
							credentials.setMessage(LABELS.getString("msgRestoreEnterWIFI_AP"));
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(ShellyAbstractDevice.Restore.RESTORE_WI_FI_AP, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						if(test.containsKey(ShellyAbstractDevice.Restore.RESTORE_MQTT)) {
							DialogAuthentication credentials = new DialogAuthentication(w,
									LABELS.getString("dlgSetMQTT"), LABELS.getString("labelUser"), LABELS.getString("labelPassword") /*,LABELS.getString("labelConfPassword")*/);
							credentials.setUser(test.get(ShellyAbstractDevice.Restore.RESTORE_MQTT));
							credentials.setMessage(LABELS.getString("msgRestoreEnterMQTT"));
							credentials.editableUser(false);
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(ShellyAbstractDevice.Restore.RESTORE_MQTT, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
					}
					w.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					final String ret = device.restore(backupJsons, resData);
					appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getCanonicalPath());
					device.refreshSettings();
					device.refreshStatus();
					w.update(Devices.EventType.UPDATE, modelRow);
					
					if(ret == null || ret.length() == 0) {
						if(device.rebootRequired())	{
							String ok = LABELS.getString("dlgOK");
							if(JOptionPane.showOptionDialog(w, LABELS.getString("msgRestoreSuccess"), device.getHostname(),
									JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[] {ok, LABELS.getString("action_reboot_name")}, ok) == 1 /*reboot*/) {
								device.setStatus(Status.READING);
								devicesTable.getModel().setValueAt(DevicesTable.UPDATING_BULLET, modelRow, DevicesTable.COL_STATUS_IDX);
								SwingUtilities.invokeLater(() -> model.reboot(modelRow));
							}
						} else {
							JOptionPane.showMessageDialog(w, LABELS.getString("msgRestoreSuccess"), device.getHostname(), JOptionPane.INFORMATION_MESSAGE);
						}
					} else {
						JOptionPane.showMessageDialog(w, (ret.equals(Restore.ERR_UNKNOWN.toString())) ? LABELS.getString("labelError") : ret, device.getHostname(), JOptionPane.ERROR_MESSAGE);
					}
				}
			} catch (FileNotFoundException e1) {
				Msg.errorMsg(w, String.format(LABELS.getString("action_restore_error_file"), fc.getSelectedFile().getName()));
			} catch (IOException e1) {
				Msg.errorStatusMsg(w, device, e1);
			} catch (RuntimeException e1) {
				Msg.errorMsg(e1);
			} finally {
				w.getContentPane().setCursor(Cursor.getDefaultCursor());
			}
		});
	}
}