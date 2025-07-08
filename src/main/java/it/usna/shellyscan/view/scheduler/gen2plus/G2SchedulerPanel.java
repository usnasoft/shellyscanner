package it.usna.shellyscan.view.scheduler.gen2plus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Iterator;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.RestoreAction;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManager;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.swing.VerticalFlowLayout;

public class G2SchedulerPanel extends JScrollPane {
	private static final long serialVersionUID = 1L;

	private final JDialog parent;
	private final ScheduleManager sceduleManager;
	private final MethodHints mHints;
	private final ArrayList<ScheduleData> originalValues = new ArrayList<>();
	private final ArrayList<Integer> removedId = new ArrayList<>();
	private final JPanel schedulesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, VerticalFlowLayout.CENTER, 0, 0));

	public G2SchedulerPanel(JDialog parent, AbstractG2Device device) {
		super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.parent = parent;
		this.sceduleManager = new ScheduleManager(device);
		this.mHints = new MethodHints(device);
		
		init();
		fill();
	}
	
	/** test & design */
	public G2SchedulerPanel(JDialog parent) {
		super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.parent = parent;
		sceduleManager = null;
		mHints = null;
		addJob(null, Integer.MAX_VALUE);
		init();
	}
	
	private void init() {
		setBackground(Main.BG_COLOR);
		getVerticalScrollBar().setUnitIncrement(16);
		setViewportView(schedulesPanel);
		setBorder(BorderFactory.createEmptyBorder());
	}
	
	private void fill() {
		boolean exist = false;
		try {
			Iterator<JsonNode> scIt = sceduleManager.getJobs().iterator();
			while(scIt.hasNext()) {
				addJob(scIt.next(), Integer.MAX_VALUE);
				exist = true;
			}
		} catch (IOException e) {
			Msg.errorMsg(e);
		}
		if(exist == false) {
			addJob(null, Integer.MAX_VALUE);
		}
		lineColors();
	}
	
	public void refresh() {
		schedulesPanel.removeAll();
		originalValues.clear();
		fill();
	}
	
	public void loadFromBackup() {
		final ScannerProperties appProp = ScannerProperties.instance();
		final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(new FileNameExtensionFilter(Main.LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
		if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				Map<String, JsonNode> files = RestoreAction.readBackupFile(fc.getSelectedFile().toPath());
				appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getCanonicalPath());
				JsonNode schNode = files.get("Schedule.List.json").get("jobs");
				Iterator<JsonNode> scIt = schNode.iterator();
				while(scIt.hasNext()) {
					ObjectNode jobNode = (ObjectNode)scIt.next();
					jobNode.remove("id");
					if(jobNode.hasNonNull("timespec") && jobNode.hasNonNull("calls")) {
						if(schedulesPanel.getComponentCount() == 1 && ((G2JobPanel)((JPanel)schedulesPanel.getComponent(0)).getComponent(0)).isNullJob()) {
							schedulesPanel.remove(0);
						}
						addJob(jobNode, Integer.MAX_VALUE);
					} else {
						Msg.errorMsg(this, "msgIncompatibleFile");
					}
				}
				lineColors();
			} catch (FileNotFoundException | NoSuchFileException e) {
				Msg.errorMsg(this, String.format(Main.LABELS.getString("msgFileNotFound"), fc.getSelectedFile().getName()));
			} catch (/*IO*/Exception e) {
				Msg.errorMsg(this, "msgIncompatibleFile");
			} finally {
				parent.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	public boolean apply() {
		try {
			parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			int numJobs = schedulesPanel.getComponentCount();

			// Validation
			if(numJobs == 1) {
				G2JobPanel sl = (G2JobPanel)((JPanel)schedulesPanel.getComponent(0)).getComponent(0);
				if(sl.isNullJob() == false && sl.validateData() == false) {
					return false;
				}
			} else {
				for(int i = 0; i < numJobs; i++) {
					G2JobPanel sl = (G2JobPanel)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
					if(sl.validateData() == false) {
						sl.scrollRectToVisible(sl.getBounds());
						return false;
					}
				}
			}
			
			// Delete
			for(int id: removedId) {
				String res = sceduleManager.delete(id);
				try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
				if(res != null) {
					Msg.errorMsg(this, res);
					return false;
				}
			}
			removedId.clear();
			
			// Create / Update
			for(int i = 0; i < numJobs; i++) {
				G2JobPanel sl = (G2JobPanel)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
				ScheduleData original = originalValues.get(i);
				if(/*i > 0 ||*/ sl.isNullJob() == false) {
					ObjectNode jobJson = sl.getJson();
					String res = null;
					if(original.id < 0) { // new -> create
						JButton enableBtn = (JButton)((JPanel)schedulesPanel.getComponent(i)).getComponent(1);
						int newId = sceduleManager.create(jobJson, ((UsnaToggleAction)enableBtn.getAction()).isSelected());
						originalValues.set(i, new ScheduleData(newId, jobJson));
						if(newId < 0) {
							res = "creation error";
						}
						try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
					} else if(sl.hasSystemCalls() && jobJson.get("timespec").equals(original.orig.get("timespec")) == false) {
						jobJson.remove("calls");
						res = sceduleManager.update(original.id, jobJson);
						originalValues.set(i, new ScheduleData(original.id, jobJson));
						try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
					} else if(sl.hasSystemCalls() == false &&
							(jobJson.get("timespec").equals(original.orig.get("timespec")) == false || jobJson.get("calls").equals(original.orig.get("calls")) == false)) { // id >= 0 -> existed
						res = sceduleManager.update(original.id, jobJson);
						originalValues.set(i, new ScheduleData(original.id, jobJson));
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
		} catch (IOException e) {
			Msg.errorMsg(this, e);
			return false;
		} finally {
			parent.setCursor(Cursor.getDefaultCursor());
		}
	}

	private void addJob(JsonNode node, int pos) {
		JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		G2JobPanel job = new G2JobPanel(parent, node, mHints);
		linePanel.add(job);

		JButton enableButton = new JButton();
		enableButton.setContentAreaFilled(false);
		enableButton.setBorder(BorderFactory.createEmptyBorder());
		linePanel.add(enableButton);

		JButton addBtn = new JButton(new UsnaAction(null, "schAdd", "/images/plus_transp16.png", e -> {
			int i;
			for(i = 0; schedulesPanel.getComponent(i) != linePanel; i++);
			addJob(null, i + 1);
			lineColors();
			schedulesPanel.revalidate();
		}));
		addBtn.setContentAreaFilled(false);
		addBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
		
		JButton removeBtn = new JButton(new UsnaAction(null, "schRemove", "/images/erase-9-16.png", e -> {
			ScheduleData data = null;
			if(schedulesPanel.getComponentCount() > 1) {
				int i;
				for(i = 0; schedulesPanel.getComponent(i) != linePanel; i++);
				schedulesPanel.remove(i);
				lineColors();
				data = originalValues.remove(i);
			} else if(schedulesPanel.getComponentCount() == 1) {
				job.clean();
				data = originalValues.get(0);
				originalValues.set(0, new ScheduleData(-1, job.getJson()));
			}
			if(data != null && data.id >= 0) {
				removedId.add(data.id);
			}
			schedulesPanel.revalidate();
			schedulesPanel.repaint(); // last one need this ... do not know why
		}));
		removeBtn.setContentAreaFilled(false);
		removeBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		JButton duplicateBtn = new JButton(new UsnaAction(null, "schDuplicate", "/images/duplicate_trasp16.png", e -> {
			int i;
			for(i = 0; schedulesPanel.getComponent(i) != linePanel; i++);
			addJob(job.getJson(), i + 1);
			lineColors();
			schedulesPanel.revalidate();
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
				if(pastedNode.hasNonNull("calls")) {
					job.setCalls(pastedNode.get("calls"));
				}
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
		if(job.hasSystemCalls() == false) {
			opPanel.add(duplicateBtn);
		}
		opPanel.add(removeBtn);
		opPanel.add(copyBtn);
		if(job.hasSystemCalls() == false) {
			opPanel.add(pasteBtn);
		}
		linePanel.add(opPanel, BorderLayout.EAST);
		
		ScheduleData thisScheduleLine = new ScheduleData((node != null) ? node.path("id").asInt(-1) : -1, job.getJson());
		if(pos >= originalValues.size()) {
			schedulesPanel.add(linePanel);
			originalValues.add(thisScheduleLine);
		} else {
			schedulesPanel.add(linePanel, pos);
			originalValues.add(pos, thisScheduleLine);
		}

		UsnaToggleAction enableAction = new UsnaToggleAction(this, "/images/Standby24.png", "/images/StandbyOn24.png",
				e -> enableSchedule(linePanel, true), e -> enableSchedule(linePanel, false) );
		enableAction.setTooltip("lblDisabled", "lblEnabled");
		
		enableAction.setSelected(node != null && node.path("enable").booleanValue());
		enableButton.setAction(enableAction);
	}
	
	private void enableSchedule(JPanel line, boolean enable) {
		int i;
		for(i = 0; schedulesPanel.getComponent(i) != line; i++);
		ScheduleData data = originalValues.get(i);
		if(data.id >= 0) {
			String res = sceduleManager.enable(data.id, enable);
			if(res != null) {
				Msg.errorMsg(this, res);
			}
		}
	}
	
	private void lineColors() {
		Component[] list = schedulesPanel.getComponents();
		for(int i = 0; i < list.length; i++) {
			list[i].setBackground((i % 2 == 1) ? Main.TAB_LINE2_COLOR : Main.TAB_LINE1_COLOR);
		}
	}
	
	private record ScheduleData(int id, JsonNode orig) {}
}

// https://next-api-docs.shelly.cloud/gen2/ComponentsAndServices/Schedule
// https://github.com/mongoose-os-libs/cron
// https://crontab.guru/
// https://regex101.com/
// https://www.freeformatter.com/regex-tester.html

// http://<ip>/rpc/Schedule.DeleteAll
// http://<ip>/rpc/Schedule.Create?timespec="0 0 22 * * FRI"&calls=[{"method":"Shelly.GetDeviceInfo"}]
// http://<ip>/rpc/Schedule.Create?timespec="10/100 * * * * *"&calls=[{"method":"light.toggle?id=0"}]

// notes: 10 not working (do 0); 100 not working (do 60)