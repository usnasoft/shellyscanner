package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Cursor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import it.usna.util.AppProperties;

public class RestoreAction extends UsnaSelectedAction {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(RestoreAction.class);

	public RestoreAction(MainView mainView, JTable devicesTable, AppProperties appProp, Devices model) {
		super(mainView, "action_restore_name", "action_restore_tooltip", "/images/Upload16.png", "/images/Upload.png");

		setConsumer(devicesTable, modelRow -> {
			ShellyAbstractDevice device = model.get(modelRow);
			final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
			try {
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
				final String fileName = device.getHostname().replaceAll("[^\\w_-]+", "_") + "." + Main.BACKUP_FILE_EXT;
				fc.setSelectedFile(new File(fileName));
				if(fc.showOpenDialog(mainView) == JFileChooser.APPROVE_OPTION) {
					mainView.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					final Map<String, JsonNode> backupJsons = readBackupFile(fc.getSelectedFile());
					Map<Restore, String> test = device.restoreCheck(backupJsons);

					mainView.getContentPane().setCursor(Cursor.getDefaultCursor());
					Map<Restore, String> resData = new HashMap<>();
					if(test.containsKey(Restore.ERR_RESTORE_HOST) &&
							JOptionPane.showConfirmDialog(mainView, String.format(LABELS.getString("msgRestoreDifferent"), test.get(Restore.ERR_RESTORE_HOST)),
									LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
						return;
					} else if(test.containsKey(Restore.ERR_RESTORE_MODEL)) {
						Msg.errorMsg(mainView, LABELS.getString("msgRestoreDifferentModel"));
						return;
					} else if(test.containsKey(Restore.ERR_RESTORE_CONF)) {
						Msg.errorMsg(mainView, LABELS.getString("msgRestoreConfigurationError"));
						return;
					} else if(test.containsKey(Restore.ERR_RESTORE_MSG)) {
						Msg.errorMsg(mainView, LABELS.getString(test.get(Restore.ERR_RESTORE_MSG)));
						return;
					} else {
						if(test.containsKey(Restore.WARN_RESTORE_MSG)) {
							Msg.warningMsg(mainView, LABELS.getString(test.get(Restore.WARN_RESTORE_MSG)));
						}
						if(test.containsKey(Restore.RESTORE_LOGIN)) {
							DialogAuthentication credentials = new DialogAuthentication(mainView,
									LABELS.getString("dlgAuthTitle"), device instanceof AbstractG1Device ? LABELS.getString("labelUser") : null,
											LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
							credentials.setUser(test.get(Restore.RESTORE_LOGIN));
							credentials.setMessage(LABELS.getString("msgRestoreEnterLogin"));
							credentials.editableUser(false);
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(Restore.RESTORE_LOGIN, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						if(test.containsKey(Restore.RESTORE_WI_FI1)) {
							DialogAuthentication credentials = new DialogAuthentication(mainView,
									LABELS.getString("dlgSetWIFI"), LABELS.getString("dlgSetSSID"), LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
							credentials.setUser(test.get(Restore.RESTORE_WI_FI1));
							credentials.setMessage(LABELS.getString("msgRestoreEnterWIFI1"));
							credentials.editableUser(false);
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(Restore.RESTORE_WI_FI1, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						if(test.containsKey(Restore.RESTORE_WI_FI2)) {
							DialogAuthentication credentials = new DialogAuthentication(mainView,
									LABELS.getString("dlgSetWIFIBackup"), LABELS.getString("dlgSetSSID"), LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
							credentials.setUser(test.get(Restore.RESTORE_WI_FI2));
							credentials.setMessage(LABELS.getString("msgRestoreEnterWIFI2"));
							credentials.editableUser(false);
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(Restore.RESTORE_WI_FI2, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						if(test.containsKey(Restore.RESTORE_WI_FI_AP)) {
							DialogAuthentication credentials = new DialogAuthentication(mainView,
									LABELS.getString("dlgSetWIFI_AP"), null, LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
							credentials.setUser(test.get(Restore.RESTORE_WI_FI_AP));
							credentials.setMessage(LABELS.getString("msgRestoreEnterWIFI_AP"));
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(Restore.RESTORE_WI_FI_AP, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						if(test.containsKey(Restore.RESTORE_MQTT)) {
							DialogAuthentication credentials = new DialogAuthentication(mainView,
									LABELS.getString("dlgSetMQTT"), LABELS.getString("labelUser"), LABELS.getString("labelPassword") /*,LABELS.getString("labelConfPassword")*/);
							credentials.setUser(test.get(Restore.RESTORE_MQTT));
							credentials.setMessage(LABELS.getString("msgRestoreEnterMQTT"));
							credentials.editableUser(false);
							credentials.setVisible(true);
							if(credentials.getUser() != null) {
								resData.put(Restore.RESTORE_MQTT, new String(credentials.getPassword()));
							}
							credentials.dispose();
						}
						boolean overwriteScriptNames = false;
						final String no = UIManager.getString("OptionPane.noButtonText");
						if(test.containsKey(Restore.QUESTION_RESTORE_SCRIPTS_OVERRIDE) && 
							JOptionPane.showOptionDialog(mainView,
									String.format(LABELS.getString("msgRestoreScriptsOverride"), test.get(Restore.QUESTION_RESTORE_SCRIPTS_OVERRIDE)),
									LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[] {UIManager.getString("OptionPane.yesButtonText"), no}, no) == JOptionPane.YES_OPTION) {
								resData.put(Restore.QUESTION_RESTORE_SCRIPTS_OVERRIDE, "true");
								overwriteScriptNames = true;
						}
						if(test.containsKey(Restore.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP) && overwriteScriptNames &&
							JOptionPane.showConfirmDialog(mainView,
									String.format(LABELS.getString("msgRestoreScriptsEnableLikeBackedUp"), test.get(Restore.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP)),
									LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
								resData.put(Restore.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP, "true");
						}
					}
					mainView.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getCanonicalPath());
					final String ret = erroreMsg(device.restore(backupJsons, resData));

					if(ret == null || ret.length() == 0) {
						Thread.sleep(Devices.MULTI_QUERY_DELAY);
						device.refreshSettings();
						Thread.sleep(Devices.MULTI_QUERY_DELAY);
						device.refreshStatus();

						if(device.rebootRequired())	{
							String ok = LABELS.getString("dlgOK");
							if(JOptionPane.showOptionDialog(mainView, LABELS.getString("msgRestoreSuccessReboot"), device.getHostname(),
									JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[] {ok, LABELS.getString("action_reboot_name")}, ok) == 1 /*reboot*/) {
								device.setStatus(Status.READING);
								devicesTable.getModel().setValueAt(DevicesTable.UPDATING_BULLET, modelRow, DevicesTable.COL_STATUS_IDX);
								SwingUtilities.invokeLater(() -> model.reboot(modelRow));
							}
						} else {
							JOptionPane.showMessageDialog(mainView, LABELS.getString("msgRestoreSuccess"), device.getHostname(), JOptionPane.INFORMATION_MESSAGE);
						}
					} else {	
						if(device.getStatus() == Status.OFF_LINE /*|| device.getStatus() == Status.NOT_LOOGGED*/ || device.getStatus() == Status.GHOST) { // if error happened because the device is off-line -> try to queue action in DeferrablesContainer
							LOG.debug("Interactive Restore error {} {}", device, ret);
							SwingUtilities.invokeLater(() ->
							JOptionPane.showMessageDialog(mainView, LABELS.getString("msgRestoreQueue"), device.getHostname(), JOptionPane.WARNING_MESSAGE));

							DeferrablesContainer dc = DeferrablesContainer.getInstance();
							dc.addOrUpdate(modelRow, DeferrableTask.Type.RESTORE, LABELS.getString("action_restore_tooltip"), (def, dev) -> {
								final String restoreError = erroreMsg(dev.restore(backupJsons, resData));
								if(restoreError.length() > 0) {
									def.setStatus(DeferrableTask.Status.FAIL);
								}
								try {
									if(device.getStatus() != Status.OFF_LINE) {
										Thread.sleep(Devices.MULTI_QUERY_DELAY);
										dev.refreshSettings();
										Thread.sleep(Devices.MULTI_QUERY_DELAY);
										dev.refreshStatus();
										mainView.update(Devices.EventType.UPDATE, modelRow);
									}
								} catch(Exception e) {}
								return restoreError;
							});
						} else {
							LOG.error("Restore error {} {}", device, ret);
							Msg.showMsg(mainView, (ret.equals(Restore.ERR_UNKNOWN.name())) ? LABELS.getString("labelError") : ret, device.getHostname(), JOptionPane.ERROR_MESSAGE);
						}
					}

					mainView.update(Devices.EventType.UPDATE, modelRow);
				}
			} catch (FileNotFoundException | NoSuchFileException e1) {
				Msg.errorMsg(mainView, String.format(LABELS.getString("action_restore_error_file"), fc.getSelectedFile().getName()));
			} catch (IOException e1) {
				Msg.errorStatusMsg(mainView, device, e1);
			} catch (InterruptedException | RuntimeException e1) {
				Msg.errorMsg(mainView, e1);
			} finally {
				mainView.getContentPane().setCursor(Cursor.getDefaultCursor());
			}
		});
	}

	private static String erroreMsg(List<String> errors) {
		return errors.stream().filter(s-> s != null && s.length() > 0).map(s -> LABELS.containsKey(s) ? LABELS.getString(s) : s).distinct().collect(Collectors.joining("\n"));
	}

	private static Map<String, JsonNode> readBackupFile(final File file) throws IOException {
		final ObjectMapper jsonMapper = new ObjectMapper();
		try (ZipFile in = new ZipFile(file, StandardCharsets.UTF_8)) {
			final Map<String, JsonNode> backupJsons = in.stream().filter(entry -> entry.getName().endsWith(".json")).collect(Collectors.toMap(ZipEntry::getName, entry -> {
				try (InputStream is = in.getInputStream(entry)) {
					return jsonMapper.readTree(is);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}));
//			final Map<String, JsonNode> backupScripts = new HashMap<>(backupJsons);
			//add all other objects as byte[]
			in.stream().filter(entry -> entry.getName().endsWith(".mjs")).forEach(entry -> {
				try (InputStream is = in.getInputStream(entry)) {
					backupJsons.put(entry.getName() + ".json", jsonMapper.createObjectNode().put("code", new String(is.readAllBytes(), StandardCharsets.UTF_8)));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			});
			//combine jsonMapper and backupObject at same depth
//			backupJsons.putAll(backupScripts);
			return backupJsons;
		} catch(RuntimeException e) {
			if(e.getCause() instanceof IOException) {
				throw (IOException)e.getCause();
			} 
			throw e;
		}
	}
}