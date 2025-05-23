package it.usna.shellyscan.view.chart;

import static it.usna.shellyscan.Main.LABELS;

import it.usna.shellyscan.model.device.Meters;

public enum ChartType {
	INT_TEMP("dlgChartsIntTempLabel", "dlgChartsTempYLabel"),
	RSSI("dlgChartsRSSILabel", "dlgChartsRSSIYLabel"),
	P("dlgChartsAPowerLabel", "dlgChartsAPowerYLabel", Meters.Type.W),
	P_SUM("dlgChartsAPowerSumLabel", "dlgChartsAPowerYLabel"),
	Q("dlgChartsQPowerLabel", "dlgChartsQPowerYLabel", Meters.Type.VAR), // only gen1 EM
	S("dlgChartsSPowerLabel", "dlgChartsSPowerYLabel", Meters.Type.VA),
	V("dlgChartsVoltageLabel", "dlgChartsVoltageYLabel", Meters.Type.V),
	I("dlgChartsCurrentLabel", "dlgChartsCurrentYLabel", Meters.Type.I),
	//		T("dlgChartsTempLabel", "dlgChartsTempYLabel", Meters.Type.T),
	//		T1("dlgChartsTemp1Label", "dlgChartsTempYLabel", Meters.Type.T1),
	//		T2("dlgChartsTemp2Label", "dlgChartsTempYLabel", Meters.Type.T2),
	//		T3("dlgChartsTemp3Label", "dlgChartsTempYLabel", Meters.Type.T3),
	//		T4("dlgChartsTemp4Label", "dlgChartsTempYLabel", Meters.Type.T4),
	T_ALL("dlgChartsTempAllLabel", "dlgChartsTempYLabel", Meters.Type.T),
	H("dlgChartsHumidityLabel", "dlgChartsHumidityYLabel", Meters.Type.H),
	LUX("dlgChartsLuxLabel", "dlgChartsLuxYLabel", Meters.Type.L),
	FREQ("dlgChartsFreqLabel", "dlgChartsFreqYLabel", Meters.Type.FREQ),
	DIST("dlgChartsDistanceLabel", "dlgChartsDistanceYLabel", Meters.Type.DMM);

	final String yLabel;
	final String label;
	final Meters.Type mType;

	private ChartType(String labelID, String yLabelID) {
		this.yLabel = LABELS.getString(yLabelID);
		this.label = LABELS.getString(labelID);
		this.mType = null;
	}

	private ChartType(String labelID, String yLabelID, Meters.Type mType) {
		this.yLabel = LABELS.getString(yLabelID);
		this.label = LABELS.getString(labelID);
		this.mType = mType;
	}

	@Override
	public String toString() { // combo box
		return label;
	}
}