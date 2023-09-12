package it.usna.shellyscan.view.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class UtilMiscellaneous {
	private UtilMiscellaneous() {}
	
	public static String getDescName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		return (dName != null && dName.length() > 0 ? dName : d.getHostname());
	}
	
	public static String getDescName(ShellyAbstractDevice d, int channel) {
		if(d instanceof RelayCommander) {
			RelayInterface[] ri = ((RelayCommander)d).getRelays();
			String name;
			RelayInterface rel;
			if(ri != null && ri.length > channel && (rel = ri[channel]) != null && (name = rel.getName()) != null && name.length() > 0) {
				final String dName = d.getName();
				return (dName != null && dName.length() > 0) ? dName + "-" + name : name;
			}
		}
		return channel == 0 ? getDescName(d) : getDescName(d) +  "-" + (channel + 1);
	}
	
	public static String getFullName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		if(dName.length() > 0) {
			return dName + "-" + d.getHostname() + "-" + d.getTypeName();
		} else {
			return d.getHostname() + "-" + d.getTypeName();
		}
	}
	
	public static String getExtendedHostName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		return d.getHostname() + " - " + (dName != null && dName.length() > 0 ? dName : d.getTypeName());
	}
	
	public static Map<String, JsonNode> readBackupFile(final File file) throws IOException {
		final ObjectMapper jsonMapper = new ObjectMapper();
		try (ZipFile in = new ZipFile(file, StandardCharsets.UTF_8)) {
			final Map<String, JsonNode> backupJsons = in.stream().filter(entry -> entry.getName().endsWith(".json")).collect(Collectors.toMap(ZipEntry::getName, entry -> {
				try (InputStream is = in.getInputStream(entry)) {
					return jsonMapper.readTree(is);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}));
			return backupJsons;
		} catch(RuntimeException e) {
			if(e.getCause() instanceof IOException) {
				throw (IOException)e.getCause();
			} 
			throw e;
		}
	}
}