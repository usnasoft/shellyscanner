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
import java.nio.file.Paths;
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

public class DevicesStore {
	private final static Logger LOG = LoggerFactory.getLogger(DevicesStore.class);
	private final static int FORMAT_VERSION = 0;
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
	private final static String TYPE_ID = "tid";
	private final static String TYPE_NAME = "tn";
	private final static String NAME = "name";
	private final static String HOSTNAME = "host";
	private final static String MAC = "mac";
	private final static String ADDRESS = "ip";
	private final static String PORT = "port";
	private final static String SSID = "ssid";

	public static void store(Devices model) {
		LOG.trace("storing archive");
		final JsonNodeFactory factory = new JsonNodeFactory(false);
		final ObjectNode root = factory.objectNode();
		root.put("ver", FORMAT_VERSION);
		root.put("time", System.currentTimeMillis());
		final ArrayNode array = factory.arrayNode();
		for (int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice dev = model.get(i);
			ObjectNode jsonDev = factory.objectNode();
			jsonDev.put(TYPE_ID, dev.getTypeID());
			jsonDev.put(TYPE_NAME, dev.getTypeName());
			jsonDev.put(HOSTNAME, dev.getHostname());
			jsonDev.put(MAC, dev.getMacAddress());
			jsonDev.put(ADDRESS, dev.getAddress().getHostAddress());
			jsonDev.put(PORT, dev.getPort());
			jsonDev.put(NAME, dev.getName());
			jsonDev.put(SSID, dev.getSSID());
			array.add(jsonDev);
		}
		root.set("dev", array);

		Path storeFile = Paths.get(System.getProperty("user.home"), "ShellyStore.arc");
		try (Writer w = Files.newBufferedWriter(storeFile, StandardCharsets.UTF_8)) {
			w.write(root.toString());
//			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
		} catch (IOException e) {
			LOG.error("Archive store", e);
		}
	}

	public static List<GhostDevice> read() {
		LOG.trace("reading archive");
		List<GhostDevice> list = new ArrayList<>();
		Path storeFile = Paths.get(System.getProperty("user.home"), "ShellyStore.arc");
		try (BufferedReader r = Files.newBufferedReader(storeFile, StandardCharsets.UTF_8)) {
			final JsonNode arc = JSON_MAPPER.readTree(r);
			if(arc.path("ver").asInt() == FORMAT_VERSION) {
				final ArrayNode array = (ArrayNode) arc.get("dev");
				array.forEach(el -> {
					try {
						list.add(new GhostDevice(
								InetAddress.getByName(el.get(ADDRESS).asText()), el.get(PORT).asInt(), el.get(HOSTNAME).asText(), el.get(MAC).asText(),
								el.get(SSID).asText(), el.get(TYPE_NAME).asText(), el.get(TYPE_ID).asText(), el.get(NAME).asText()));
					} catch (UnknownHostException | RuntimeException e) {
						LOG.error("Archive read", e);
					}
				});
			} else {
				LOG.info("Archive version is %; " + FORMAT_VERSION + " expected", arc.path("ver").asInt());
			}
		} catch(FileNotFoundException | NoSuchFileException e) {
			// first run?
		} catch (IOException e) {
			LOG.error("Archive read", e);
		}
		return list;
	}
	
	public static List<GhostDevice> toGhosts(Devices model) {
		List<GhostDevice> list = new ArrayList<>(model.size());
		for(int i= 0; i < model.size(); i++) {
			ShellyAbstractDevice d = model.get(i);
			list.add(new GhostDevice(
					d.getAddress(), d.getPort(), d.getHostname(), d.getMacAddress(),
					d.getSSID(), d.getTypeName(), d.getTypeID(), d.getName()));
		}
		return list;
	}
}