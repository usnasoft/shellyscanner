package it.usna.shellyscan.view.scheduler.walldisplay;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.WallDisplay;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD;
import it.usna.shellyscan.model.device.g2.modules.ThermostatG2;
import it.usna.shellyscan.view.scheduler.CronUtils;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.VerticalFlowLayout;

public class WDThermSchedulerPanel extends JPanel {
	private static final long serialVersionUID = 1L;
//	private final static float DEF_TARGET = 20f;
	private final ProfilesPanel profilesPanel;
	private JPanel rulesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, VerticalFlowLayout.CENTER, 0, 0));
	private final ScheduleManagerThermWD wdSceduleManager;
	private HashMap<Integer, ArrayList<ScheduleData>> rules = new HashMap<>();
	private final JDialog parent;
//	private final WallDisplay device;

	public WDThermSchedulerPanel(JDialog parent, WallDisplay device) {
		setLayout(new BorderLayout());
		this.parent = parent;
//		this.device = device;
		this.wdSceduleManager = (device != null) ? new ScheduleManagerThermWD(device) : null; // device == null -> design
		ThermostatG2 thermostat = new ThermostatG2(device);
		
		profilesPanel = new ProfilesPanel(parent, device);
		profilesPanel.setPreferredSize(new Dimension(getPreferredSize().width, 16*5));
		add(profilesPanel, BorderLayout.NORTH);
		
		profilesPanel.addPropertyChangeListener(ProfilesPanel.SELECTION_EVENT, propertyChangeEvent -> {
//			System.out.println(propertyChangeEvent.getNewValue());
			try {
				Integer profileId = (Integer)propertyChangeEvent.getNewValue();
				ArrayList<ScheduleData> list = rules.get(profileId);
				if(list == null && profileId >= 0) {
					list = new ArrayList<>();
					Iterator<JsonNode> scIt = wdSceduleManager.getRules(profileId).iterator();
					while(scIt.hasNext()) {
						JsonNode node = scIt.next();
						// {"rule_id":"1751118368455","enable":true,"target_C":21,"profile_id":0,"timespec":"* 0 0 * * MON,TUE,WED,THU,FRI,SAT,SUN"
						ScheduleData thisRule = new ScheduleData(node.get("rule_id").intValue(), node.get("target_C").floatValue(), CronUtils.fragStrToNum(node.get("timespec").textValue()));
						list.add(thisRule);
					}
					rules.put(profileId, list);
				}

				rulesPanel.removeAll();
				if(list != null) {
					list.forEach(data -> {
//						rulesPanel.add(new JLabel(data.toString()));
//						rulesPanel.add(addJob());
//						, device.getMinTargetTemp(), device.getMaxTargetTemp()
						addJob(thermostat.getMinTargetTemp(), thermostat.getMaxTargetTemp(), data.timespec, data.target, Integer.MAX_VALUE);
					});
				}
				rulesPanel.revalidate();
				rulesPanel.repaint(); // last one need this ... do not know why
			} catch (IOException e) {
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
	
	private void addJob(float min, float max, String timespec, float temp, int pos) {
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
			addJob(min, max, null, 0, i + 1);
			lineColors();
			rulesPanel.revalidate();
		}));
		addBtn.setContentAreaFilled(false);
		addBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
		
		JButton removeBtn = new JButton(new UsnaAction(null, "schRemove", "/images/erase-9-16.png", e -> {
//			ScheduleData data = null;
//			if(rulesPanel.getComponentCount() > 1) {
//				int i;
//				for(i = 0; rulesPanel.getComponent(i) != linePanel; i++);
//				rulesPanel.remove(i);
//				lineColors();
//				data = originalValues.remove(i);
//			} else if(rulesPanel.getComponentCount() == 1) {
//				job.clean();
//				data = originalValues.get(0);
//				originalValues.set(0, new ScheduleData(-1, job.getJson()));
//			}
//			if(data != null && data.id >= 0) {
//				removedId.add(data.id);
//			}
//			rulesPanel.revalidate();
//			rulesPanel.repaint(); // last one need this ... do not know why
		}));
		removeBtn.setContentAreaFilled(false);
		removeBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		JButton duplicateBtn = new JButton(new UsnaAction(null, "schDuplicate", "/images/duplicate_trasp16.png", e -> {
			int i;
			for(i = 0; rulesPanel.getComponent(i) != linePanel; i++);
			addJob(min, max, job.getTimeSpec(), job.getTarget(), i + 1);
			lineColors();
			rulesPanel.revalidate();
		}));
		duplicateBtn.setContentAreaFilled(false);
		duplicateBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
		
		JButton copyBtn = new JButton(new UsnaAction(this, "schCopy", "/images/copy_trasp16.png", e -> {
			final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(job.getJson().toString());
			cb.setContents(selection, selection);
			try { TimeUnit.MILLISECONDS.sleep(200); } catch (InterruptedException e1) {} // a small time to show busy pointer
		}));
		copyBtn.setContentAreaFilled(false);
		copyBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		JButton pasteBtn = new JButton(new UsnaAction(this, "schPaste", "/images/paste_trasp16.png", e -> {
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
				Msg.errorMsg(this, "schErrorInvalidPaste");
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
		
//		ScheduleData thisScheduleLine = new ScheduleData((node != null) ? node.path("rule_id").asInt(-1) : -1, job.getJson());
//		if(pos >= originalValues.size()) {
//			rulesPanel.add(linePanel);
//			originalValues.add(thisScheduleLine);
//		} else {
//			rulesPanel.add(linePanel, pos);
//			originalValues.add(pos, thisScheduleLine);
//		}
		rulesPanel.add(linePanel);

//		UsnaToggleAction enableAction = new UsnaToggleAction(this, "/images/Standby24.png", "/images/StandbyOn24.png",
//				e -> enableSchedule(linePanel, true), e -> enableSchedule(linePanel, false) );
//		enableAction.setTooltip("lblDisabled", "lblEnabled");
//		
//		enableAction.setSelected(node != null && node.path("enable").booleanValue());
//		enableButton.setAction(enableAction);
	}
	
	public boolean apply() {
//		todo
		return false;
	}
	
	public void refresh() {
		rules.clear();
		profilesPanel.refresh();
	}
	
	public void loadFromBackup() {
		// todo
	}
	
	private void lineColors() {
		Component[] list = rulesPanel.getComponents();
		for(int i = 0; i < list.length; i++) {
			list[i].setBackground((i % 2 == 1) ? Main.TAB_LINE2_COLOR : Main.TAB_LINE1_COLOR);
		}
	}
	
	private record ScheduleData(int ruleId, float target, String timespec) {}
}
