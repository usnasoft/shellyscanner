package it.usna.shellyscan.view.scheduler.blutrv;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.view.scheduler.AbstractCronPanel;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.NumericTextField;

/**
 * Cron panel + target temp / valve position
 * No support for °F for now
 */
public class TRVJobPanel extends AbstractCronPanel {
	private static final long serialVersionUID = 1L;
	private float minTarget, maxTarget;
	private JRadioButton rdbtnTemp;
	private JRadioButton rdbtnPosition;
	private NumericTextField<Float> targetField;

	TRVJobPanel(JDialog parent, float minTarget, float maxTarget, JsonNode scheduleNode) {
		super(parent);
		this.minTarget = minTarget;
		this.maxTarget = maxTarget;
		targetField = new NumericTextField<>(minTarget, maxTarget, Locale.ENGLISH);
		initTempSection();
		if(scheduleNode == null) {
			setCron(DEF_CRON);
			setTarget(null);
		} else {
			setCron(scheduleNode.path("timespec").asText());
			setTarget(scheduleNode);
		}
	}
	
	private void initTempSection() {
		targetField.setColumns(4);
		targetField.setMaximumFractionDigits(1);
		JPanel targetpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		targetpanel.setOpaque(false);

		GridBagConstraints gbc_targetpanel = new GridBagConstraints();
		gbc_targetpanel.anchor = GridBagConstraints.WEST;
		gbc_targetpanel.insets = new Insets(0, 0, 0, 0);
		gbc_targetpanel.gridx = 0;
		gbc_targetpanel.gridy = 3;
		gbc_targetpanel.gridwidth = 8;
		add(targetpanel, gbc_targetpanel);
		
		rdbtnTemp = new JRadioButton(LABELS.getString("lblTargetTemp"));
		targetpanel.add(rdbtnTemp);
		rdbtnPosition = new JRadioButton(LABELS.getString("lblTargetPos"));
		targetpanel.add(rdbtnPosition);
		targetpanel.add(targetField);
		
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(rdbtnTemp);
		modeGroup.add(rdbtnPosition);
		
		rdbtnTemp.addItemListener(e -> {
			if(rdbtnTemp.isSelected()) {
				targetField.setMaximumFractionDigits(1);
				targetField.setLimits(minTarget, maxTarget);
				if(targetField.isEmpty() == false) {
					targetField.setValue(UtilMiscellaneous.clamp(targetField.getFloatValue(), minTarget, maxTarget));
				}
				targetField.requestFocus();
			}
		});
		
		rdbtnPosition.addItemListener(e -> {
			if(rdbtnPosition.isSelected()) {
				targetField.setMaximumFractionDigits(0);
				targetField.setLimits(0f, 100f);
				// clamp on °F ?
				targetField.requestFocus();
			}
		});
	}
	
	public void setTarget(JsonNode scheduleNode) {
		if(scheduleNode == null) {
			rdbtnTemp.setSelected(true);
		} else {
			if(scheduleNode.hasNonNull("target_C")) {
				rdbtnTemp.setSelected(true);
				targetField.setValue(scheduleNode.get("target_C").floatValue());
			} else if(scheduleNode.hasNonNull("pos")) {
				rdbtnPosition.setSelected(true);
				targetField.setValue(scheduleNode.get("pos").intValue());
			}
		}
	}

	public void clean() {
		setCron(DEF_CRON);
		targetField.setValue(null);
	}

	public boolean isNullJob() {
		return expressionField.getText().equals(DEF_CRON) && targetField.getText().isBlank();
	}

	@Override
	public boolean validateData() {
		if(super.validateData()) {
			if(targetField.isEmpty()) {
				targetField.requestFocus();
				Msg.errorMsg(parentDlg, "schErrorInvalidTarget");
				return false;
			}
			return true;
		}
		return false;
	}

	public ObjectNode getJson() {
		final ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("timespec", expressionField.getText());
		if(targetField.isEmpty() == false) {
			if(rdbtnTemp.isSelected()) {
				out.put("target_C", targetField.getFloatValue());
			} else {
				out.put("pos", targetField.getIntValue());
			}
		}
		return out;
	}
}