package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Cursor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
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

public class RestoreAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(RestoreAction.class);
	private final static String CHECK_MSG_PREFIX = "msgRestore";
	private final static String ERROR_MSG_PREFIX = "errRestore";
	private final static String QUEUE_RET = "QUEUE";
	private final static String CANCEL_RET = "CANC";
	private SwingWorker<String, Object> worker;

	public RestoreAction(MainView mainView, DevicesTable devicesTable, AppProperties appProp, Devices model) {
		super(mainView, "action_restore_name", "action_restore_tooltip", "/images/Upload16.png", "/images/Upload.png");

		setActionListener(event -> {
			if(worker != null && worker.isDone() == false) {
				Msg.showMsg(mainView, "msgRestoreRunning", LABELS.getString("action_restore_tooltip"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			final int[] sel = devicesTable.getSelectedModelRows();
			final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
			if(sel.length == 1) {
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
				ShellyAbstractDevice device = model.get(sel[0]);

				fc.setSelectedFile(new java.io.File(BackupAction.defFileName(device)));
			} else { // multiple restore
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}

			if(fc.showOpenDialog(mainView) == JFileChooser.APPROVE_OPTION) {

				class RestoreWorker extends SwingWorker<String, Object> {
					@Override
					protected String doInBackground() {
						mainView.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						mainView.reserveStatusLine(true);
						if(sel.length == 1) { // single device
							ShellyAbstractDevice device = model.get(sel[0]);
							mainView.setStatus(String.format(LABELS.getString("statusRestore"), 1, 1, device.getHostname()));
							try {
								appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getCanonicalPath());
								String restoreRet = restoreDevice(mainView, device, model, sel[0], fc.getSelectedFile().toPath(), false);
								if(restoreRet == null) {
									if(device.rebootRequired())	{
										String ok = LABELS.getString("dlgOK");
										if(JOptionPane.showOptionDialog(mainView, LABELS.getString("msgRestoreSuccessReboot"), LABELS.getString("msgRestoreTitle") + " - " + device.getHostname(),
												JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[] {ok, LABELS.getString("action_reboot_name")}, ok) == 1 /*reboot*/) {
											device.setStatus(Status.READING);
											model.reboot(sel[0]);
										}
									} else {
										Msg.showHtmlMessageDialog(mainView, "<html>" + String.format(LABELS.getString("dlgSetMultiMsgOk"), device.getHostname()), LABELS.getString("titleRestoreDone"), JOptionPane.INFORMATION_MESSAGE);
									}
								} else if(QUEUE_RET.equals(restoreRet)) {
									JOptionPane.showMessageDialog(mainView, LABELS.getString("msgRestoreQueue"), device.getHostname(), JOptionPane.WARNING_MESSAGE);
								} else if(CANCEL_RET.equals(restoreRet) == false) { // CANCEL_RET follows a showConfirmDialog -> no message here
									Msg.showMsg(mainView, restoreRet, LABELS.getString("msgRestoreTitle") + " - " + device.getHostname(), JOptionPane.ERROR_MESSAGE);
								}
							} catch (FileNotFoundException | NoSuchFileException e1) {
								Msg.errorMsg(mainView, String.format(LABELS.getString("msgFileNotFound"), fc.getSelectedFile().getName()));
							} catch (IOException e1) {
								if(Msg.errorStatusMsg(mainView, device, e1) == false) {
									LOG.error("Restore error", e1);
								}
							} catch (InterruptedException | RuntimeException e1) {
								Msg.errorMsg(mainView, e1);
							}
						} else { // multiple devices
							appProp.setProperty("LAST_PATH", fc.getSelectedFile().getPath());
							String res = "<html>";
							for(int i = 0; i < sel.length; i++) {
								ShellyAbstractDevice device = model.get(sel[i]);
								mainView.setStatus(String.format(LABELS.getString("statusRestore"), i + 1, sel.length, device.getHostname()));
//								final File inFile = new File(fc.getSelectedFile(), BackupAction.defFileName(device));
								final Path inFile = fc.getSelectedFile().toPath().resolve(BackupAction.defFileName(device));
								try {
									String restoreRet = restoreDevice(mainView, device, model, sel[i], inFile, true);
									if(restoreRet == null) {
										res += String.format(LABELS.getString("dlgSetMultiMsgOk"), device.getHostname(), restoreRet) + "<br>";
									} else if(QUEUE_RET.equals(restoreRet)) {
										res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname(), restoreRet) + "<br>";
									} else if(CANCEL_RET.equals(restoreRet)) {
										res += String.format(LABELS.getString("dlgSetMultiMsgCancel"), device.getHostname(), restoreRet) + "<br>";
									} else if(restoreRet.length() < 50) {
										res += String.format(LABELS.getString("dlgSetMultiMsgFailReason"), device.getHostname(), restoreRet) + "<br>";
									} else {
										res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + "<br>";
									}
								} catch (FileNotFoundException | NoSuchFileException e1) {
									res += String.format(LABELS.getString("dlgSetMultiMsgFailReason"), device.getHostname(), String.format(LABELS.getString("msgFileNotFound"), inFile.getFileName().toString())) + "<br>";
								} catch (IOException | InterruptedException | RuntimeException e1) {
									res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + "<br>";
									LOG.error("restore", e1);
								}
							}
							Msg.showHtmlMessageDialog(mainView, res, LABELS.getString("titleRestoreDone"), JOptionPane.PLAIN_MESSAGE);
						}
						return null;
					}

					@Override
					protected void done() {
						mainView.reserveStatusLine(false);
						mainView.getRootPane().setCursor(Cursor.getDefaultCursor());
						worker = null;
					}
				}
				
				new RestoreWorker().execute();
			}
		});
	}

	private static String restoreDevice(MainView mainView, final ShellyAbstractDevice device, Devices model, final int modelRow, final Path file, boolean multi) throws IOException, InterruptedException {
		try {
			final Map<String, JsonNode> backupJsons = readBackupFile(file);
			final Map<RestoreMsg, Object> test = device.restoreCheck(backupJsons);

			for(Map.Entry<RestoreMsg, Object> e: test.entrySet()) {
				if(e.getKey().getType() == RestoreMsg.Type.PRE &&
						JOptionPane.showConfirmDialog(mainView, String.format(LABELS.getString(CHECK_MSG_PREFIX + e.getKey().name()), e.getValue()),
								LABELS.getString("msgRestoreTitle") + " - " + device.getHostname(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
					return CANCEL_RET;
				}
			}

			for(Map.Entry<RestoreMsg, Object> e: test.entrySet()) {
				if(e.getKey().getType() == RestoreMsg.Type.ERROR) {
					Object val = e.getValue();
					if(val != null) {
						Stream<Object> args = val instanceof Object[] arr ? Stream.of(arr) : Stream.of(val);
						args = args.map(v -> LABELS.containsKey("lbl_" + device.getTypeID() + v) ? LABELS.getString("lbl_" + device.getTypeID() + v) : v);
						return String.format(LABELS.getString(CHECK_MSG_PREFIX + e.getKey().name()), args.toArray());
					} else {
						return LABELS.getString(CHECK_MSG_PREFIX + e.getKey().name());
					}
				}
			}
			String warn = test.entrySet().stream().filter(e -> e.getKey().getType() == RestoreMsg.Type.WARN).map(e -> LABELS.getString(CHECK_MSG_PREFIX + e.getKey().name()))
					.collect(Collectors.joining("<br><br>"));
			if(warn.isEmpty() == false) {
				Msg.warningMsg(mainView, "<html>" + warn);
			}

			Map<RestoreMsg, String> resData = new HashMap<>();

			if(test.containsKey(RestoreMsg.RESTORE_LOGIN) && multi == false) {
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
			if(test.containsKey(RestoreMsg.RESTORE_WI_FI1) && multi == false) {
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
			if(test.containsKey(RestoreMsg.RESTORE_WI_FI2) && multi == false) {
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
			if(test.containsKey(RestoreMsg.RESTORE_WI_FI_AP) && multi == false) {
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
			if(test.containsKey(RestoreMsg.RESTORE_MQTT) && multi == false) {
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
			if(multi) {
				resData.put(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE, "true");
				resData.put(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP, "true");
			} else {
				boolean overwriteScriptNames = false;
				String rename = LABELS.getString("lblRename");
				if(test.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE) && 
						JOptionPane.showOptionDialog(mainView,
								String.format(LABELS.getString(CHECK_MSG_PREFIX + "QUESTION_RESTORE_SCRIPTS_OVERRIDE"), test.get(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE)),
								LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
								null, new Object[] {rename, LABELS.getString("lblOverwrite")}, rename) == 1 /*overwrite*/) {
					resData.put(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE, "true");
					overwriteScriptNames = true;
				}
				if(test.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP) && (overwriteScriptNames || test.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE) == false) &&
						JOptionPane.showConfirmDialog(mainView,
								String.format(LABELS.getString(CHECK_MSG_PREFIX + "QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP"), test.get(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP)),
								LABELS.getString("msgRestoreTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					resData.put(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP, "true");
				}
			}

			model.pauseRefresh(modelRow);
			final List<String> restoreResult = device.restore(backupJsons, resData); // Do restore
			final String ret = erroreMsg(restoreResult);

			if(ret == null || ret.isEmpty()) {
				Thread.sleep(Devices.MULTI_QUERY_DELAY);
				device.refreshSettings();
				Thread.sleep(Devices.MULTI_QUERY_DELAY);
				device.refreshStatus();
				mainView.update(Devices.EventType.UPDATE, modelRow);
				return null;
			} else {
				if(device.getStatus() == Status.OFF_LINE || device.getStatus() == Status.NOT_LOOGGED || device.getStatus() == Status.GHOST) { // if error caused by "device is off-line" -> try to queue action in DeferrablesContainer
					LOG.debug("Interactive Restore error {} {}", device, ret);

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
					return QUEUE_RET;
				} else {
					LOG.error("Restore error {} {}", device, ret);
					mainView.update(Devices.EventType.UPDATE, modelRow);
					return ret;
				}
			}
		} finally {
			model.activateRefresh(modelRow);
		}
	}

	private static String erroreMsg(List<String> errors) {
		String err = errors.stream().filter(s-> s != null && s.length() > 0 && s.startsWith("->r_step:") == false)
				.map(s -> LABELS.containsKey(ERROR_MSG_PREFIX + s) ? LABELS.getString(ERROR_MSG_PREFIX + s) : s).distinct().collect(Collectors.joining("\n"));
		if(err.isEmpty() == false) {
			LOG.debug(errors.stream().map(s -> s == null ? "-" : s).collect(Collectors.joining("\n")));
		}
		return err;
	}

	public static Map<String, JsonNode> readBackupFile(final Path file) throws IOException {
		final Map<String, JsonNode> backupJsons = new HashMap<>();
		try(FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + file.toUri()), Map.of()); Stream<Path> pathStream = Files.list(fs.getPath("/"))) {
			final ObjectMapper jsonMapper = new ObjectMapper();
			pathStream.forEach(p -> {
				try {
					if(p.toString().endsWith(".json")) {
						backupJsons.put(p.getFileName().toString(), jsonMapper.readTree(/*Files.readString(p)*/Files.newBufferedReader(p)));
					} else {
						backupJsons.put(p.getFileName().toString() + ".json", jsonMapper.createObjectNode().put("code", Files.readString(p/*, StandardCharsets.UTF_8*/)));
					}
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			});
			return backupJsons;
		} catch(ProviderNotFoundException e) {
			return backupJsons; // will result in a "different device" error
		} catch(RuntimeException e) {
			if(e.getCause() instanceof IOException ioe) {
				throw ioe;
			} 
			throw e;
		}
	}
}