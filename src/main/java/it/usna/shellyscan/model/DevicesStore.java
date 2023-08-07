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

	public static void store(Devices model, Path storeFile) {
		LOG.trace("storing archive");
		final JsonNodeFactory factory = new JsonNodeFactory(false);
		final ObjectNode root = factory.objectNode();
		root.put("ver", STORE_VERSION);
		root.put("time", System.currentTimeMillis());
		final ArrayNode array = factory.arrayNode();
		for (int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice dev = model.get(i);
			// do not include into store devices with errors
			if(dev instanceof ShellyUnmanagedDevice == false || ((ShellyUnmanagedDevice)dev).getException() == null) {
				ObjectNode jsonDev = factory.objectNode();
				jsonDev.put(TYPE_ID, dev.getTypeID());
				jsonDev.put(TYPE_NAME, dev.getTypeName());
				jsonDev.put(HOSTNAME, dev.getHostname());
				jsonDev.put(MAC, dev.getMacAddress());
				jsonDev.put(ADDRESS, dev.getAddress().getHostAddress());
				jsonDev.put(PORT, dev.getPort());
				jsonDev.put(NAME, dev.getName());
				jsonDev.put(SSID, dev.getSSID());
				jsonDev.put(LAST_CON, dev.getLastTime());
				array.add(jsonDev);
			}
		}
		root.set("dev", array);

		try (Writer w = Files.newBufferedWriter(storeFile, StandardCharsets.UTF_8)) {
			w.write(root.toString());
//			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
		} catch (IOException e) {
			LOG.error("Archive store", e);
		}
	}

	public static List<GhostDevice> read(Path storeFile) throws IOException {
		LOG.trace("reading archive");
		List<GhostDevice> list = new ArrayList<>();
		try (BufferedReader r = Files.newBufferedReader(storeFile, StandardCharsets.UTF_8)) {
			final JsonNode arc = JSON_MAPPER.readTree(r);
			if(arc.path("ver").asInt() == STORE_VERSION) {
				final ArrayNode array = (ArrayNode) arc.get("dev");
				array.forEach(el -> {
					try {
						list.add(new GhostDevice(
								InetAddress.getByName(el.get(ADDRESS).asText()), el.get(PORT).asInt(), el.get(HOSTNAME).asText(), el.get(MAC).asText(),
								el.get(SSID).asText(), el.get(TYPE_NAME).asText(), el.get(TYPE_ID).asText(), el.get(NAME).asText(), el.path(LAST_CON).asLong()));
					} catch (UnknownHostException | RuntimeException e) {
						LOG.error("Archive read", e);
					}
				});
			} else {
				LOG.info("Archive version is %; " + STORE_VERSION + " expected", arc.path("ver").asInt());
			}
		} catch(FileNotFoundException | NoSuchFileException e) {
			// first run?
		} catch (IOException e) {
			LOG.error("Archive read", e);
			throw e;
		}
		return list;
	}
	
	public static List<GhostDevice> toGhosts(Devices model) {
		List<GhostDevice> list = new ArrayList<>(model.size());
		for(int i= 0; i < model.size(); i++) {
			ShellyAbstractDevice dev = model.get(i);
			if(dev instanceof ShellyUnmanagedDevice == false || ((ShellyUnmanagedDevice)dev).getException() == null) {
				list.add(new GhostDevice(
						dev.getAddress(), dev.getPort(), dev.getHostname(), dev.getMacAddress(),
						dev.getSSID(), dev.getTypeName(), dev.getTypeID(), dev.getName(), dev.getLastTime()));
			}
		}
		return list;
	}
}