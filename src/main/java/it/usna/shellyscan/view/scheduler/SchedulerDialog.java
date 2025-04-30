package it.usna.shellyscan.view.scheduler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;

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
import it.usna.swing.VerticalFlowLayout;

public class SchedulerDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	//	private final static String INITIAL_VAL = "0 * 1 * 2,3,5-7,dec *";
	//	private final static String INITIAL_VAL = "@sunset+1h30m 1 dec sun";
	//	private final static String INITIAL_VAL = "@sunrise-1h30m";
	//	private final static String INITIAL_VAL = "@sunrise+1h";
	//	private final static String INITIAL_VAL = "@sunrise";

	private JPanel schedulesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, VerticalFlowLayout.CENTER));
	JScrollPane scrollPane = new JScrollPane();

	public SchedulerDialog(Window owner) {
		super(owner, "sch", Dialog.ModalityType.APPLICATION_MODAL);

//		JScrollPane scrollPane = new JScrollPane();
//		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		final ObjectMapper jsonMapper = new ObjectMapper();
		try {
			JsonNode node = jsonMapper.readTree("		{\r\n"
					+ "		    \"id\" : 3,\r\n"
					+ "		    \"enable\" : false,\r\n"
					+ "		    \"timespec\" : \"0 * 1 * 2,3,5-7,dec *\",\r\n"
					+ "		    \"calls\" : [ {\r\n"
					+ "		      \"method\" : \"light.toggle\",\r\n"
					+ "		      \"params\" : {\r\n"
					+ "		        \"id\" : 0\r\n"
					+ "		      }\r\n"
					+ "		    } , {\"method\":\"xxx\"}]\r\n"
					+ "		  }");
			addLine(node);
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

		JButton btnNewButton = new JButton("Ok");
		btnNewButton.addActionListener(e -> {
			for(int i = 0; i < schedulesPanel.getComponentCount(); i++) {
				ScheduleLine sl = (ScheduleLine)((JPanel)schedulesPanel.getComponent(i)).getComponent(0);
				System.out.println(sl.getJsonCalls());
			}
		});
		buttonsPanel.add(btnNewButton);


		pack();
		setSize(getWidth(), 500);
		//		validate();
		setLocationRelativeTo(owner);
	}

	private void addLine(JsonNode node) {
		JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JButton remove = new JButton(new UsnaAction(null, null, "/images/erase-9-16.png", e -> {
			Component[] list = schedulesPanel.getComponents();
			if(list.length > 1) {
				int i;
				for(i = 0; list[i] != linePanel; i++);
				schedulesPanel.remove(i);
				lineColors();
				schedulesPanel.revalidate();
				schedulesPanel.repaint(); // last one ... do not know why
			}
		}));
		remove.setContentAreaFilled(false);
		remove.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		JButton add = new JButton(new UsnaAction(null, null, "/images/plus_transp16.png", e -> {
			Component[] list = schedulesPanel.getComponents();
			int i;
			for(i = 0; list[i] != linePanel; i++);
			addLine(null);
			lineColors();
			schedulesPanel.revalidate();
		}));
		add.setContentAreaFilled(false);
		add.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
		
		JButton duplicate = new JButton(new UsnaAction(null, null, "/images/documents_duplicate.png", e -> {
			Component[] list = schedulesPanel.getComponents();
			int i;
			for(i = 0; list[i] != linePanel; i++);
			      // todo ((ScheduleLine)((JPanel)list[i]).getComponent(0)).getJsonCalls();
			addLine(null);
			lineColors();
			schedulesPanel.revalidate();
		}));
		duplicate.setContentAreaFilled(false);
		duplicate.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

		ScheduleLine line = (node == null) ? new ScheduleLine() : new ScheduleLine(node);
		linePanel.add(line, BorderLayout.CENTER);
		
		JPanel opPanel = new JPanel(new VerticalFlowLayout());
		opPanel.setOpaque(false);
		opPanel.add(add);
		opPanel.add(remove);
		opPanel.add(duplicate);
		linePanel.add(opPanel, BorderLayout.EAST);

		schedulesPanel.add(linePanel);
		linePanel.setBackground(it.usna.shellyscan.Main.TAB_LINE2_COLOR);
	}
	
	private void lineColors() {
		Component[] list = schedulesPanel.getComponents();
		for(int i = 0; i < list.length; i++) {
			list[i].setBackground((i % 2 == 1) ? Main.TAB_LINE2_COLOR : Main.TAB_LINE1_COLOR);
		}
	}

	public static void main(final String ... args) throws JsonMappingException, JsonProcessingException {
		SchedulerDialog s = new SchedulerDialog(null);
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