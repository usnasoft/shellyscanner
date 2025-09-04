package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class EM1Manager {
	public static final String ACT_ENERGY = "total_act_energy";
	private static final int PERIOD = 3600; // seconds {300, 900, 1800, or 3600}
	private final AbstractG2Device device;
	private final int id;
	
	public EM1Manager(AbstractG2Device device, int id) {
		this.device = device;
		this.id = id;
	}
	
	/**
	 * return all available type id
	 * @return
	 */
//	public String[] getTypes() {
//		return null;
//	}
	
	/**
	 * return all available data for a given type id
	 * @param dataType data type id
	 * @return
	 * @throws IOException 
	 */
//	int end = (int)((System.currentTimeMillis()/1000) / 60) * 60;
//	int start = end - (3600 * 2);
	public List<EnergyData> getEnergyData(String dataType, int startTs, int endTs) throws IOException {
		ArrayList<EnergyData> data = new ArrayList<>();
		int nextTs = startTs;
		do {
			JsonNode energyDataValue = device.getJSON("/rpc/EM1Data.GetData?add_keys=false&id=" + id + "&ts=" + nextTs + "&end_ts=" + endTs); // too many values -> too much time
			System.out.println("-------------------------------------------------------------------");
			for(JsonNode energyData: energyDataValue.get("data")) {
				int ts = energyData.get("ts").intValue();
				int period = energyData.get("period").intValue();
				JsonNode enArray = energyData.get("values");
				for(JsonNode valArray: enArray) {

					data.add(new EnergyData(ts /** 1000L*/, valArray.get(0).floatValue()));
					ts += period;

					System.out.println(data.get(data.size() - 1));
				}
			}
			nextTs = energyDataValue.path("next_record_ts").intValue();
		} while(nextTs > 0);

		return data;
	}
	
	/*
		long end = (System.currentTimeMillis() / 3600) * 3600;
		long start = end - (3600 * 24 * 7);
	 */
	public List<EnergyData> getEnergy(int startTs, int endTs) throws IOException {
		ArrayList<EnergyData> data = new ArrayList<>();
		int nextTs = startTs;
		do {
			JsonNode energyDataValue = device.getJSON("/rpc/EM1Data.GetNetEnergies?id=" + id + "&ts=" + nextTs + "&end_ts=" + endTs + "&add_keys=false&period=" + PERIOD);
			System.out.println("-------------------------------------------------------------------");
			for(JsonNode energyData: energyDataValue.get("data")) {
				int ts = energyData.get("ts").intValue();
				int period = energyData.get("period").intValue();
				JsonNode enArray = energyData.get("values");
				for(JsonNode valArray: enArray) {
					data.add(new EnergyData(ts /** 1000L*/, valArray.get(0).floatValue()));
					ts += period;

					System.out.println(data.get(data.size() - 1));
				}
			}
			nextTs = energyDataValue.path("next_record_ts").intValue();
		} while(nextTs > 0);

		return data;
	}
	
	/**
	 * get records from a given timestamp
	 * @param dataType data type id
	 * @param FromTs timestamp
	 * @return
	 */
	public List<EnergyData> getFrom(String dataType, long fromTs) { // or array
		return null;
	}
	
	/**
	 * get unread records
	 * @param dataType data type id
	 * @param FromTs timestamp
	 * @return
	 */
	public List<EnergyData> getNextEnergy(/*String dataType*/) {
		return null;
	}
	
	public static String[] getInfoRequests(String [] cmd, int ... ids) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length + ids.length);
		for(int i = 0; i < ids.length; i++) {
			newArray[cmd.length + i] = "(EM1Data.GetRecords [" + ids[i] + "])/rpc/EM1Data.GetRecords?id=" + ids[i];
		}
		return newArray;
	}
	
	//l.add("(BTHomeSensor.GetConfig [" + s.getId() + "-" + s.getObjId() + "])/rpc/BTHomeSensor.GetCon)fig?id=" + s.getId());

//	public record TimedData(long timestamp, float value) {}
	public record EnergyData(long timestamp, float ... value) {
		@Override
		public String toString() {
			return timestamp + "-" + value[0];
		}
	}
}

//todo questo diventa EM1Manager che deriva da un'interffacia da cui deriverà anche EMManager (e forse anche EMPMManager)

// to it.usna.shellyscan.view.chart.MeasuresChart.typeComboContent(int[]) - if device instanceod EMHolder add EM type(s)

//http://192.168.1.200/rpc/EM1Data.GetRecords?id=0 [&ts=1752128820]
//http://192.168.1.200/rpc/EM1Data.GetData?id=0&ts=0 - period = 60 implicit
//http://192.168.1.200/rpc/EM1Data.GetNetEnergies?id=0&period=300&ts=0 - period = '300', '900', '1800', '3600' ... -  // only possible value "net_act_energy"
//http://192.168.1.200/rpc/EM1Data.GetNetEnergies?id=0&ts=1755788400&add_keys=false&period=3600
//http://192.168.1.200/rpc/EM1Data.GetNetEnergies?id=0&ts=1755788400&end_ts=1755874800&add_keys=false&period=3600
//http://192.168.1.200/rpc/EM1Data.GetNetEnergies?id=0&ts=1755788400&end_ts=1752235200&add_keys=false&period=300 - paging (7 days)
