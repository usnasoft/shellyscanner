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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.ShellyUnmanagedDevice;

public class DevicesStore {
	private final static Logger LOG = LoggerFactory.getLogger(DevicesStore.class);
	private final static int STORE_VERSION = 0;
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
	private final static String TYPE_ID = "tid";
	private final static String TYPE_NAME = "tn";
	private final static String NAME = "name";
	private final static String HOSTNAME = "host";
	private final static String MAC = "mac";
	private final static String ADDRESS = "ip";
	private final static String PORT = "port";
	private final static String SSID = "ssid";
	private final static String LAST_CON = "last";
	private final static String USER_NOTE = "note";
	
	private final static JsonNodeFactory J_FACTORY = new JsonNodeFactory(false);
	private List<GhostDevice> ghostsList = new ArrayList<>();

	public void store(Devices model, Path storeFile) throws IOException {
		LOG.trace("storing archive");
		final ObjectNode root = J_FACTORY.objectNode();
		root.put("ver", STORE_VERSION);
		root.put("time", System.currentTimeMillis());
		final ArrayNode array = J_FACTORY.arrayNode();
		for (int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice device = model.get(i);
			GhostDevice stored = getStoredGhost(device);
			// Device with errors or not authenticated -> get information from old store
			if((device instanceof ShellyUnmanagedDevice ud && ud.getException() != null) || device.getStatus() == Status.NOT_LOOGGED) {
				if(stored != null) {
					ObjectNode jsonDev = toJson(stored);
					jsonDev.put(USER_NOTE, stored.getNote());
					jsonDev.put(ADDRESS, device.getAddress().getHostAddress());
					array.add(jsonDev);
				} else {
					array.add(toJson(device));
				}
			} else {
				ObjectNode jsonDev = toJson(device);
				if(stored != null) {
					jsonDev.put(USER_NOTE, stored.getNote());
				}
				array.add(jsonDev);
			}
		}
		root.set("dev", array);

		try (Writer w = Files.newBufferedWriter(storeFile, StandardCharsets.UTF_8)) {
			w.write(root.toString());
//			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
		}
	}
	
	private static ObjectNode toJson(ShellyAbstractDevice device) {
		ObjectNode jsonDev = J_FACTORY.objectNode();
		jsonDev.put(TYPE_ID, device.getTypeID());
		jsonDev.put(TYPE_NAME, device.getTypeName());
		jsonDev.put(HOSTNAME, device.getHostname());
		jsonDev.put(MAC, device.getMacAddress());
		jsonDev.put(ADDRESS, device.getAddress().getHostAddress());
		jsonDev.put(PORT, device.getPort());
		jsonDev.put(NAME, device.getName());
		jsonDev.put(SSID, device.getSSID());
		jsonDev.put(LAST_CON, device.getLastTime());
		return jsonDev;
	}

	/**
	 * Read a store file
	 * @param storeFile
	 * @return ghosts list 
	 * @throws IOException
	 */
	public List<GhostDevice> read(Path storeFile) throws IOException {
		LOG.trace("reading archive");
		ghostsList.clear();
		try (BufferedReader r = Files.newBufferedReader(storeFile, StandardCharsets.UTF_8)) {
			final JsonNode arc = JSON_MAPPER.readTree(r);
			if(arc.path("ver").asInt() == STORE_VERSION) {
				final ArrayNode array = (ArrayNode) arc.get("dev");
				array.forEach(el -> {
					try {
						ghostsList.add(new GhostDevice(
								InetAddress.getByName(el.get(ADDRESS).asText()), el.get(PORT).asInt(), el.get(HOSTNAME).asText(), el.get(MAC).asText(),
								el.get(SSID).asText(), el.get(TYPE_NAME).asText(), el.get(TYPE_ID).asText(), el.get(NAME).asText(), el.path(LAST_CON).asLong(),
								el.path(USER_NOTE).asText()));
					} catch (UnknownHostException | RuntimeException e) {
						LOG.error("Archive read", e);
					}
				});
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
	 * Map the full model to a list of ghosts
	 */
	public static List<GhostDevice> toGhosts(Devices model) {
		List<GhostDevice> list = new ArrayList<>(model.size());
		for(int i= 0; i < model.size(); i++) {
			ShellyAbstractDevice dev = model.get(i);
			if(dev instanceof ShellyUnmanagedDevice == false || ((ShellyUnmanagedDevice)dev).getException() == null) {
				list.add(toGhost(dev));
			}
		}
		return list;
	}
	
	private static GhostDevice toGhost(ShellyAbstractDevice dev) {
		return new GhostDevice(
				dev.getAddress(), dev.getPort(), dev.getHostname(), dev.getMacAddress(),
				dev.getSSID(), dev.getTypeName(), dev.getTypeID(), dev.getName(), dev.getLastTime(), "");
	}
	
	private GhostDevice getStoredGhost(ShellyAbstractDevice d) {
		int ind = ghostsList.indexOf(d);
		return ind >= 0 ? ghostsList.get(ind) : null;
	}
	
	public GhostDevice getGhost(ShellyAbstractDevice d) {
		int ind = ghostsList.indexOf(d);
		if(ind >= 0) {
			return ghostsList.get(ind);
		} else {
			GhostDevice ghost = toGhost(d);
			ghostsList.add(ghost);
			return ghost;
		}
	}
	
//	public String getNote(ShellyAbstractDevice d) {
//		int ind = ghostsList.indexOf(d);
//		return ind >= 0 ? ghostsList.get(ind).getNote() : "";
//	}
//	
//	public void setNote(ShellyAbstractDevice d, String note) {
//		int ind = ghostsList.indexOf(d);
//		if(ind >= 0) {
//			ghostsList.get(ind).setNote(note);
//		} else {
//			GhostDevice ghost = toGhost(d);
//			ghostsList.add(ghost);
//			ghostsList.get(ind).setNote(note);
//		}
//	}
}