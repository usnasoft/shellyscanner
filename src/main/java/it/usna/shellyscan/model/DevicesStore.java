package it.usna.shellyscan.model;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class DevicesStore {
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
	private final static String TYPE_ID = "tid";
	private final static String TYPE_NAME = "tn";
	private final static String HOSTNAME = "host";
	private final static String MAC = "mac";
	private final static String ADDRESS = "ip";
	private final static String PORT = "port";
	
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
			array.add(jsonDev);
		}
		
		Path storeFile = Paths.get(System.getProperty("user.home"), "ShellyStore.arc");
		try (Writer w = Files.newBufferedWriter(storeFile, StandardCharsets.UTF_8)) {
			w.write(array.toString());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(array));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}