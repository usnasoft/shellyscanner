package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Cursor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
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
	private final static String CHECK_MSG_PREFIX = "msgRestore";
	private final static String ERROR_MSG_PREFIX = "errRestore";

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
					final Map<String, JsonNode> backupJsons = readBackupFile(fc.getSelectedFile().toPath());
					final Map<RestoreMsg, Object> test = device.restoreCheck(backupJsons);
					
					mainView.getContentPane().setCursor(Cursor.getDefaultCursor());

					for(Map.Entry<RestoreMsg, Object> e: test.entrySet()) {
						if(e.getKey().getType() == RestoreMsg.Type.PRE &&
								JOptionPane.showConfirmDialog(mainView, String.format(LABELS.getString(CHECK_MSG_PREFIX + e.getKey().name()), e.getValue()),
										LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
							return;

						}
					}
					
					for(Map.Entry<RestoreMsg, Object> e: test.entrySet()) {
						if(e.getKey().getType() == RestoreMsg.Type.ERROR) {
							if(e.getValue() != null) {
								Msg.errorMsg(mainView, String.format(LABELS.getString(CHECK_MSG_PREFIX + e.getKey().name()), e.getValue()));
							} else {
								Msg.errorMsg(mainView, LABELS.getString(CHECK_MSG_PREFIX + e.getKey().name()));
							}
							return;
						}
					}
					String warn = test.entrySet().stream().filter(e -> e.getKey().getType() == RestoreMsg.Type.WARN).
							map(e -> LABELS.getString(CHECK_MSG_PREFIX + e.getKey().name())).collect(Collectors.joining("<br><br>"));
					if(warn.isEmpty() == false) {
						Msg.warningMsg(mainView, "<html>" + warn);
					}

					mainView.getContentPane().setCursor(Cursor.getDefaultCursor());
					Map<RestoreMsg, String> resData = new HashMap<>();

					if(test.containsKey(RestoreMsg.RESTORE_LOGIN)) {
						DialogAuthentication credentials = new DialogAuthentication(mainView,
								LABELS.getString("dlgAuthTitle"), device instanceof AbstractG1Device ? LABELS.getString("labelUser") : null,
										LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
						credentials.setUser(test.get(RestoreMsg.RESTORE_LOGIN).toString());
						credentials.setMessage(LABELS.getString(CHECK_MSG_PREFIX + "RESTORE_LOGIN"));
						credentials.editableUser(false);
						credentials.setVisible(true);
						if(credentials.getUser() != null) {
							resData.put(RestoreMsg.RESTORE_LOGIN, new String(credentials.getPassword()));
						}
						credentials.dispose();
					}
					if(test.containsKey(RestoreMsg.RESTORE_WI_FI1)) {
						DialogAuthentication credentials = new DialogAuthentication(mainView,
								LABELS.getString("dlgSetWIFI"), LABELS.getString("dlgSetSSID"), LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
						credentials.setUser(test.get(RestoreMsg.RESTORE_WI_FI1).toString());
						credentials.setMessage(LABELS.getString(CHECK_MSG_PREFIX + "RESTORE_WI_FI1"));
						credentials.editableUser(false);
						credentials.setVisible(true);
						if(credentials.getUser() != null) {
							resData.put(RestoreMsg.RESTORE_WI_FI1, new String(credentials.getPassword()));
						}
						credentials.dispose();
					}
					if(test.containsKey(RestoreMsg.RESTORE_WI_FI2)) {
						DialogAuthentication credentials = new DialogAuthentication(mainView,
								LABELS.getString("dlgSetWIFIBackup"), LABELS.getString("dlgSetSSID"), LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
						credentials.setUser(test.get(RestoreMsg.RESTORE_WI_FI2).toString());
						credentials.setMessage(LABELS.getString(CHECK_MSG_PREFIX + "RESTORE_WI_FI2"));
						credentials.editableUser(false);
						credentials.setVisible(true);
						if(credentials.getUser() != null) {
							resData.put(RestoreMsg.RESTORE_WI_FI2, new String(credentials.getPassword()));
						}
						credentials.dispose();
					}
					if(test.containsKey(RestoreMsg.RESTORE_WI_FI_AP)) {
						DialogAuthentication credentials = new DialogAuthentication(mainView,
								LABELS.getString("dlgSetWIFI_AP"), null, LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
						credentials.setUser(test.get(RestoreMsg.RESTORE_WI_FI_AP).toString());
						credentials.setMessage(LABELS.getString(CHECK_MSG_PREFIX + "RESTORE_WI_FI_AP"));
						credentials.setVisible(true);
						if(credentials.getUser() != null) {
							resData.put(RestoreMsg.RESTORE_WI_FI_AP, new String(credentials.getPassword()));
						}
						credentials.dispose();
					}
					if(test.containsKey(RestoreMsg.RESTORE_MQTT)) {
						DialogAuthentication credentials = new DialogAuthentication(mainView,
								LABELS.getString("dlgSetMQTT"), LABELS.getString("labelUser"), LABELS.getString("labelPassword") /*,LABELS.getString("labelConfPassword")*/);
						credentials.setUser(test.get(RestoreMsg.RESTORE_MQTT).toString());
						credentials.setMessage(LABELS.getString(CHECK_MSG_PREFIX + "RESTORE_MQTT"));
						credentials.editableUser(false);
						credentials.setVisible(true);
						if(credentials.getUser() != null) {
							resData.put(RestoreMsg.RESTORE_MQTT, new String(credentials.getPassword()));
						}
						credentials.dispose();
					}
					boolean overwriteScriptNames = false;
					if(test.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE) && 
							JOptionPane.showConfirmDialog(mainView,
									String.format(LABELS.getString(CHECK_MSG_PREFIX + "QUESTION_RESTORE_SCRIPTS_OVERRIDE"), test.get(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE)),
									LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
						resData.put(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE, "true");
						overwriteScriptNames = true;
					}
					if(test.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP) && (overwriteScriptNames || test.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE) == false) &&
							JOptionPane.showConfirmDialog(mainView,
									String.format(LABELS.getString(CHECK_MSG_PREFIX + "QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP"), test.get(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP)),
									LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
						resData.put(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP, "true");
					}

					mainView.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getCanonicalPath());
					List<String> restoreResult = device.restore(backupJsons, resData);
//					restoreResult = restoreResult.stream().filter(err -> { // remove warnings previously shown
//						try {
//							return test.containsKey(RestoreMsg.valueOf(err)) == false; // test.containsKey(RestoreMsg.valueOf(ret) ->  warning already showed
//						} catch(RuntimeException e) {
//							return true;
//						}
//					}).collect(Collectors.toList());
					final String ret = erroreMsg(restoreResult);

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
							SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainView, LABELS.getString("msgRestoreQueue"), device.getHostname(), JOptionPane.WARNING_MESSAGE));

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
							Msg.showMsg(mainView, ret, device.getHostname(), JOptionPane.ERROR_MESSAGE);
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
		String err = errors.stream().filter(s-> s != null && s.length() > 0 && s.startsWith("->r_step:") == false)
				.map(s -> LABELS.containsKey(ERROR_MSG_PREFIX + s) ? LABELS.getString(ERROR_MSG_PREFIX + s) : s).distinct().collect(Collectors.joining("\n"));
		if(err.isEmpty() == false) {
			LOG.debug(errors.stream().map(s -> s == null ? "-" : s).collect(Collectors.joining("\n")));
		}
		return err;
	}

	private static Map<String, JsonNode> readBackupFile(final Path file) throws IOException {
		try(FileSystem fs = FileSystems.newFileSystem(file); Stream<Path> pathStream = Files.list(fs.getPath("/"))) {
			final Map<String, JsonNode> backupJsons = new HashMap<>();
			final ObjectMapper jsonMapper = new ObjectMapper();
			pathStream.forEach(p -> {
				try {
					if(p.toString().endsWith(".json")) {
						backupJsons.put(p.getFileName().toString(), jsonMapper.readTree(Files.readString(p)));
					} else {
						backupJsons.put(p.getFileName().toString() + ".json", jsonMapper.createObjectNode().put("code", Files.readString(p, StandardCharsets.UTF_8)));
					}
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			});
			return backupJsons;
		} catch(RuntimeException e) {
			if(e.getCause() instanceof IOException) {
				throw (IOException)e.getCause();
			} 
			throw e;
		}
	}
}