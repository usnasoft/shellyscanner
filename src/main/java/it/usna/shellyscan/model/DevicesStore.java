package it.usna.shellyscan.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class DevicesStore {
	private final static Logger LOG = LoggerFactory.getLogger(DevicesStore.class);
	private final static int VERSION = 1;
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
		final JsonNodeFactory factory = new JsonNodeFactory(false);
		final ObjectNode root = factory.objectNode();
		root.put("ver", VERSION);
		root.put("time", System.currentTimeMillis());
		final ArrayNode array = factory.arrayNode();
		for(int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice dev = model.get(i);
			ObjectNode jsonDev = factory.objectNode();
			jsonDev.put(TYPE_ID, dev.getTypeID());
			jsonDev.put(TYPE_NAME, dev.getTypeName());
			jsonDev.put(HOSTNAME, dev.getHostname());
			jsonDev.put(MAC, dev.getMacAddress());
			jsonDev.put(ADDRESS, dev.getAddress().toString());
			jsonDev.put(PORT, dev.getPort());
			jsonDev.put(NAME, dev.getName());
			jsonDev.put(SSID, dev.getSSID());
			array.add(jsonDev);
		}
		root.set("dev", array);
		
		Path storeFile = Paths.get(System.getProperty("user.home"), "ShellyStore.arc");
		try (Writer w = Files.newBufferedWriter(storeFile, StandardCharsets.UTF_8)) {
			w.write(root.toString());
		} catch (IOException e) {
			LOG.error("Archive store", e); 
		}
		try {
			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void read(Devices model) {
		Path storeFile = Paths.get(System.getProperty("user.home"), "ShellyStore.arc");
		try (BufferedReader r = Files.newBufferedReader(storeFile, StandardCharsets.UTF_8)) {
			final JsonNode arc = JSON_MAPPER.readTree(r);
			final ArrayNode array = (ArrayNode)arc.get("dev");
			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(array));
		} catch (IOException e) {
			LOG.error("Archive read", e); 
		}
	}
}