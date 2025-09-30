package it.usna.shellyscan.model.device;

import java.io.IOException;
import java.util.List;

public interface EMDataInterface {
	List<TimedData> getEnergyData(int startTs, int endTs) throws IOException;
	
	int getNumLines();
	
	record TimedData(int timestamp, float ... values) {
		@Override
		public String toString() {
			return timestamp + "-" + values[0];
		}
	}
}