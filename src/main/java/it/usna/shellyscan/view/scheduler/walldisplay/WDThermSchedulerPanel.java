package it.usna.shellyscan.view.scheduler.walldisplay;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.RestoreAction;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.WallDisplay;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD.Rule;
import it.usna.shellyscan.model.device.g2.modules.ThermostatG2;
import it.usna.shellyscan.view.scheduler.CronUtils;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.swing.VerticalFlowLayout;

public class WDThermSchedulerPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final ProfilesPanel profilesPanel;
	private JPanel rulesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, VerticalFlowLayout.CENTER, 0, 0));
	private final ScheduleManagerThermWD wdSceduleManager;
	private HashMap<Integer, List<Rule>> rules = new HashMap<>();
	private ArrayList<RemovedRule> removed = new ArrayList<>();
	private int currentProfileId = -1;
	private final JDialog parent;
//	private final WallDisplay device;

	public WDThermSchedulerPanel(JDialog parent, WallDisplay device) {
		setLayout(new BorderLayout());
		this.parent = parent;
//		this.device = device;
		this.wdSceduleManager = (device != null) ? new ScheduleManagerThermWD(device) : null; // device == null -> design
		ThermostatG2 thermostat = new ThermostatG2(device);
		
		profilesPanel = new ProfilesPanel(parent, device, wdSceduleManager);
		profilesPanel.setPreferredSize(new Dimension(getPreferredSize().width, 16 * 5));
		add(profilesPanel, BorderLayout.NORTH);
		
		profilesPanel.addPropertyChangeListener(ProfilesPanel.SELECTION_EVENT, propertyChangeEvent -> {
			try {
				currentProfileId = (Integer)propertyChangeEvent.getNewValue();
				List<Rule> rulesList = rules.get(currentProfileId);
				if(rulesList == null && currentProfileId >= 0) {
					rulesList = wdSceduleManager.getRules(currentProfileId);
					rulesList.forEach(rule -> rule.setTimespec(CronUtils.fragStrToNum(rule.getTimespec())));
					rules.put(currentProfileId, rulesList);
				}

				rulesPanel.removeAll();
				if(rulesList != null && rulesList.size() > 0) { // there is a selected profile with rules
					rulesList.forEach(data -> {
						addJob(thermostat.getMinTargetTemp(), thermostat.getMaxTargetTemp(), data.isEnabled() , data.getTimespec(), data.getTarget(), Integer.MAX_VALUE);
					});
				} else if(currentProfileId >= 0) { // there is a selected profile with NO rules -> add an empty rule
					rulesList = new ArrayList<Rule>();
					rulesList.add(new Rule(null, null, null, false));
					rules.put(currentProfileId, rulesList);
					addJob(thermostat.getMinTargetTemp(), thermostat.getMaxTargetTemp(), false, null, null, Integer.MAX_VALUE);
				}
				lineColors();
				rulesPanel.revalidate();
				rulesPanel.repaint(); // last one need this ... do not know why
			} catch (IOException e) {
				Msg.errorMsg(parent, e);
			}
		});
		
		profilesPanel.addPropertyChangeListener(ProfilesPanel.DELETE_EVENT, propertyChangeEvent -> {
			int deletedId = (Integer)propertyChangeEvent.getOldValue();
			rules.put(deletedId, null);
		});
		
		profilesPanel.addPropertyChangeListener(ProfilesPanel.DUPLICATE_EVENT, propertyChangeEvent -> {
			int newId = (Integer)propertyChangeEvent.getNewValue();
			try {
				for(Rule r: rules.get((Integer)propertyChangeEvent.getOldValue())) {
					wdSceduleManager.create(r, newId);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			} catch (IOException | InterruptedException e) {
				Msg.errorMsg(parent, e);
			}
		});

		rulesPanel.setBackground(Main.BG_COLOR);
		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setViewportView(rulesPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		add(scrollPane, BorderLayout.CENTER);
	}
	
	private void addJob(float min, float max, boolean enabled, String timespec, Float temp, int pos) {
		JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		ThermJobPanel job = new ThermJobPanel(parent, min, max, timespec, temp);
		linePanel.add(job);

		JButton enableButton = new JButton();
		enableButton.setContentAreaFilled(false);
		enableButton.setBorder(BorderFactory.createEmptyBorder());
		linePanel.add(enableButton);

		JButton addBtn = new JButton(new UsnaAction(null, "schAdd", "/images/plus_transp16.png", e -> {
			int i;
			for(i = 0; rulesPanel.getComponent(i) != linePanel; i++);
			addJob(min, max, false, null, null, i + 1);
			addRule(false, null, null, i + 1);
			lineColors();
			rulesPanel.revalidate();
		}));
		addBtn.setContentAreaFilled(false);
		addBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
		
		JButton removeBtn = new JButton(new UsnaAction(null, "schRemove", "/images/erase-9-16.png", e -> {
			Rule data = null;
			if(rulesPanel.getComponentCount() > 1) {
				int i;
				for(i = 0; rulesPanel.getComponent(i) != linePanel; i++);
				rulesPanel.remove(i);
				lineColors();
				data = rules.get(currentProfileId).remove(i);
			} else if(rulesPanel.getComponentCount() == 1) {
				job.clean();
				data = rules.get(currentProfileId).remove(0);
				rules.get(currentProfileId).add(new Rule(null, null, null, false));
			}
			if(data.getId() != null) {
				removed.add(new RemovedRule(data.getId(), currentProfileId));
			}
			rulesPanel.revalidate();
			rulesPanel.repaint(); // last one need this ... do not know why
		}));
		removeBtn.setContentAreaFilled(false);
		removeBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		JButton duplicateBtn = new JButton(new UsnaAction(null, "schDuplicate", "/images/duplicate_trasp16.png", e -> {
			int i;
			for(i = 0; rulesPanel.getComponent(i) != linePanel; i++);
			addJob(min, max, false, job.getTimespec(), job.getTarget(), i + 1);
			addRule(false, job.getTimespec(), job.getTarget(), i + 1);
			lineColors();
			rulesPanel.revalidate();
		}));
		duplicateBtn.setContentAreaFilled(false);
		duplicateBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
		
		JButton copyBtn = new JButton(new UsnaAction(parent, "schCopy", "/images/copy_trasp16.png", e -> {
			final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(job.getJson().toString());
			cb.setContents(selection, selection);
			try { TimeUnit.MILLISECONDS.sleep(200); } catch (InterruptedException e1) {} // a small time to show busy pointer
		}));
		copyBtn.setContentAreaFilled(false);
		copyBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		JButton pasteBtn = new JButton(new UsnaAction(parent, "schPaste", "/images/paste_trasp16.png", e -> {
			final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			try {
				String sch = cb.getContents(this).getTransferData(DataFlavor.stringFlavor).toString();
				final ObjectMapper jsonMapper = new ObjectMapper();

				JsonNode pastedNode = jsonMapper.readTree(sch);
				job.setCron(pastedNode.get("timespec").asText());
				job.setTarget(pastedNode.get("target_C").floatValue());
				job.revalidate();
				try { TimeUnit.MILLISECONDS.sleep(200); } catch (InterruptedException e1) {} // a small time to show busy pointer
			} catch (Exception e1) {
				Msg.errorMsg(parent, "schErrorInvalidPaste");
			}
		}));
		pasteBtn.setContentAreaFilled(false);
		pasteBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
		
		JPanel opPanel = new JPanel(new VerticalFlowLayout());
		opPanel.setOpaque(false);
		opPanel.add(addBtn);
		opPanel.add(duplicateBtn);
		opPanel.add(removeBtn);
		opPanel.add(copyBtn);
		opPanel.add(pasteBtn);
		linePanel.add(opPanel, BorderLayout.EAST);
		
		if(pos >= rules.get(currentProfileId).size()) {
			rulesPanel.add(linePanel);
		} else {
			rulesPanel.add(linePanel, pos);
		}
		
		UsnaToggleAction enableAction = new UsnaToggleAction(this, "/images/Standby24.png", "/images/StandbyOn24.png",
				e -> enableSchedule(linePanel, true), e -> enableSchedule(linePanel, false) );
		enableAction.setTooltip("lblDisabled", "lblEnabled");
		
		enableAction.setSelected(enabled);
		enableButton.setAction(enableAction);
	}
	
	private void addRule(boolean enabled, String timespec, Float temp, int pos) {
		if(pos >= rules.get(currentProfileId).size()) {
			rules.get(currentProfileId).add(new Rule(null, temp, timespec, enabled));
		} else {
			rules.get(currentProfileId).add(pos, new Rule(null, temp, timespec, enabled));
		}
	}
	
	private void enableSchedule(JPanel rulePanel, boolean enable) {
		int i;
		for(i = 0; rulesPanel.getComponent(i) != rulePanel; i++);
		Rule rule = rules.get(currentProfileId).get(i);
		if(rule.getId() != null) {
			String res = wdSceduleManager.enable(rule.getId(), currentProfileId, enable);
			if(res != null) {
				Msg.errorMsg(parent, res);
			} else {
				rule.setEnabled(enable);
			}
		}
	}
	
	public boolean apply() {
		try {
			parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			int numJobs = rulesPanel.getComponentCount();

			// Validation
			if(numJobs == 1) {
				ThermJobPanel sl = (ThermJobPanel)((JPanel)rulesPanel.getComponent(0)).getComponent(0);
				if(sl.isNullJob() == false && sl.validateData() == false) {
					return false;
				}
			} else {
				for(int i = 0; i < numJobs; i++) {
					ThermJobPanel sl = (ThermJobPanel)((JPanel)rulesPanel.getComponent(i)).getComponent(0);
					if(sl.validateData() == false) {
						sl.scrollRectToVisible(sl.getBounds());
						return false;
					}
				}
			}

			// Delete
			for(RemovedRule rule: removed) {
				String res = wdSceduleManager.delete(rule.ruleId, rule.profileId());
				try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
				if(res != null) {
					Msg.errorMsg(this, res);
					return false;
				}
			}
			removed.clear();

			// Create / Update
			for(int i = 0; i < numJobs; i++) {
				ThermJobPanel jobPanel = (ThermJobPanel)((JPanel)rulesPanel.getComponent(i)).getComponent(0);
				Rule thisRule = rules.get(currentProfileId).get(i);
				String res = null;
				if(/*i > 0 ||*/ jobPanel.isNullJob() == false) {
					if(thisRule.getId() == null) { // new -> create
						JButton enableBtn = (JButton)((JPanel)rulesPanel.getComponent(i)).getComponent(1);
						thisRule.setTarget(jobPanel.getTarget());
						thisRule.setTimespec(jobPanel.getExtTimespec());
						thisRule.setEnabled(((UsnaToggleAction)enableBtn.getAction()).isSelected());
						String newId = wdSceduleManager.create(thisRule, currentProfileId);
						if(newId == null) {
							res = "creation error";
						} else {
							thisRule.setId(newId);
						}
						try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
					} else if(thisRule.getTarget().equals(jobPanel.getTarget()) == false || thisRule.getTimespec().equals(jobPanel.getTimespec()) == false) { // update
						thisRule.setTarget(jobPanel.getTarget());
						thisRule.setTimespec(jobPanel.getExtTimespec());
						res = wdSceduleManager.update(thisRule, currentProfileId);
						try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
					}
					if(res != null) {
						Msg.errorMsg(this, res);
						return false;
					}
				}
			}
			try { TimeUnit.MILLISECONDS.sleep(100); } catch (InterruptedException e1) {} // a small time to show busy pointer
			return true;
		} catch (/*IO*/Exception e) {
			Msg.errorMsg(this, e);
			return false;
		} finally {
			parent.setCursor(Cursor.getDefaultCursor());
		}
	}
	
	public void refresh() {
		rules.clear();
		profilesPanel.refresh();
	}
	
	public void loadFromBackup() {
		try {
			if(wdSceduleManager.getProfiles().size() > 0) {
				Msg.errorMsg(this, "msgExistingProfile");
				return;
			}
			final ScannerProperties appProp = ScannerProperties.instance();
			final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
			fc.setAcceptAllFileFilterUsed(false);
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
			if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getCanonicalPath());
				
				Map<String, JsonNode> files = RestoreAction.readBackupFile(fc.getSelectedFile().toPath());
				JsonNode profilesNode = files.get("Thermostat.Schedule.ListProfiles.json").path("profiles");
				
//				try(FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + fc.getSelectedFile().toPath().toUri()), Map.of())) {
//					
//					
//					final ObjectMapper jsonMapper = new ObjectMapper();
//					JsonNode profilesNode = jsonMapper.readTree(Files.newBufferedReader(fs.getPath("Thermostat.Schedule.ListProfiles.json"))).get("profiles");
//					//				Iterator<JsonNode> scIt = scNode.iterator();
//					//				while(scIt.hasNext()) {
//					//					ObjectNode jobNode = (ObjectNode)scIt.next();
//					//					jobNode.remove("id");
//					//					if(jobNode.hasNonNull("timespec") && (jobNode.hasNonNull("target_C") || jobNode.hasNonNull("pos"))) {
//					//						if(schedulesPanel.getComponentCount() == 1 && ((TRVJobPanel)((JPanel)schedulesPanel.getComponent(0)).getComponent(0)).isNullJob()) {
//					//							schedulesPanel.remove(0);
//					//						}
//					//						addJob(jobNode, device.getMinTargetTemp(), device.getMaxTargetTemp(), Integer.MAX_VALUE);
//					//					} else {
//					//						Msg.errorMsg(this, "msgIncompatibleFile");
//					//					}
//					//				}
//					lineColors();
//				} catch (FileNotFoundException | NoSuchFileException e) {
//					Msg.errorMsg(this, String.format(LABELS.getString("msgFileNotFound"), fc.getSelectedFile().getName()));
//				} catch (/*IO*/Exception e) {
//					Msg.errorMsg(this, "msgIncompatibleFile");
//				} finally {
//					setCursor(Cursor.getDefaultCursor());
//				}
			}
		} catch (IOException e) {
			Msg.errorMsg(this, e);
		}
	}
	
	private void lineColors() {
		Component[] list = rulesPanel.getComponents();
		for(int i = 0; i < list.length; i++) {
			list[i].setBackground((i % 2 == 1) ? Main.TAB_LINE2_COLOR : Main.TAB_LINE1_COLOR);
		}
	}

	private record RemovedRule(String ruleId, int profileId) {}
}