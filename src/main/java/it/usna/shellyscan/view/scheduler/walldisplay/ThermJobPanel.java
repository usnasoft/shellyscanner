package it.usna.shellyscan.view.scheduler.walldisplay;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.JDialog;
import javax.swing.JLabel;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.view.scheduler.AbstractCronPanel;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.NumericTextField;

class ThermJobPanel extends AbstractCronPanel {
	private static final long serialVersionUID = 1L;
	private float minTarget, maxTarget;
	private NumericTextField<Float> target;

	ThermJobPanel(JDialog parent, float minTarget, float maxTarget, /*JsonNode scheduleNode*/String timespec, Float temp) {
		super(parent);
		this.minTarget = minTarget;
		this.maxTarget = maxTarget;
		initTempSection();
		if(timespec == null) {
			setCron(DEF_CRON);
			target.setValue(null);
		} else {
			setCron(timespec);
			target.setValue(temp);
		}
	}

	private void initTempSection() {
		target = new NumericTextField<Float>(minTarget, maxTarget, Locale.ENGLISH);
		target.allowNull(true);
		target.setColumns(4);
		target.setMaximumFractionDigits(1);
		target.setLimits(minTarget, maxTarget);

		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 0, 0);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 3;
//		gbc_lblNewLabel_5.gridwidth = 8;
		add(new JLabel(LABELS.getString("lblTargetTemp")), gbc_lblNewLabel_5);

		GridBagConstraints gbc_target = new GridBagConstraints();
		gbc_target.anchor = GridBagConstraints.WEST;
		gbc_target.insets = new Insets(0, 0, 0, 0);
		gbc_target.gridx = 1;
		gbc_target.gridy = 3;
		gbc_target.gridwidth = 8;
		add(target, gbc_target);
	}

	public void clean() {
		setCron(DEF_CRON);
		target.setValue(null);
	}

	public boolean isNullJob() {
		return expressionField.getText().equals(DEF_CRON) && target.getText().isBlank();
	}

	@Override
	public boolean validateData() {
		if(super.validateData()) {
			if(target.isEmpty()) {
				target.requestFocus();
				Msg.errorMsg(parent, "schErrorInvalidTarget");
				return false;
			}
			return true;
		}
		return false;
	}
	
	public String getTimeSpec() {
		return expressionField.getText();
	}
	
	public float getTarget() {
		return target.getFloatValue();
	}
	
	public void setTarget(float temp) {
		target.setValue(temp);
	}

	public ObjectNode getJson() {
		final ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("timespec", expressionField.getText());
		if(target.isEmpty() == false) {
			out.put("target_C", target.getFloatValue());
		}
		return out;
	}
}