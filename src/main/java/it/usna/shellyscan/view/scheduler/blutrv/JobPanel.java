package it.usna.shellyscan.view.scheduler.blutrv;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.view.scheduler.AbstractCronPanel;
import it.usna.shellyscan.view.scheduler.CronUtils;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.NumericTextField;

public class JobPanel extends AbstractCronPanel {
	private static final long serialVersionUID = 1L;
//	private IntegerTextFieldPanel target = new IntegerTextFieldPanel(5, 20);
	private NumericTextField<Float> target = new NumericTextField<Float>(10f, 10f, 30f, Locale.ENGLISH);
//	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

	JobPanel(JDialog parent, JsonNode scheduleNode) {
		super(parent);
		initTempSection();
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));
		if(scheduleNode == null) {
			setCron(DEF_CRON);
		} else {
			setCron(scheduleNode.path("timespec").asText());
		}
	}
	
	private void initTempSection() {
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 3;
		add(new JLabel(LABELS.getString("lblTargetType")), gbc_lblNewLabel_5);
		
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.gridwidth = 2;
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 3;
		gbc_lblNewLabel_6.gridy = 3;
		add(new JLabel(LABELS.getString("lblTargetTemp")), gbc_lblNewLabel_6);
		
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.gridwidth = 2;
		gbc_lblNewLabel_7.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_7.gridx = 3;
		gbc_lblNewLabel_7.gridy = 4;
		add(target, gbc_lblNewLabel_7);
		target.setColumns(4);
		target.setMaximumFractionDigits(1);
	}

	public void clean() {
		setCron(DEF_CRON);
	}

	public boolean isNullJob() {
		return expressionField.getText().equals(DEF_CRON) /*&&
				callsPanel.getComponentCount() == 1 &&
				((JTextField)callsPanel.getComponent(0)).getText().isBlank() &&
				((JTextField)callsParameterPanel.getComponent(0)).getText().isBlank()*/;
	}

	public boolean validateData() {
		String exp = expressionField.getText();
		if(CronUtils.CRON_PATTERN.matcher(exp).matches() == false && CronUtils.SUNSET_PATTERN.matcher(exp).matches() == false) {
			expressionField.requestFocus();
			Msg.errorMsg(parent, "schErrorInvalidExpression");
			return false;
		}
//		for(int i = 0; i < callsPanel.getComponentCount(); i++) {
//			if(((JTextField)callsPanel.getComponent(i)).getText().isBlank()) {
//				callsPanel.getComponent(i).requestFocus();
//				Msg.errorMsg(parent, "schErrorInvalidMethod");
//				return false;
//			}
//			String parameters = ((JTextField)callsParameterPanel.getComponent(i)).getText();
//			if(parameters.isBlank() == false) {
//				try {
//					JSON_MAPPER.readTree("{" + parameters + "}");
//				} catch (JsonProcessingException e) {
//					callsParameterPanel.getComponent(i).requestFocus();
//					Msg.errorMsg(parent, "schErrorInvalidParameters");
//					return false;
//				}
//			}
//		}
		return true;
	}

	public ObjectNode getJson() {
		final ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("timespec", expressionField.getText());
//		final ArrayNode calls = JsonNodeFactory.instance.arrayNode();
//		for(int i = 0; i < callsPanel.getComponentCount(); i++) {
//			final ObjectNode call = JsonNodeFactory.instance.objectNode();
//			call.put("method", ((JTextField)callsPanel.getComponent(i)).getText());
//			String parameters = ((JTextField)callsParameterPanel.getComponent(i)).getText();
//			if(parameters.isBlank() == false) {
//				try {
//					call.set("params", JSON_MAPPER.readTree("{" + parameters + "}"));
//				} catch (JsonProcessingException e) {
//					Msg.errorMsg(parent, "schErrorInvalidParameters");
//					callsParameterPanel.getComponent(i).requestFocus();
//					return null;
//				}
//			}
//			calls.add(call);
//		}
//		out.set("calls", calls);
		return out;
	}
}