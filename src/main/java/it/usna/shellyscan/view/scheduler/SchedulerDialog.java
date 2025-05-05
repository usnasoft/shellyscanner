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
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
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

	private final AbstractG2Device device;
	private ArrayList<Schedule> originalValues = new ArrayList<>();
	private final JPanel schedulesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, VerticalFlowLayout.CENTER, 0, 0));
//	JScrollPane scrollPane = new JScrollPane();

	public SchedulerDialog(Window owner, AbstractG2Device device) {
		super(owner, Main.LABELS.getString("schTitle") /*+ " - " + UtilMiscellaneous.getExtendedHostName(device)*/, Dialog.ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.device = device;

		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
//		scrollPane.getViewport().setBackground(Color.red);
		schedulesPanel.setBackground(Main.BG_COLOR);
		
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		final ObjectMapper jsonMapper = new ObjectMapper();
		try {
			JsonNode node = jsonMapper.readTree("		{\r\n"
					+ "		    \"id\" : 3,\r\n"
					+ "		    \"enable\" : true,\r\n"
					+ "		    \"timespec\" : \"0 * 1 * 2,3,5-7,dec *\",\r\n"
					+ "		    \"calls\" : [ {\r\n"
					+ "		      \"method\" : \"light.toggle\",\r\n"
					+ "		      \"params\" : {\r\n"
					+ "		        \"id\" : 0\r\n"
					+ "		      }\r\n"
					+ "		    } , {\"method\":\"xxx\"}]\r\n"
					+ "		  }");
			addLine(node, 0);
			lineColors();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		scrollPane.setViewportView(schedulesPanel);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton okButton = new JButton("Ok");
		okButton.addActionListener(e -> {
			for(int i = 0; i < schedulesPanel.getComponentCount(); i++) {
				ScheduleLine sl = (ScheduleLine)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
				boolean valid = sl.validateData(); 
				System.out.println(valid);
				if(valid == false) {
					return;
				}
			}
			for(int i = 0; i < schedulesPanel.getComponentCount(); i++) {
				ScheduleLine sl = (ScheduleLine)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
				System.out.println(sl.getJson());
				System.out.println(originalValues.get(i).orig);
				System.out.println(originalValues.get(i).id + " -- " + sl.getJson().equals(originalValues.get(i).orig));
			}
		});
		JButton jButtonClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		buttonsPanel.add(okButton);
		buttonsPanel.add(jButtonClose);

		pack();
		setSize(getWidth(), 500);
		setLocationRelativeTo(owner);
	}

	private void addLine(JsonNode node, int pos) {
		JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		ScheduleLine line = (node == null) ? new ScheduleLine() : new ScheduleLine(node);
		linePanel.add(line);
		
		UsnaToggleAction switchAction = new UsnaToggleAction(null, "/images/Standby24.png", "/images/StandbyOn24.png", e -> {
//			try {
//				light.toggle();
//				adjust();
//			} catch (IOException e1) {
//				LOG.error("toggle", e1);
//			}
		});
		JButton switchButton = new JButton(switchAction);
		switchButton.setContentAreaFilled(false);
		switchButton.setBorder(BorderFactory.createEmptyBorder());
		switchAction.setSelected(node != null && node.path("enable").booleanValue());
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
			Component[] list = schedulesPanel.getComponents();
			if(list.length > 1) {
				schedulesPanel.remove(linePanel);
				lineColors();
				schedulesPanel.revalidate();
				schedulesPanel.repaint(); // last one ... do not know why
			}
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
		
		JButton copyBtn = new JButton(new UsnaAction(null, "schCopy", "/images/copy_trasp16.png", e -> {
			final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(line.getJson().toString());
			cb.setContents(selection, selection);
		}));
		copyBtn.setContentAreaFilled(false);
		copyBtn.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		JButton pasteBtn = new JButton(new UsnaAction(null, "schPaste", "/images/paste_trasp16.png", e -> {
			final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			try {
				String sch = cb.getContents(this).getTransferData(DataFlavor.stringFlavor).toString();
				final ObjectMapper jsonMapper = new ObjectMapper();

				JsonNode pastedNode = jsonMapper.readTree(sch);
//				if(pastedNode.hasNonNull("timespec") && pastedNode.hasNonNull("calls")) {
					line.setCron(pastedNode.get("timespec").asText());
					line.setCalls(pastedNode.get("calls"));
					line.revalidate();
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

		schedulesPanel.add(linePanel, pos);
		
		Schedule s = new Schedule(line.getJson(), (node != null) ? node.path("id").asInt(-1) : -1);
		if(pos == originalValues.size()) {
			originalValues.add(s);
		} else {
			originalValues.add(pos, s);
		}
	}
	
	private void lineColors() {
		Component[] list = schedulesPanel.getComponents();
		for(int i = 0; i < list.length; i++) {
			list[i].setBackground((i % 2 == 1) ? Main.TAB_LINE2_COLOR : Main.TAB_LINE1_COLOR);
		}
	}
	
	record Schedule(JsonNode orig, int id) {}

	public static void main(final String ... args) throws Exception {
		UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NIMBUS);
		SchedulerDialog s = new SchedulerDialog(null, null);
		s.setVisible(true);
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

// todo asString @sun...