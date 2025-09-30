package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.EMDataInterface;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

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
	
	@Override
	public List<TimedData> getEnergyData(int startTs, int endTs) throws IOException {
		ArrayList<TimedData> data = new ArrayList<>();
		int nextTs = startTs;
		do {
			JsonNode energyDataValue = device.getJSON("/rpc/EMData.GetData?add_keys=false&id=" + ID + "&ts=" + nextTs + "&end_ts=" + endTs); // too many values -> too much time
//			System.out.println("-------------------------------------------------------------------");
			for(JsonNode energyData: energyDataValue.get("data")) {
				int ts = energyData.get("ts").intValue();
				int period = energyData.get("period").intValue();
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
			nextTs = energyDataValue.path("next_record_ts").intValue();
		} while(nextTs > 0);

		return data;
	}
	
	@Override
	public int getNumLines() {
		return 3;
	}

	public static String[] getInfoRequests(String [] cmd/*, int ... ids*/) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length + /*ids.length*/ 1);
//		for(int i = 0; i < ids.length; i++) {
//			newArray[cmd.length + i] = "(EMData.GetRecords [" + ids[i] + "])/rpc/EMData.GetRecords?id=" + ids[i];
			newArray[cmd.length] = "/rpc/EMData.GetRecords?id=0";
//		}
		return newArray;
	}
}
