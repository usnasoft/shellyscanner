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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class DevicesStore {
	private final static Logger LOG = LoggerFactory.getLogger(DevicesStore.class);
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
		
		Path storeFile = Paths.get(System.getProperty("user.home"), "ShellyStore.arc");
		try (Writer w = Files.newBufferedWriter(storeFile, StandardCharsets.UTF_8)) {
			w.write(array.toString());
		} catch (IOException e) {
			LOG.error("Archive store", e); 
		}
//		try {
//			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(array));
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	public void read(Devices model) {
		Path storeFile = Paths.get(System.getProperty("user.home"), "ShellyStore.arc");
		try (BufferedReader inputStream = Files.newBufferedReader(storeFile)) {
			JsonNode arc = JSON_MAPPER.readTree(inputStream);
			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(arc));
		} catch (IOException e) {
			LOG.error("Archive read", e); 
		}
	}
}