package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.UtilMiscellaneous;

public class DialogDeviceScriptsG2 extends JDialog {
	private static final long serialVersionUID = 1L;
	public final static String FILE_EXTENSION = "js";
//	private final static Border BUTTON_BORDERS = BorderFactory.createEmptyBorder(0, 12, 0, 12);
//	private final ExTooltipTable table;
//
//	private final ArrayList<Script> scripts = new ArrayList<>();
//	private final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("lblScrColName"), LABELS.getString("lblScrColEnabled"), LABELS.getString("lblScrColRunning"));

	public DialogDeviceScriptsG2(final MainView owner, AbstractG2Device device) {
		super(owner, false);
		setTitle(String.format(LABELS.getString("dlgScriptTitle"), UtilMiscellaneous.getExtendedHostName(device)));
		setDefaultCloseOperation(/*DO_NOTHING_ON_CLOSE*/DISPOSE_ON_CLOSE);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton jButtonClose = new JButton(LABELS.getString("dlgClose"));
		jButtonClose.addActionListener(event -> dispose());

		buttonsPanel.add(jButtonClose);
		
		JPanel scriptsPanel = new ScriptsPanel(device);
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		JPanel kvsPanel = new KVSPanel(device);
		
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(LABELS.getString("lblScriptsTab"), scriptsPanel);
		tabs.addTab(LABELS.getString("lblKVSTab"), kvsPanel);
		
		getContentPane().add(tabs, BorderLayout.CENTER);

		setSize(490, 380);
		setLocationRelativeTo(owner);
		setVisible(true);
	}
} // 274 - 360