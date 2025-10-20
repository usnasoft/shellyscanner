package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.meters.EMDataInterface;
import tools.jackson.databind.JsonNode;

public class EM1Manager implements EMDataInterface {
//	private static final int PERIOD = 3600; // seconds {300, 900, 1800, or 3600}
	private static final int IND_total_act_energy = 0;
	private static final int IND_total_act_ret_energy = 1;
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
	public List<TimedData> getData(int startTs, int endTs) throws IOException {
		ArrayList<TimedData> data = new ArrayList<>();
		int nextTs = startTs;
		do {
			JsonNode energyDataValue = device.getJSON("/rpc/EM1Data.GetData?add_keys=false&id=" + id + "&ts=" + nextTs + "&end_ts=" + endTs); // too many values -> too much time
//			System.out.println("-------------------------------------------------------------------");
			for(JsonNode energyData: energyDataValue.get("data")) {
				int ts = energyData.get("ts").intValue();
				int period = energyData.get("period").intValue();
				JsonNode enArray = energyData.get("values");
				for(JsonNode valArray: enArray) {
					float[] values = new float[valArray.size()];
					for(int i = 0; i < values.length; i++) {
						values[i] = valArray.get(i).floatValue();
					}
					data.add(new TimedData(ts, values));
					ts += period;
//					System.out.println(data.get(data.size() - 1));
				}
			}
			nextTs = energyDataValue.path("next_record_ts").intValue();
		} while(nextTs > 0);

		return data;
	}
	
	@Override
	/**
	 * like getData but only returns active energy data
	 */
	public List<TimedData> getEnergyData(int startTs, int endTs) throws IOException {
		ArrayList<TimedData> data = new ArrayList<>();
		int nextTs = startTs;
		do {
			JsonNode energyDataValue = device.getJSON("/rpc/EM1Data.GetData?add_keys=false&id=" + id + "&ts=" + nextTs + "&end_ts=" + endTs); // too many values -> too much time
//			System.out.println("-------------------------------------------------------------------");
			for(JsonNode energyData: energyDataValue.get("data")) {
				int ts = energyData.get("ts").intValue();
				int period = energyData.get("period").intValue();
				JsonNode enArray = energyData.get("values");
				for(JsonNode valArray: enArray) {
					data.add(new TimedData(ts, new float[] {valArray.get(IND_total_act_energy).floatValue() - valArray.get(IND_total_act_ret_energy).floatValue()}));
					ts += period;
//					System.out.println(data.get(data.size() - 1));
				}
			}
			nextTs = energyDataValue.path("next_record_ts").intValue();
		} while(nextTs > 0);

		return data;
	}
	
	@Override
	public int getNumLines() {
		return 1;
	}

	/**
	 * @param startTs e.g. period 3600 -> long end = (System.currentTimeMillis() / 3600) * 3600; long start = end - (3600 * 24 * 7);
	 * @param endTs
	 * @param period seconds {300, 900, 1800, or 3600}
	 * @return
	 * @throws IOException
	 */
	public List<TimedData> getEnergy(int startTs, int endTs, int period) throws IOException {
		ArrayList<TimedData> data = new ArrayList<>();
		int nextTs = startTs;
		do {
			JsonNode energyDataValue = device.getJSON("/rpc/EM1Data.GetNetEnergies?id=" + id + "&ts=" + nextTs + "&end_ts=" + endTs + "&add_keys=false&period=" + period);
//			System.out.println("-------------------------------------------------------------------");
			for(JsonNode energyData: energyDataValue.get("data")) {
				int ts = energyData.get("ts").intValue();
				int thisPeriod = energyData.get("period").intValue();
				JsonNode enArray = energyData.get("values");
				for(JsonNode valArray: enArray) {
					data.add(new TimedData(ts, new float[] {valArray.get(0).floatValue()}));
					ts += thisPeriod;
//					System.out.println(data.get(data.size() - 1));
				}
			}
			nextTs = energyDataValue.path("next_record_ts").intValue();
		} while(nextTs > 0);

		return data;
	}
	
	public static String[] getInfoRequests(String [] cmd, int ... ids) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length + ids.length);
		for(int i = 0; i < ids.length; i++) {
			newArray[cmd.length + i] = "(EM1Data.GetRecords [" + ids[i] + "])/rpc/EM1Data.GetRecords?id=" + ids[i];
		}
		return newArray;
	}
}

//http://192.168.1.200/rpc/EM1Data.GetRecords?id=0 [&ts=1752128820]
//http://192.168.1.200/rpc/EM1Data.GetData?id=0&ts=0 - period = 60 implicit
//http://192.168.1.200/rpc/EM1Data.GetNetEnergies?id=0&period=300&ts=0 - period = '300', '900', '1800', '3600' ... -  // only possible value "net_act_energy"
//http://192.168.1.200/rpc/EM1Data.GetNetEnergies?id=0&ts=1755788400&add_keys=false&period=3600
//http://192.168.1.200/rpc/EM1Data.GetNetEnergies?id=0&ts=1755788400&end_ts=1755874800&add_keys=false&period=3600
//http://192.168.1.200/rpc/EM1Data.GetNetEnergies?id=0&ts=1755788400&end_ts=1752235200&add_keys=false&period=300 - paging (7 days)