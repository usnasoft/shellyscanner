package it.usna.shellyscan.model.device.g2;

import java.util.function.Predicate;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebSocketDeviceListener implements Session.Listener.AutoDemanding {
	public static String NOTIFY_STATUS = "NotifyStatus";
	public static String NOTIFY_FULL_STATUS = "NotifyFullStatus";
	public static String NOTIFY_EVENT = "NotifyEvent";
	private Predicate<JsonNode> notifyCondition;
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
	
	private final static Logger LOG = LoggerFactory.getLogger(WebSocketDeviceListener.class);
	
	public WebSocketDeviceListener() {
	}
	
	public WebSocketDeviceListener(Predicate<JsonNode> condition) {
		this.notifyCondition = condition;
	}
	
//	@Override
//	public void onWebSocketConnect(Session session) {
//		LOG.trace("ws-open"); // session.getRemoteAddress()
//	}
	
    @Override
    public void onWebSocketOpen(org.eclipse.jetty.websocket.api.Session session) {
       LOG.trace("ws-open"); // session.getRemoteAddress()
    }

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		LOG.trace("sw-close: reason: {}, status: {}", reason, statusCode);
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		if(cause instanceof java.nio.channels.ClosedChannelException == false) {
			LOG.debug("ws-error", cause);
		}
	}

	@Override
	public void onWebSocketText(String message) {
		try {
			JsonNode msg = JSON_MAPPER.readTree(message);
			if(notifyCondition == null || notifyCondition.test(msg)) {
				onMessage(msg);
			}
		} catch (JsonProcessingException e) {
			LOG.warn("ws-message-error: {}", message, e);
		}
	}

//	@Override
//	public void onWebSocketBinary(byte[] payload, int offset, int length) {
//		LOG.trace("ws-binary; length: {}", length );
//	}
//	
	public void onMessage(JsonNode msg) {
		LOG.debug("M: {}", msg);
	}
	
	public void setNotifyCondition(Predicate<JsonNode> condition) {
		this.notifyCondition = condition;
	}
}