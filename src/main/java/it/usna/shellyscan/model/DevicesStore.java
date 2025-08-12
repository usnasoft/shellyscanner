package it.usna.shellyscan.model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.ShellyUnmanagedDeviceInterface;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
import it.usna.shellyscan.model.device.blu.BTHomeDevice;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g3.AbstractG3Device;

public class DevicesStore {
	private static final Logger LOG = LoggerFactory.getLogger(DevicesStore.class);
	private static final int STORE_VERSION = 0;
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static final String TYPE_ID = "tid";
	private static final String GENERATION = "gen";
	private static final String TYPE_NAME = "tn";
	private static final String NAME = "name";
	private static final String HOSTNAME = "host";
	private static final String MAC = "mac";
	private static final String ADDRESS = "ip";
	private static final String PORT = "port";
	private static final String SSID = "ssid";
	private static final String BATTERY = "bat";
	private static final String LAST_CON = "last";
	private static final String USER_NOTE = "note";
	private static final String KEYWORD_NOTE = "keyword";

	private static final Pattern MAC_PATTERN = Pattern.compile("^([a-f0-9]{12})|([a-f0-9]{2}(:[a-f0-9]{2}){5})$", Pattern.CASE_INSENSITIVE);
	
	private final ArrayList<GhostDevice> ghostsList = new ArrayList<>();

	public void store(Devices model, Path storeFile) throws IOException {
		LOG.trace("storing archive");
		final ObjectNode root = JsonNodeFactory.instance.objectNode();
		root.put("ver", STORE_VERSION);
		root.put("time", System.currentTimeMillis());
		final ArrayNode toBeStored = JsonNodeFactory.instance.arrayNode();
		for (int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice device = model.get(i);
			GhostDevice stored = getStoredGhost(device);
			// Device with errors or not authenticated -> get data from old store
			if((device instanceof ShellyUnmanagedDeviceInterface ud && ud.getException() != null) || device.getStatus() == Status.NOT_LOOGGED) {
				if(stored != null) {
					ObjectNode jsonDev = toJson(stored);
					jsonDev.put(USER_NOTE, stored.getNote());
					jsonDev.put(KEYWORD_NOTE, stored.getKeyNote());
					jsonDev.put(ADDRESS, device.getAddressAndPort().getIpAsText()); // from actual device
					jsonDev.put(PORT, device.getAddressAndPort().getPort()); // from actual device
					toBeStored.add(jsonDev);
				} else if(MAC_PATTERN.matcher(device.getMacAddress()).matches()) {
					toBeStored.add(toJson(device));
				}
			} else {
				ObjectNode jsonDev = toJson(device);
				if(stored != null) {
					jsonDev.put(USER_NOTE, stored.getNote());
					jsonDev.put(KEYWORD_NOTE, stored.getKeyNote());
					if(device.getLastTime() < stored.getLastTime()) { // device.getLastTime() == 0 for originally off-line BLU devices 
						jsonDev.put(LAST_CON, stored.getLastTime());
					}
				}
				toBeStored.add(jsonDev);
			}
		}
		root.set("dev", toBeStored);

		try (Writer w = Files.newBufferedWriter(storeFile, StandardCharsets.UTF_8)) {
			JSON_MAPPER.writer().writeValue(w, root);
		}
	}
	
	private static ObjectNode toJson(ShellyAbstractDevice device) {
		ObjectNode jsonDev = JsonNodeFactory.instance.objectNode();
		jsonDev.put(TYPE_ID, device.getTypeID());
		jsonDev.put(TYPE_NAME, device.getTypeName());
		jsonDev.put(HOSTNAME, device.getHostname());
		jsonDev.put(MAC, device.getMacAddress());
		jsonDev.put(ADDRESS, device.getAddressAndPort().getIpAsText());
		jsonDev.put(PORT, device.getAddressAndPort().getPort());
		jsonDev.put(NAME, device.getName());
		jsonDev.put(SSID, device.getSSID());
		jsonDev.put(LAST_CON, device.getLastTime());
		jsonDev.put(BATTERY, device instanceof BatteryDeviceInterface  || (device instanceof GhostDevice g && g.isBatteryOperated()));
		jsonDev.put(GENERATION, gen(device));
		return jsonDev;
	}

