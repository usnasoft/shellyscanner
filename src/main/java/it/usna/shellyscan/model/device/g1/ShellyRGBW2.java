package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.MetersPower;
import it.usna.shellyscan.model.device.g1.modules.LightRGBW;
import it.usna.shellyscan.model.device.g1.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.RGBWCommander;
import it.usna.shellyscan.model.device.modules.WhiteCommander;

public class ShellyRGBW2 extends AbstractG1Device implements RGBWCommander, WhiteCommander {
	public final static String ID = "SHRGBW2";
	private boolean modeColor;
	private LightRGBW color;
	private LightWhite white0, white1, white2, white3;
	private float power[] = new float[4];
	
	private final String MODE_COLOR = "color";

	public ShellyRGBW2(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly RGBW2";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	public float getPower(int index) {
		return power[index];
	}
	
	@Override
	public Meters[] getMeters() {
		if(modeColor) {
			return new Meters[] {
					new MetersPower() {
						@Override
						public float getValue(Type t) {
							return power[0];
						}
					}
			};
		} else {
			return new Meters[] {
					new MetersPower() {
						@Override
						public float getValue(Type t) {
							return power[0];
						}
					},
					new MetersPower() {
						@Override
						public float getValue(Type t) {
							return power[1];
						}
					},
					new MetersPower() {
						@Override
						public float getValue(Type t) {
							return power[2];
						}
					},
					new MetersPower() {
						@Override
						public float getValue(Type t) {
							return power[3];
						}
					}
			};
		}
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		modeColor = MODE_COLOR.equals(settings.get("mode").asText());
		if(modeColor) {
			if(color == null) {
				color = new LightRGBW(this, 0);
				white0 = white1 = white2 = white3 = null;
			}
		} else {
			JsonNode lightsSettings = settings.get("lights");
			if(white0 == null /*|| white1 == null || white2 == null || white3 == null*/) {
				white0 = new LightWhite(this, "/white/", 0);
				white1 = new LightWhite(this, "/white/", 1);
				white2 = new LightWhite(this, "/white/", 2);
				white3 = new LightWhite(this, "/white/", 3);
				color = null;
			}
			white0.fillSettings(lightsSettings.get(0));
			white1.fillSettings(lightsSettings.get(1));
			white2.fillSettings(lightsSettings.get(2));
			white3.fillSettings(lightsSettings.get(3));
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		if(modeColor) {
			color.fillStatus(status.get("lights").get(0));
			power[0] = (float)status.get("meters").get(0).get("power").asDouble(0);
		} else {
			white0.fillStatus(status.get("lights").get(0));
			white1.fillStatus(status.get("lights").get(1));
			white2.fillStatus(status.get("lights").get(2));
			white3.fillStatus(status.get("lights").get(3));
			final JsonNode meters = status.get("meters");
			power[0] = (float)meters.get(0).get("power").asDouble(0);
			power[1] = (float)meters.get(1).get("power").asDouble(0);
			power[2] = (float)meters.get(2).get("power").asDouble(0);
			power[3] = (float)meters.get(3).get("power").asDouble(0);
		}
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "mode", "led_status_disable", "factory_reset_from_switch")));
		JsonNode nightMode = settings.get("night_mode");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		if(nightMode != null) {
			if(nightMode.get("enabled").asBoolean()) {
				errors.add(sendCommand("/settings/night_mode?" + jsonNodeToURLPar(nightMode, "enabled", "start_time", "end_time", "brightness")));
			} else {
				errors.add(sendCommand("/settings/night_mode?enabled=false"));
			}
		}
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		final boolean backModeColor = MODE_COLOR.equals(settings.get("mode").asText());
		if(backModeColor) {
			final LightRGBW color = new LightRGBW(this, 0); // just for restore; object is later refreshed (fill called)
			color.restore(settings.get("lights").get(0));
		} else {
			LightWhite w = new LightWhite(this, "/white/", 0); // just for restore; object is later refreshed (fill called)
			w.restore(settings.get("lights").get(0));
			w = new LightWhite(this, "/white/", 1);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			w.restore(settings.get("lights").get(1));
			w = new LightWhite(this, "/white/", 2);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			w.restore(settings.get("lights").get(2));
			w = new LightWhite(this, "/white/", 3);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			w.restore(settings.get("lights").get(3));
		}
	}
	
	public boolean isColorMode() {
		return modeColor;
	}
	
	@Override
	public LightRGBW getColor(int index) {
		return color;
	}
	
	@Override
	public int getColorCount() {
		return modeColor ? 1 : 0;
	}
	
	@Override
	public LightWhite getWhite(int index) {
		if(index == 0) {
			return white0;
		} else if(index == 1) {
			return white1;
		} else if(index == 2) {
			return white2;
		} else {
			return white3;
		}
	}
	
	@Override
	public LightWhite[] getWhites() {
		return new LightWhite[] {white0, white1, white2, white3};
	}
	
	@Override
	public int getWhiteCount() {
		return modeColor ? 0 : 4;
	}
}