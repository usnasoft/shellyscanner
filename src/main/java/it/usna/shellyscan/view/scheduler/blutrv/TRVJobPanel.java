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

public class TRVJobPanel extends AbstractCronPanel {
	private static final long serialVersionUID = 1L;
	private float minTarget, maxTarget;
	private JRadioButton rdbtnTemp;
	private JRadioButton rdbtnPosition;
	private NumericTextField<Float> target;
//	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

	TRVJobPanel(JDialog parent, float minTarget, float maxTarget, JsonNode scheduleNode) {
		super(parent);
		this.minTarget = minTarget;
		this.maxTarget = maxTarget;
		target = new NumericTextField<Float>(/*(maxTarget - minTarget)/2,*/ minTarget, maxTarget, Locale.ENGLISH);
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
		target.setColumns(4);
		target.setMaximumFractionDigits(1);
		JPanel targetpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		targetpanel.setOpaque(false);

		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 0, 0);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 3;
		gbc_lblNewLabel_5.gridwidth = 8;
		add(targetpanel, gbc_lblNewLabel_5);
		
		rdbtnTemp = new JRadioButton(LABELS.getString("lblTargetTemp"));
		targetpanel.add(rdbtnTemp);
		rdbtnPosition = new JRadioButton(LABELS.getString("lblTargetPos"));
		targetpanel.add(rdbtnPosition);
		targetpanel.add(target);
		
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(rdbtnTemp);
		modeGroup.add(rdbtnPosition);
		
		rdbtnTemp.addItemListener(e -> {
			if(rdbtnTemp.isSelected()) {
				target.setMaximumFractionDigits(1);
				target.setLimits(minTarget, maxTarget);
				if(target.isEmpty() == false) {
					target.setValue(UtilMiscellaneous.clamp(target.getFloatValue(), minTarget, maxTarget));
				}
				target.requestFocus();
			}
		});
		
		rdbtnPosition.addItemListener(e -> {
			if(rdbtnPosition.isSelected()) {
				target.setMaximumFractionDigits(0);
				target.setLimits(0f, 100f);
				// clamp on °F ?
				target.requestFocus();
			}
		});
	}
	
	public void setTarget(JsonNode scheduleNode) {
		if(scheduleNode == null) {
			rdbtnTemp.setSelected(true);
		} else {
			if(scheduleNode.hasNonNull("target_C")) {
				rdbtnTemp.setSelected(true);
				target.setValue(scheduleNode.get("target_C").floatValue());
			} else if(scheduleNode.hasNonNull("pos")) {
				rdbtnPosition.setSelected(true);
				target.setValue(scheduleNode.get("pos").intValue());
			}
		}
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

	public ObjectNode getJson() {
		final ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("timespec", expressionField.getText());
		if(target.isEmpty() == false) {
			if(rdbtnTemp.isSelected()) {
				out.put("target_C", target.getFloatValue());
			} else {
				out.put("pos", target.getIntValue());
			}
		}
		return out;
	}
}