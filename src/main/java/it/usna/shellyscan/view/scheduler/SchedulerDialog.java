package it.usna.shellyscan.view.scheduler;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SchedulerDialog extends JDialog {
	private static final long serialVersionUID = 1L;
//	private final static String INITIAL_VAL = "0 * 1 * 2,3,5-7,dec *";
	private final static String INITIAL_VAL = "@sunset+1h30m 1 dec sun";
//	private final static String INITIAL_VAL = "@sunrise-1h30m";
//	private final static String INITIAL_VAL = "@sunrise+1h";
//	private final static String INITIAL_VAL = "@sunrise";
	
	public SchedulerDialog(Window owner) {
		super(owner, "sch", Dialog.ModalityType.APPLICATION_MODAL);
		JPanel mainPanel = new JPanel(new GridLayout(0, 1));

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		JButton remove = new JButton("del");

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

			ScheduleLine line = new ScheduleLine(node);
			panel.add(line);
			panel.add(remove);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mainPanel.add(panel);

		getContentPane().add(mainPanel, BorderLayout.NORTH);
		setSize(1100, 200);
		setLocationRelativeTo(owner);
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