package it.usna.shellyscan.view.scheduler.gen2plus;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.controller.UsnaDropdownAction;
import it.usna.shellyscan.view.scheduler.AbstractCronPanel;
import it.usna.shellyscan.view.util.Msg;

public class G2JobPanel extends AbstractCronPanel {
	private static final long serialVersionUID = 1L;
	private JPanel callsPanel;
	private JPanel callsParameterPanel;
	private JPanel callsOperationsPanel;
	private boolean systemJob = false;
	private final MethodHints mHints;
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

	/**
	 * @wbp.parser.constructor
	 */
	public G2JobPanel(JDialog parent, JsonNode scheduleNode, MethodHints mHints) {
		super(parent);
		initCallSection();
		this.mHints = mHints;
		if(scheduleNode == null) {
			setCron(DEF_CRON);
			addCall("", "", 0);
		} else {
			setCron(scheduleNode.path("timespec").asText());
			JsonNode calls = scheduleNode.path("calls");
			if(calls.size() > 0) {
				setCalls(scheduleNode.path("calls"));
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
			addCall(call.path("method").asText(), params.isEmpty() ? "" :  params.substring(1, params.length() - 1), i);
		}
		if(systemJob) {
			enableEdit(callsPanel, false);
			enableEdit(callsParameterPanel, false);
			enableEdit(callsOperationsPanel, false);
		}
	}

	private void addCall(String method, String params/*, String origin*/, int index) {
		JTextField methodTF = new JTextField(method);
		JTextField paramsTF = new JTextField(params);
		callsPanel.add(methodTF, index);
		callsParameterPanel.add(paramsTF, index);

		JPanel callOpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		callOpPanel.setOpaque(false);
		JButton addB = new JButton(new ImageIcon(getClass().getResource("/images/plus_transp16.png")));
		addB.setContentAreaFilled(false);
		addB.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		addB.addActionListener(e ->  {
			Component[] list = callsOperationsPanel.getComponents();
			int i;
			for(i = 0; list[i] != callOpPanel; i++);
			addCall("", "", i + 1);
			callsOperationsPanel.revalidate();
		});
		callOpPanel.add(addB);
		JButton minusB = new JButton(new ImageIcon(getClass().getResource("/images/erase-9-16.png")));
		minusB.setContentAreaFilled(false);
		minusB.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		minusB.addActionListener(e ->  {
			Component[] list = callsOperationsPanel.getComponents();
			if(list.length > 1) {
				int i;
				for(i = 0; list[i] != callOpPanel; i++);
				callsPanel.remove(i);
				callsParameterPanel.remove(i);
				callsOperationsPanel.remove(i);
				callsOperationsPanel.revalidate();
			}
		});
		callOpPanel.add(minusB);

		JButton btnSelectCombo = new JButton();
		btnSelectCombo.setAction(new UsnaDropdownAction(btnSelectCombo, "/images/expand-more.png", "lblMethodSelect", () -> {
			try {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				return mHints.get(methodTF, paramsTF);
			} finally {
				this.setCursor(Cursor.getDefaultCursor());
			}
		}));
		btnSelectCombo.setContentAreaFilled(false);
		btnSelectCombo.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		callOpPanel.add(btnSelectCombo);

		callsOperationsPanel.add(callOpPanel, index);
	}

	public void clean() {
		callsPanel.removeAll();
		callsParameterPanel.removeAll();
		callsOperationsPanel.removeAll();
		setCron(DEF_CRON);
		addCall("", "", 0);
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
		gbc_callsParameterPanel.gridwidth = 6;
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
		gbc_callsOperations.insets = new Insets(0, 0, 5, 5);
		gbc_callsOperations.gridx = 10;
		gbc_callsOperations.gridy = 4;
		add(callsOperationsPanel, gbc_callsOperations);
		callsOperationsPanel.setOpaque(false);
		//		callsOperationsPanel.setBackground(Color.red);
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
					Msg.errorMsg(parent, "schErrorInvalidMethod");
					return false;
				}
				String parameters = ((JTextField)callsParameterPanel.getComponent(i)).getText();
				if(parameters.isBlank() == false) {
					try {
						JSON_MAPPER.readTree("{" + parameters + "}");
					} catch (JsonProcessingException e) {
						callsParameterPanel.getComponent(i).requestFocus();
						Msg.errorMsg(parent, "schErrorInvalidParameters");
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
				} catch (JsonProcessingException e) {
					Msg.errorMsg(parent, "schErrorInvalidParameters");
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