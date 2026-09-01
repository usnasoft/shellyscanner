package it.usna.shellyscan.view.scheduler.gen2plus;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaDropdownAction;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.scheduler.AbstractCronPanel;
import it.usna.shellyscan.view.scheduler.gen2plus.pareditor.ParamEditorDialog;
import it.usna.shellyscan.view.util.Msg;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class G2JobPanel extends AbstractCronPanel {
	private static final long serialVersionUID = 1L;
	private JPanel callsPanel;
	private JPanel callsParameterPanel;
	private JPanel callsOperationsPanel;
	private boolean systemJob = false;
	private final AbstractG2Device device;
	private final MethodHints mHints;
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	/**
	 * @wbp.parser.constructor
	 */
	public G2JobPanel(JDialog parent, AbstractG2Device device, JsonNode scheduleNode, MethodHints mHints) {
		super(parent);
		initCallSection();
		this.mHints = mHints;
		this.device = device;
		if(scheduleNode == null) {
			setCron(DEF_CRON);
			addCall("", "", 0);
		} else {
			setCron(scheduleNode.path("timespec").asString(""));
			JsonNode calls = scheduleNode.path("calls");
			if(calls.size() > 0) {
				setCalls(/*scheduleNode.path("calls")*/calls);
			} else {
				addCall("", "", 0);
			}
		}
	}

	public void setCalls(JsonNode calls) {
		int iniIdx = callsPanel.getComponentCount();
		if(iniIdx > 0 && calls.size() > 0 && ((JTextField)callsPanel.getComponent(iniIdx - 1)).getText().isEmpty() && ((JTextField)callsParameterPanel.getComponent(iniIdx - 1)).getText().isEmpty()) {
			iniIdx--;
			callsPanel.remove(iniIdx);
			callsParameterPanel.remove(iniIdx);
			callsOperationsPanel.remove(iniIdx);
		}
		Iterator<JsonNode> callsIt = calls.iterator();
		for(int i = iniIdx; callsIt.hasNext(); i++) {
			JsonNode call = callsIt.next();
			String params = call.path("params").toString();
			if(call.hasNonNull("origin")) {
				systemJob = true;
			}
			addCall(call.path("method").asString(""), params.isEmpty() ? "" :  params.substring(1, params.length() - 1), i);
		}
		if(systemJob) {
			enableEdit(callsPanel, false);
			enableEdit(callsParameterPanel, false);
			enableEdit(callsOperationsPanel, false);
		}
	}

	private void addCall(String method, String params/*, String origin*/, int index) {
		JTextField methodTF = new JTextField(method);
		methodTF.setColumns(20); // not all the space needed space (in case of long strings)
		JTextField paramsTF = new JTextField(params);
		paramsTF.setColumns(40); // not all the space needed space (in case of long strings)
		callsPanel.add(methodTF, index);
		callsParameterPanel.add(paramsTF, index);
		
		UsnaAction paramEditAction = new UsnaAction(parentDlg, "edit", e -> new ParamEditorDialog(parentDlg, paramsTF));
		
		paramsTF.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					paramEditAction.actionPerformed(null);
				}
			}
		});

		JPanel callOpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		callOpPanel.setOpaque(false);
		
		JButton addB = new JButton(new UsnaAction(null, "btnMethodAddTooltip", "/images/plus_transp16.png", e -> { //new ImageIcon(getClass().getResource("/images/plus_transp16.png")));
			Component[] list = callsOperationsPanel.getComponents();
			int i;
			for(i = 0; list[i] != callOpPanel; i++);
			addCall("", "", i + 1);
			callsOperationsPanel.revalidate();
		}));
		addB.setContentAreaFilled(false);
		addB.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 3));
		callOpPanel.add(addB);
		
		JButton minusB = new JButton(new UsnaAction(null, "btnMethodRemoveTooltip", "/images/erase-9-16.png", e -> {
			Component[] list = callsOperationsPanel.getComponents();
			if(list.length > 1) {
				int i;
				for(i = 0; list[i] != callOpPanel; i++);
				callsPanel.remove(i);
				callsParameterPanel.remove(i);
				callsOperationsPanel.remove(i);
				callsOperationsPanel.revalidate();
			}
		}));
		minusB.setContentAreaFilled(false);
		minusB.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		callOpPanel.add(minusB);

		JButton btnSelectCombo = new JButton();
		btnSelectCombo.setAction(new UsnaDropdownAction(btnSelectCombo, "lblMethodSelect", "/images/expand-more.png", () -> {
			try {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				Object[] menu = mHints.get(methodTF, paramsTF);
				if(ParamEditorDialog.canEdit(paramsTF.getText())) {
					ArrayList<Object> m = new ArrayList<>();
					m.add(paramEditAction);
					m.add(null);
					m.addAll(List.of(menu));
					menu = m.toArray(Object[]::new);
				}
				return menu;
			} finally {
				this.setCursor(Cursor.getDefaultCursor());
			}
		}));
		btnSelectCombo.setContentAreaFilled(false);
		btnSelectCombo.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		callOpPanel.add(btnSelectCombo);
		
		JButton testButton = new JButton(new UsnaAction(parentDlg, "btnMethodTestTooltip", "/images/Play16.png", e -> {
			if(!methodTF.getText().isBlank()) {
				String res = device.postCommand(methodTF.getText(), "{" + paramsTF.getText() + "}");
				if(res != null) {
					Msg.errorMsg(parentDlg, res);
				}
			}
		}));
		testButton.setContentAreaFilled(false);
		testButton.setBorder(BorderFactory.createEmptyBorder(3, 2, 4, 0));
		callOpPanel.add(testButton);

		callsOperationsPanel.add(callOpPanel, index);
	}

	private void initCallSection() {
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 3;
		add(new JLabel(LABELS.getString("lblMethod")), gbc_lblNewLabel_5);

		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.gridwidth = 2;
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 4;
		gbc_lblNewLabel_6.gridy = 3;
		add(new JLabel(LABELS.getString("lblParameters")), gbc_lblNewLabel_6);
		
		callsPanel = new JPanel();
		GridBagConstraints gbc_callsPanel = new GridBagConstraints();
		gbc_callsPanel.gridwidth = 4;
		gbc_callsPanel.insets = new Insets(0, 0, 5, 5);
		gbc_callsPanel.fill = GridBagConstraints.BOTH;
		gbc_callsPanel.gridx = 0;
		gbc_callsPanel.gridy = 4;
		add(callsPanel, gbc_callsPanel);
		callsPanel.setLayout(new BoxLayout(callsPanel, BoxLayout.Y_AXIS));
		callsPanel.setOpaque(true);
		
		callsParameterPanel = new JPanel();
		GridBagConstraints gbc_callsParameterPanel = new GridBagConstraints();
		gbc_callsParameterPanel.gridwidth = 7;
		gbc_callsParameterPanel.insets = new Insets(0, 0, 5, 5);
		gbc_callsParameterPanel.fill = GridBagConstraints.BOTH;
		gbc_callsParameterPanel.gridx = 4;
		gbc_callsParameterPanel.gridy = 4;
		add(callsParameterPanel, gbc_callsParameterPanel);
		callsParameterPanel.setLayout(new BoxLayout(callsParameterPanel, BoxLayout.Y_AXIS));

		callsOperationsPanel = new JPanel();
		GridBagConstraints gbc_callsOperations = new GridBagConstraints();
		gbc_callsOperations.fill = GridBagConstraints.VERTICAL;
		gbc_callsOperations.anchor = GridBagConstraints.WEST;
		gbc_callsOperations.insets = new Insets(0, 0, 5, 2);
		gbc_callsOperations.gridx = 11;
		gbc_callsOperations.gridy = 4;
		add(callsOperationsPanel, gbc_callsOperations);
		callsOperationsPanel.setOpaque(false);
		callsOperationsPanel.setLayout(new BoxLayout(callsOperationsPanel, BoxLayout.Y_AXIS));
	}

	public boolean isNullJob() {
		return expressionField.getText().equals(DEF_CRON) &&
				callsPanel.getComponentCount() == 1 &&
				((JTextField)callsPanel.getComponent(0)).getText().isBlank() &&
				((JTextField)callsParameterPanel.getComponent(0)).getText().isBlank();
	}

	@Override
	public boolean validateData() {
		if(super.validateData()) {
			for(int i = 0; i < callsPanel.getComponentCount(); i++) {
				if(((JTextField)callsPanel.getComponent(i)).getText().isBlank()) {
					callsPanel.getComponent(i).requestFocus();
					Msg.errorMsg(parentDlg, "schErrorInvalidMethod");
					return false;
				}
				String parameters = ((JTextField)callsParameterPanel.getComponent(i)).getText();
				if(parameters.isBlank() == false) {
					try {
						JSON_MAPPER.readTree("{" + parameters + "}");
					} catch (JacksonException e) {
						callsParameterPanel.getComponent(i).requestFocus();
						Msg.errorMsg(parentDlg, "schErrorInvalidParameters");
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
	
	public boolean hasSystemCalls() {
		return systemJob;
	}

	public ObjectNode getJson() {
		final ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("timespec", expressionField.getText());
		final ArrayNode calls = JsonNodeFactory.instance.arrayNode();
		for(int i = 0; i < callsPanel.getComponentCount(); i++) {
			final ObjectNode call = JsonNodeFactory.instance.objectNode();
			String method = ((JTextField)callsPanel.getComponent(i)).getText();
			call.put("method", method);
			String parameters = ((JTextField)callsParameterPanel.getComponent(i)).getText();
			if(parameters.isBlank() == false) {
				try {
					call.set("params", JSON_MAPPER.readTree("{" + parameters + "}"));
				} catch (JacksonException e) {
					Msg.errorMsg(parentDlg, "schErrorInvalidParameters");
					callsParameterPanel.getComponent(i).requestFocus();
					return null;
				}
			}
			if(method.isBlank() == false || parameters.isBlank() == false) {
				calls.add(call);
			}
		}
		out.set("calls", calls);
		return out;
	}
}