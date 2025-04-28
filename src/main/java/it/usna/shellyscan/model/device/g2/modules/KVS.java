package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class KVS {
	private final AbstractG2Device device;
	private final ArrayList<KVItem> kvItems = new ArrayList<>();
	
	public KVS(AbstractG2Device device) throws IOException {
		this.device = device;
		refresh();
	}

	public void refresh() throws IOException {
		kvItems.clear();
		JsonNode kvsItems = device.getJSON("/rpc/KVS.GetMany").path("items"); // wall display: {"code":-114,"message":"Method KVS.GetMany failed: No such component"}
		if(kvsItems.isArray()) { // fw >= 1.5.0
			for(JsonNode item: kvsItems) {
				kvItems.add(new KVItem(item.get("key").asText(), item.get("etag").asText(), item.get("value").asText()));
			}
		} else { // fw < 1.5.0
			for(Entry<String, JsonNode> entry: kvsItems.properties()) {
				kvItems.add(new KVItem(entry.getKey(), entry.getValue().get("etag").asText(), entry.getValue().get("value").asText()));
			}
		}
	}
	
	public List<KVItem> getItems() {
		return kvItems;
	}
	
	public KVItem get(int index) {
		return kvItems.get(index);
	}
	
	public void delete(int index) throws IOException {
		device.getJSON("/rpc/KVS.Delete?key=" + URLEncoder.encode(kvItems.get(index).key, StandardCharsets.UTF_8.name()));
		kvItems.remove(index);
	}
	
	public KVItem edit(int index, String value) throws IOException {
		ObjectNode pars = JsonNodeFactory.instance.objectNode();
		String key = kvItems.get(index).key;
		pars.put("key", key);
		pars.put("value", value);
		JsonNode node = device.getJSON("KVS.Set", pars);
		return kvItems.set(index, new KVItem(key, node.get("etag").asText(), value));
	}
	
	public KVItem add(String key, String value) throws IOException {
		ObjectNode pars = JsonNodeFactory.instance.objectNode();
		pars.put("key", key);
		pars.put("value", value);
		JsonNode node = device.getJSON("KVS.Set", pars);
		KVItem item = new KVItem(key, node.get("etag").asText(), value);
		kvItems.add(item);
		return item;
	}
	
	public int size() {
		return kvItems.size();
	}
	
	public int getIndex(String key) {
		for(int i = 0; i < kvItems.size(); i++) {
			if(kvItems.get(i).key.equals(key)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Restore missing or modified values; call refresh() at the end if KVS must still be used
	 * @param kvsMany
	 * @param errors
	 * @throws InterruptedException
	 */
	public void restoreKVS(JsonNode kvsMany, List<String> errors) throws InterruptedException {
		JsonNode kvsItems = kvsMany.path("items");
		if(kvsItems.isArray()) { // fw >= 1.5.0
			for(JsonNode item: kvsItems) {
				KVItem storedItem = new KVItem(item.get("key").asText(), item.get("etag").asText(), item.get("value").asText());
				if(kvItems.contains(storedItem) == false) {
					ObjectNode out = JsonNodeFactory.instance.objectNode();
					out.put("key", storedItem.key);
					out.put("value", storedItem.value);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(device.postCommand("KVS.Set", out));
				}
			}
		} else { // fw < 1.5.0
			for(Entry<String, JsonNode> entry: kvsItems.properties()) {
				KVItem storedItem = new KVItem(entry.getKey(), entry.getValue().get("etag").asText(), entry.getValue().get("value").asText());
				if(kvItems.contains(storedItem) == false) {
					ObjectNode out = JsonNodeFactory.instance.objectNode();
					out.put("key", storedItem.key);
					out.put("value", storedItem.value);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(device.postCommand("KVS.Set", out));
				} 
			}
		}
	}
	
	public record KVItem(String key, String etag, String value) {}
}