	/**
	 * Read a store file
	 * @param storeFile
	 * @return ghosts list 
	 * @throws IOException
	 */
	public synchronized List<GhostDevice> read(Path storeFile) throws IOException {
		LOG.trace("reading archive");
		ghostsList.clear();
		try (BufferedReader r = Files.newBufferedReader(storeFile, StandardCharsets.UTF_8)) {
			final JsonNode arc = JSON_MAPPER.readTree(r);
			if(arc.path("ver").asInt() == STORE_VERSION) {
				final ArrayNode array = (ArrayNode) arc.get("dev");
				if(array != null) {
					array.forEach(el -> {
						try {
							ghostsList.add(new GhostDevice(
									InetAddress.getByName(el.get(ADDRESS).asText()), el.get(PORT).intValue(), el.get(HOSTNAME).asText(), el.get(MAC).asText(),
									el.get(SSID).asText(), el.get(TYPE_NAME).asText(), el.get(TYPE_ID).asText(), el.path(GENERATION).asText(), el.get(NAME).asText(), el.path(LAST_CON).longValue(),
									el.path(BATTERY).booleanValue(), el.path(USER_NOTE).asText(), el.path(KEYWORD_NOTE).asText()));
						} catch (UnknownHostException | RuntimeException e) {
							LOG.error("Archive read", e);
						}
					});
				}
			} else {
				LOG.info("Archive version is {}; " + STORE_VERSION + " expected", arc.path("ver").asText());
			}
		} catch(FileNotFoundException | NoSuchFileException e) {
			// first run?
		} catch (IOException e) {
			LOG.error("Archive read", e);
			throw e;
		}
		return ghostsList;
	}
	
	/**
	 * Map the full model to a list of ghosts - ignore notes
	 */
	public List<GhostDevice> toGhosts(Devices model) {
		List<GhostDevice> list = new ArrayList<>(model.size());
		GhostDevice stored;
		for(int i= 0; i < model.size(); i++) {
			ShellyAbstractDevice dev = model.get(i);
			if(dev instanceof ShellyUnmanagedDeviceInterface == false || ((ShellyUnmanagedDeviceInterface)dev).getException() == null) {
				list.add(toGhost(dev));
			} else if ((stored = getStoredGhost(dev)) != null) {
				list.add(toGhost(stored));
			}
		}
		return list;
	}
	
	private static GhostDevice toGhost(ShellyAbstractDevice dev) {
		return new GhostDevice(
				dev.getAddressAndPort().getAddress(), dev.getAddressAndPort().getPort(), dev.getHostname(), dev.getMacAddress(),
				dev.getSSID(), dev.getTypeName(), dev.getTypeID(), gen(dev), dev.getName(), dev.getLastTime(),
				dev instanceof BatteryDeviceInterface || (dev instanceof GhostDevice g && g.isBatteryOperated()),
				"", "");
	}
	
	private GhostDevice getStoredGhost(ShellyAbstractDevice d) {
		int ind = ghostsList.indexOf(d);
		return ind >= 0 ? ghostsList.get(ind) : null;
	}
	
	private static String gen(ShellyAbstractDevice dev) {
		if(dev instanceof GhostDevice) {
			return ((GhostDevice) dev).getGeneration();
		} else if(dev instanceof AbstractG3Device) {
			return "3";
		} else if(dev instanceof AbstractG2Device) {
			return "2";
		} else if(dev instanceof AbstractG1Device) {
			return "1";
		} else if(dev instanceof BTHomeDevice) {
			return BTHomeDevice.GENERATION;
		} else if(dev instanceof AbstractBluDevice) {
			return AbstractBluDevice.GENERATION;
		} else{
			return "0";
		}
	}
	
	/**
	 * get corresponding dev ghost or generate a new one; the idea is to align model index with ghostsList index for better performances
	 * @param dev ShellyAbstractDevice
	 * @param modelIndex an hint; could be wrong
	 * @return the ghost corrisponding to d
	 */
	public synchronized GhostDevice getGhost(ShellyAbstractDevice dev, int modelIndex) {
		if(modelIndex < ghostsList.size()) {
			GhostDevice ghost;
			if(dev.equals(ghost = ghostsList.get(modelIndex))) { // ShellyAbstractDevice equals use only mac address
				return ghost;
			} else {
				int localInd = ghostsList.indexOf(dev);
				GhostDevice found;
				if(localInd >= 0) { // we have it but in the wrong index
					found = ghostsList.get(localInd);
					ghostsList.set(modelIndex, found);
					ghostsList.set(localInd, ghost);
				} else { // generate, put on the correct index, save old occupant 
					found = toGhost(dev);
					ghostsList.set(modelIndex, found);
					if(ghost != null) {
						ghostsList.add(ghost);
					}
				}
				return found;
			}
		} else {
			GhostDevice ghost = toGhost(dev);
			ghostsList.addAll(Collections.nCopies(modelIndex - ghostsList.size(), null)); // could add 0 elements
			ghostsList.add(ghost);
			return ghost;
		}
	}
}