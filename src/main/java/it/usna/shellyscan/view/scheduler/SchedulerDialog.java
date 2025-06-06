package it.usna.shellyscan.view.scheduler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManager;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.UsnaSwingUtils;
import it.usna.swing.VerticalFlowLayout;

public class SchedulerDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	//	private final static String INITIAL_VAL = "0 * 1 * 2,3,5-7,dec *";
	//	private final static String INITIAL_VAL = "@sunset+1h30m 1 dec sun";
	//	private final static String INITIAL_VAL = "@sunrise-1h30m";
	//	private final static String INITIAL_VAL = "@sunrise+1h";
	//	private final static String INITIAL_VAL = "@sunrise";

	private final ScheduleManager sceduleManager;
	private final MethodHints mHints;
	private final ArrayList<ScheduleData> originalValues = new ArrayList<>();
	private final ArrayList<Integer> removedId = new ArrayList<>();
	private final JPanel schedulesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, VerticalFlowLayout.CENTER, 0, 0));

	public SchedulerDialog(Window owner, AbstractG2Device device) {
		super(owner, Main.LABELS.getString("schTitle") + " - " + UtilMiscellaneous.getExtendedHostName(device), Dialog.ModalityType.MODELESS);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.sceduleManager = new ScheduleManager(device);
		this.mHints = new MethodHints(device);

		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setViewportView(schedulesPanel);
		schedulesPanel.setBackground(Main.BG_COLOR);
		
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		boolean exist = false;
		try {
			Iterator<JsonNode> scIt = sceduleManager.getSchedules().iterator();
			while(scIt.hasNext()) {
				addLine(scIt.next(), Integer.MAX_VALUE);
				exist = true;
			}
		} catch (IOException e) {
			Msg.errorMsg(e);
		}
		if(exist == false) {
			addLine(null, Integer.MAX_VALUE);
		}
		lineColors();

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		JButton applyButton = new JButton(new UsnaAction("dlgApply", e -> apply()) );
		JButton applyCloseButton = new JButton(new UsnaAction("dlgApplyClose", e -> {apply(); dispose();}) );
		JButton jButtonClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		buttonsPanel.add(applyButton);
		buttonsPanel.add(applyCloseButton);
		buttonsPanel.add(jButtonClose);

		pack();
		setSize(getWidth(), 500);
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	/** test & design */
	public SchedulerDialog() {
		super(null, "schTitle", Dialog.ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		sceduleManager = null;
		mHints = null;

		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setViewportView(schedulesPanel);
		schedulesPanel.setBackground(Main.BG_COLOR);
		
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		addLine(null, Integer.MAX_VALUE);
		lineColors();

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		JButton applyButton = new JButton(new UsnaAction("dlgApply", e -> apply()) );
		JButton applyCloseButton = new JButton(new UsnaAction("dlgApplyClose", e -> {apply(); dispose();}) );
		JButton jButtonClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		buttonsPanel.add(applyButton);
		buttonsPanel.add(applyCloseButton);
		buttonsPanel.add(jButtonClose);

		pack();
		setSize(getWidth(), 500);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void apply() {
		for(int i = 0; i < schedulesPanel.getComponentCount(); i++) {
			ScheduleLine sl = (ScheduleLine)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
			boolean valid = sl.validateData(); 
			System.out.println(valid);
			if(valid == false) {
				sl.scrollRectToVisible(sl.getBounds());
				return;
			}
		}
		try {
			for(int i = 0; i < schedulesPanel.getComponentCount(); i++) {
				ScheduleLine sl = (ScheduleLine)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
				ScheduleData original = originalValues.get(i);
				System.out.println(sl.getJson());
				System.out.println(original.orig);
				System.out.println(original.id + " -- " + sl.getJson().equals(original.orig));

				if(original.id < 0) {
					JButton enableBtn = (JButton)((JPanel)schedulesPanel.getComponent(i)).getComponent(1);
					int id = sceduleManager.create((ObjectNode)sl.getJson(), ((UsnaToggleAction)enableBtn.getAction()).isSelected());
					originalValues.set(i, new ScheduleData(sl.getJson(), id));
				} else if(sl.getJson().equals(original.orig) == false) { // id >= 0 -> existed
					String res = sceduleManager.update(original.id, sl.getJson());
					originalValues.set(i, new ScheduleData(sl.getJson(), original.id));
					if(res != null) {
						Msg.errorMsg(this, res);
						return;
					}
				}
			}
			for(int id: removedId) {
				String res = sceduleManager.delete(id);
				if(res != null) {
					Msg.errorMsg(this, res);
					break;
				}
			}
			removedId.clear();
		} catch (IOException e) {
			Msg.errorMsg(this, e);
		}
	}

	private void addLine(JsonNode node, int pos) {
		JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		ScheduleLine line = new ScheduleLine(this, node, mHints);
		linePanel.add(line);

		JButton switchButton = new JButton();
		switchButton.setContentAreaFilled(false);
		switchButton.setBorder(BorderFactory.createEmptyBorder());
		linePanel.add(switchButton);

		JButton addBtn = new JButton(new UsnaAction(null, "schAdd", "/images/plus_transp16.png", e -> {
			int i;
			for(i = 0; schedulesPanel.getComponent(i) != linePanel; i++);
			addLine(null, i + 1);
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
				line.clean();
				data = originalValues.get(0);
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
			addLine(line.getJson(), i + 1);
			lineColors();
			schedulesPanel.revalidate();
		}));
		duplicateBtn.setContentAreaFilled(false);
		duplicateBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
		
		JButton copyBtn = new JButton(new UsnaAction(this, "schCopy", "/images/copy_trasp16.png", e -> {
			final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(line.getJson().toString());
			cb.setContents(selection, selection);
			try { TimeUnit.MILLISECONDS.sleep(300); } catch (InterruptedException e1) {} // a small time to show busy pointer
		}));
		copyBtn.setContentAreaFilled(false);
		copyBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		JButton pasteBtn = new JButton(new UsnaAction(this, "schPaste", "/images/paste_trasp16.png", e -> {
			final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			try {
				String sch = cb.getContents(this).getTransferData(DataFlavor.stringFlavor).toString();
				final ObjectMapper jsonMapper = new ObjectMapper();

				JsonNode pastedNode = jsonMapper.readTree(sch);
//				if(pastedNode.hasNonNull("timespec") && pastedNode.hasNonNull("calls")) {
					line.setCron(pastedNode.get("timespec").asText());
					line.setCalls(pastedNode.get("calls"));
					line.revalidate();
					try { TimeUnit.MILLISECONDS.sleep(300); } catch (InterruptedException e1) {} // a small time to show busy pointer
//				}
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
		
		ScheduleData thisScheduleLine = new ScheduleData(line.getJson(), (node != null) ? node.path("id").asInt(-1) : -1);
		if(pos >= originalValues.size()) {
			schedulesPanel.add(linePanel);
			originalValues.add(thisScheduleLine);
		} else {
			schedulesPanel.add(linePanel, pos);
			originalValues.add(pos, thisScheduleLine);
		}

		UsnaToggleAction switchAction = new UsnaToggleAction(this, "/images/Standby24.png", "/images/StandbyOn24.png",
				e -> enableSchedule(linePanel, true), e -> enableSchedule(linePanel, false) );
		switchAction.setTooltip("lblDisabled", "lblEnabled");
		
		switchAction.setSelected(node != null && node.path("enable").booleanValue());
		switchButton.setAction(switchAction);
	}
	
//	private void removeLine(int lineIndex) {
//		schedulesPanel.remove(lineIndex);
//		lineColors();
//		
//		ScheduleData data = originalValues.remove(lineIndex);
//		if(data.id >= 0) {
//			removedId.add(data.id);
//		}
//		schedulesPanel.revalidate();
//		schedulesPanel.repaint(); // last one need this ... do not know why
//	}
	
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
	
	record ScheduleData(JsonNode orig, int id) {}

	public static void main(final String ... args) throws Exception {
		UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NIMBUS);
		new SchedulerDialog();
	}
}

// https://next-api-docs.shelly.cloud/gen2/ComponentsAndServices/Schedule
// https://github.com/mongoose-os-libs/cron
// https://crontab.guru/
// https://regex101.com/

// http://<ip>/rpc/Schedule.DeleteAll
// http://<ip>/rpc/Schedule.Create?timespec="0 0 22 * * FRI"&calls=[{"method":"Shelly.GetDeviceInfo"}]
// http://<ip>/rpc/Schedule.Create?timespec="10/100 * * * * *"&calls=[{"method":"light.toggle?id=0"}]

// notes: 10 not working (do 0); 100 not working (do 60)