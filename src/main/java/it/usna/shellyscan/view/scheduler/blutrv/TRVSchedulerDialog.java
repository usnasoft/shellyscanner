package it.usna.shellyscan.view.scheduler.blutrv;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
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
import it.usna.shellyscan.model.device.blu.BluTRV;
import it.usna.shellyscan.model.device.blu.modules.ScheduleManagerTRV;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.UsnaSwingUtils;
import it.usna.swing.VerticalFlowLayout;

public class TRVSchedulerDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private final ScheduleManagerTRV sceduleManager;
	private final BluTRV device;
	private final ArrayList<ScheduleData> originalValues = new ArrayList<>();
	private final ArrayList<Integer> removedId = new ArrayList<>();
	private final JPanel schedulesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, VerticalFlowLayout.CENTER, 0, 0));

	public TRVSchedulerDialog(Window owner, BluTRV device) {
		super(owner, Main.LABELS.getString("schTitle") + " - " + UtilMiscellaneous.getExtendedHostName(device), Dialog.ModalityType.MODELESS);
		this.device = device;
		this.sceduleManager = new ScheduleManagerTRV(device);
		fill();
		init();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	/** test & design */
	public TRVSchedulerDialog() {
		super(null, "schTitle", Dialog.ModalityType.APPLICATION_MODAL);
		this.device = null;
		sceduleManager = null;
		addJob(null, 10f, 30f, Integer.MAX_VALUE);
		init();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void fill() {
		boolean exist = false;
		try {
			Iterator<JsonNode> scIt = sceduleManager.getRules().iterator();
			while(scIt.hasNext()) {
				addJob(scIt.next(), device.getMinTargetTemp(), device.getMaxTargetTemp(), Integer.MAX_VALUE);
				exist = true;
			}
		} catch (IOException e) {
			Msg.errorMsg(e);
		}
		if(exist == false) {
			addJob(null, device.getMinTargetTemp(), device.getMaxTargetTemp(), Integer.MAX_VALUE);
		}
		lineColors();
	}
	
	public void refresh() {
		schedulesPanel.removeAll();
		originalValues.clear();
		fill();
	}
	
	private void init() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		schedulesPanel.setBackground(Main.BG_COLOR);
		
		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setViewportView(schedulesPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		buttonsPanel.add(new JButton(new UsnaAction("dlgApply", e -> apply()) ));
		buttonsPanel.add( new JButton(new UsnaAction("dlgApplyClose", e -> {
			if(apply()) dispose();
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction(this, "labelRefresh", e -> refresh()) ));
		buttonsPanel.add(new JButton(new UsnaAction("lblLoadFile", e -> loadFromBackup()) ));
		buttonsPanel.add(new JButton(new UsnaAction("dlgClose", e -> dispose()) ));

		pack();
		setSize(getWidth(), 500);
	}
	
	private void loadFromBackup() {
		final ScannerProperties appProp = ScannerProperties.instance();
		final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
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
					if(jobNode.hasNonNull("timespec") && (jobNode.hasNonNull("target_C") || jobNode.hasNonNull("pos"))) {
						if(schedulesPanel.getComponentCount() == 1 && ((TRVJobPanel)((JPanel)schedulesPanel.getComponent(0)).getComponent(0)).isNullJob()) {
							schedulesPanel.remove(0);
						}
						addJob(jobNode, device.getMinTargetTemp(), device.getMaxTargetTemp(), Integer.MAX_VALUE);
					} else {
						Msg.errorMsg(this, "msgIncompatibleFile");
						return;
					}
				}
			} catch (FileNotFoundException | NoSuchFileException e) {
				Msg.errorMsg(this, String.format(LABELS.getString("msgFileNotFound"), fc.getSelectedFile().getName()));
			} catch (/*IO*/Exception e) {
				Msg.errorMsg(this, "msgIncompatibleFile");
			} finally {
				lineColors();
				setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	private boolean apply() {
		try {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			int numJobs = schedulesPanel.getComponentCount();

			// Validation
			if(numJobs == 1) { // only 1 can be null and must be alone -> (existing jobs deleted?)
				TRVJobPanel sl = (TRVJobPanel)((JPanel)schedulesPanel.getComponent(0)).getComponent(0);
				if(sl.isNullJob() == false && sl.validateData() == false) {
					return false;
				}
			} else {
				for(int i = 0; i < numJobs; i++) {
					TRVJobPanel sl = (TRVJobPanel)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
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
				TRVJobPanel sl = (TRVJobPanel)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
				ScheduleData original = originalValues.get(i);
				if(sl.isNullJob() == false) { // only first one (and single) can be a "null job" (see validation phase)
					ObjectNode jobJson = sl.getJson();
					String res = null;
					if(original.id < 0) {
						JButton enableBtn = (JButton)((JPanel)schedulesPanel.getComponent(i)).getComponent(1);
						int newId = sceduleManager.create(jobJson, ((UsnaToggleAction)enableBtn.getAction()).isSelected());
						originalValues.set(i, new ScheduleData(newId, jobJson));
						if(newId < 0) {
							res = "creation error";
						}
						try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
					} else if(jobJson.get("timespec").equals(original.orig.get("timespec")) == false || jobJson.path("pos").equals(original.orig.path("pos")) == false || jobJson.path("target_C").equals(original.orig.path("target_C")) == false) { // id >= 0 -> existed
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
			this.setCursor(Cursor.getDefaultCursor());
		}
	}

	private void addJob(JsonNode node, float min, float max, int pos) {
		JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		TRVJobPanel job = new TRVJobPanel(this, min, max, node);
		linePanel.add(job);

		JButton enableButton = new JButton();
		enableButton.setContentAreaFilled(false);
		enableButton.setBorder(BorderFactory.createEmptyBorder());
		linePanel.add(enableButton);

		JButton addBtn = new JButton(new UsnaAction(null, "schAdd", "/images/plus_transp16.png", e -> {
			int i;
			for(i = 0; schedulesPanel.getComponent(i) != linePanel; i++);
			addJob(null, min, max, i + 1);
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
			addJob(job.getJson(), min, max, i + 1);
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
				job.setTarget(pastedNode);
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
		linePanel.add(opPanel);
		
		ScheduleData thisScheduleLine = new ScheduleData((node != null) ? node.path("rule_id").asInt(-1) : -1, job.getJson());
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

	public static void main(final String ... args) throws Exception {
		UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NIMBUS);
		new TRVSchedulerDialog();
	}
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