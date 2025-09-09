package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.Arrays;

public class EMManager {
//	public static final String ACT_ENERGY = "total_act_energy";
//	private static final int PERIOD = 3600; // seconds {300, 900, 1800, or 3600}
//	private final AbstractG2Device device;
//	private final int id;
	
//	public EMManager(AbstractG2Device device, int id) {
//		this.device = device;
//		this.id = id;
//	}

	/**
	 * return all available data for a given type id
	 * @param dataType data type id
	 * @return
	 * @throws IOException 
	 */
//	int end = (int)((System.currentTimeMillis()/1000) / 60) * 60;
//	int start = end - (3600 * 2);
//	public List<TimedData> getData(int startTs, int endTs) throws IOException {
//		ArrayList<TimedData> data = new ArrayList<>();
//		int nextTs = startTs;
//		do {
//			JsonNode energyDataValue = device.getJSON("/rpc/EMData.GetData?add_keys=false&id=" + id + "&ts=" + nextTs + "&end_ts=" + endTs); // too many values -> too much time
//			System.out.println("-------------------------------------------------------------------");
//			for(JsonNode energyData: energyDataValue.get("data")) {
//				int ts = energyData.get("ts").intValue();
//				int period = energyData.get("period").intValue();
//				JsonNode enArray = energyData.get("values");
//				for(JsonNode valArray: enArray) {
//					float[] values = new float[valArray.size()];
//					for(int i = 0; i < values.length; i++) {
//						values[i] = valArray.get(i).floatValue();
//					}
//					data.add(new TimedData(ts /** 1000L*/, values));
//					ts += period;
//
//					System.out.println(data.get(data.size() - 1));
//				}
//			}
//			nextTs = energyDataValue.path("next_record_ts").intValue();
//		} while(nextTs > 0);
//
//		return data;
//	}
//	
//	/*
//		long end = (System.currentTimeMillis() / 3600) * 3600;
//		long start = end - (3600 * 24 * 7);
//	 */
//	public List<TimedData> getEnergy(int startTs, int endTs) throws IOException {
//		ArrayList<TimedData> data = new ArrayList<>();
//		int nextTs = startTs;
//		do {
//			JsonNode energyDataValue = device.getJSON("/rpc/EMData.GetNetEnergies?id=" + id + "&ts=" + nextTs + "&end_ts=" + endTs + "&add_keys=false&period=" + PERIOD);
//			System.out.println("-------------------------------------------------------------------");
//			for(JsonNode energyData: energyDataValue.get("data")) {
//				int ts = energyData.get("ts").intValue();
//				int period = energyData.get("period").intValue();
//				JsonNode enArray = energyData.get("values");
//				for(JsonNode valArray: enArray) {
//					data.add(new TimedData(ts /** 1000L*/, valArray.get(0).floatValue(), valArray.get(1).floatValue(), valArray.get(2).floatValue()));
//					ts += period;
//
//					System.out.println(data.get(data.size() - 1));
//				}
//			}
//			nextTs = energyDataValue.path("next_record_ts").intValue();
//		} while(nextTs > 0);
//
//		return data;
//	}

	public static String[] getInfoRequests(String [] cmd/*, int ... ids*/) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length + /*ids.length*/ 1);
//		for(int i = 0; i < ids.length; i++) {
//			newArray[cmd.length + i] = "(EMData.GetRecords [" + ids[i] + "])/rpc/EMData.GetRecords?id=" + ids[i];
			newArray[cmd.length] = "/rpc/EMData.GetRecords?id=0";
//		}
		return newArray;
	}

//	public record TimedData(long timestamp, float ... value) {
//		@Override
//		public String toString() {
//			return timestamp + "-" + value[0];
//		}
//	}
}
