package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.meters.EMDataInterface;
import tools.jackson.databind.JsonNode;

public class EMManager implements EMDataInterface {
	private static final int IND_a_total_act_energy = 0;
	private static final int IND_a_total_act_ret_energy = 2;
	private static final int IND_b_total_act_energy = 16;
	private static final int IND_b_total_act_ret_energy = 18;
	private static final int IND_c_total_act_energy = 32;
	private static final int IND_c_total_act_ret_energy = 34;
	private final AbstractG2Device device;
	private static final int ID = 0;
	
	public EMManager(AbstractG2Device device/*, int id*/) {
		this.device = device;
//		this.id = id;
	}
	
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
			JsonNode energyDataValue = device.getJSON("/rpc/EMData.GetData?add_keys=false&id=" + ID + "&ts=" + nextTs + "&end_ts=" + endTs); // too many values -> too much time
//			System.out.println("-------------------------------------------------------------------");
			for(JsonNode energyData: energyDataValue.get("data")) {
				int ts = energyData.get("ts").intValue(0);
				int period = energyData.get("period").intValue(0);
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
			nextTs = energyDataValue.path("next_record_ts").intValue(0);
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
			JsonNode energyDataValue = device.getJSON("/rpc/EMData.GetData?add_keys=false&id=" + ID + "&ts=" + nextTs + "&end_ts=" + endTs); // too many values -> too much time
//			System.out.println("-------------------------------------------------------------------");
			for(JsonNode energyData: energyDataValue.get("data")) {
				int ts = energyData.get("ts").intValue(0);
				int period = energyData.get("period").intValue(0);
				JsonNode enArray = energyData.get("values");
				for(JsonNode valArray: enArray) {
					data.add(new TimedData(ts, new float[] {
							valArray.get(IND_a_total_act_energy).floatValue() - valArray.get(IND_a_total_act_ret_energy).floatValue(),
							valArray.get(IND_b_total_act_energy).floatValue() - valArray.get(IND_b_total_act_ret_energy).floatValue(),
							valArray.get(IND_c_total_act_energy).floatValue() - valArray.get(IND_c_total_act_ret_energy).floatValue(),
					}));
					ts += period;
//					System.out.println(data.get(data.size() - 1));
				}
			}
			nextTs = energyDataValue.path("next_record_ts").intValue(0);
		} while(nextTs > 0);

		return data;
	}
	
	@Override
	public int getNumLines() {
		return 3;
	}

	public static String[] getInfoRequests(String [] cmd) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length);
		newArray[cmd.length] = "/rpc/EMData.GetRecords?id=0";
		return newArray;
	}
}
