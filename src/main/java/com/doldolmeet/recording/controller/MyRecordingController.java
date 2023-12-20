package com.doldolmeet.recording.controller;

import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.recording.entity.RecordingInfo;
import com.doldolmeet.recording.service.RecordingInfoService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Slf4j
@Component
@RequestMapping("/recording-java/api")
@RequiredArgsConstructor
public class MyRecordingController {

	// OpenVidu object as entrypoint of the SDK

	private final RecordingInfoService recordingInfoService;
	private OpenVidu openVidu = new OpenVidu("https://youngeui-in-jungle.store/", "MY_SECRET");
	// Collection to pair session names and OpenVidu Session objects
	private Map<String, Session> mapSessions = new ConcurrentHashMap<>();

	// Collection to pair session names and tokens (the inner Map pairs tokens and
	// role associated)
	private Map<String, Map<String, OpenViduRole>> mapSessionNamesTokens = new ConcurrentHashMap<>();

	// Collection to pair session names and recording objects
	public static Map<String, Boolean> sessionRecordings = new ConcurrentHashMap<>();
	public static Map<String, Recording> sessionIdRecordingsMap = new ConcurrentHashMap<>();

//	public static Map<List<String>, String> recordingInfo = new ConcurrentHashMap<>();
	public static Map<String, Map<String, String>> recordingInfo = new ConcurrentHashMap<>();
	;


	/*******************/
	/*** Session API ***/
	/*******************/

	@RequestMapping(value = "/get-token", method = RequestMethod.POST)
	public ResponseEntity<JsonObject> getToken(@RequestBody Map<String, Object> sessionNameParam) {

		log.info("Getting sessionId and token | {sessionName}=" + sessionNameParam);

		// The video-call to connect ("TUTORIAL")
		String sessionName = (String) sessionNameParam.get("sessionName");

		// Role associated to this user
		OpenViduRole role = OpenViduRole.PUBLISHER;

		// Build connectionProperties object with the serverData and the role
		ConnectionProperties connectionProperties = new ConnectionProperties.Builder().type(ConnectionType.WEBRTC)
				.role(role).data("user_data").build();

		JsonObject responseJson = new JsonObject();

		if (this.mapSessions.get(sessionName) != null) {
			// Session already exists
			log.info("Existing session " + sessionName);
			try {

				// Generate a new token with the recently created connectionProperties
				String token = this.mapSessions.get(sessionName).createConnection(connectionProperties).getToken();

				// Update our collection storing the new token
				this.mapSessionNamesTokens.get(sessionName).put(token, role);

				// Prepare the response with the token
				responseJson.addProperty("0", token);

				// Return the response to the client
				return new ResponseEntity<>(responseJson, HttpStatus.OK);

			} catch (OpenViduJavaClientException e1) {
				// If internal error generate an error message and return it to client
				return getErrorResponse(e1);
			} catch (OpenViduHttpException e2) {
				if (404 == e2.getStatus()) {
					// Invalid sessionId (user left unexpectedly). Session object is not valid
					// anymore. Clean collections and continue as new session
					this.mapSessions.remove(sessionName);
					this.mapSessionNamesTokens.remove(sessionName);
				}
			}
		}

		// New session
		log.info("New session " + sessionName);
		try {

			// Create a new OpenVidu Session
			Session session = this.openVidu.createSession();
			// Generate a new token with the recently created connectionProperties
			String token = session.createConnection(connectionProperties).getToken();

			// Store the session and the token in our collections
			this.mapSessions.put(sessionName, session);
			this.mapSessionNamesTokens.put(sessionName, new ConcurrentHashMap<>());
			this.mapSessionNamesTokens.get(sessionName).put(token, role);

			// Prepare the response with the sessionId and the token
			responseJson.addProperty("0", token);

			// Return the response to the client
			return new ResponseEntity<>(responseJson, HttpStatus.OK);

		} catch (Exception e) {
			// If error generate an error message and return it to client
			return getErrorResponse(e);
		}
	}

	@RequestMapping(value = "/remove-user", method = RequestMethod.POST)
	public ResponseEntity<JsonObject> removeUser(@RequestBody Map<String, Object> sessionNameToken) throws Exception {

		log.info("Removing user | {sessionName, token}=" + sessionNameToken);

		// Retrieve the params from BODY
		String sessionName = (String) sessionNameToken.get("sessionName");
		String token = (String) sessionNameToken.get("token");

		// If the session exists
		if (this.mapSessions.get(sessionName) != null && this.mapSessionNamesTokens.get(sessionName) != null) {

			// If the token exists
			if (this.mapSessionNamesTokens.get(sessionName).remove(token) != null) {
				// User left the session
				if (this.mapSessionNamesTokens.get(sessionName).isEmpty()) {
					// Last user left: session must be removed
					this.mapSessions.remove(sessionName);
				}
				return new ResponseEntity<>(HttpStatus.OK);
			} else {
				// The TOKEN wasn't valid
				log.info("Problems in the app server: the TOKEN wasn't valid");
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} else {
			// The SESSION does not exist
			log.info("Problems in the app server: the SESSION does not exist");
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/close-session", method = RequestMethod.DELETE)
	public ResponseEntity<JsonObject> closeSession(@RequestBody Map<String, Object> sessionName) throws Exception {

		log.info("Closing session | {sessionName}=" + sessionName);

		// Retrieve the param from BODY
		String session = (String) sessionName.get("sessionName");

		// If the session exists
		if (this.mapSessions.get(session) != null && this.mapSessionNamesTokens.get(session) != null) {
			Session s = this.mapSessions.get(session);
			s.close();
			this.mapSessions.remove(session);
			this.mapSessionNamesTokens.remove(session);
			this.sessionRecordings.remove(s.getSessionId());
			return new ResponseEntity<>(HttpStatus.OK);
		} else {
			// The SESSION does not exist
			log.info("Problems in the app server: the SESSION does not exist");
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/fetch-info", method = RequestMethod.POST)
	public ResponseEntity<JsonObject> fetchInfo(@RequestBody Map<String, Object> sessionName) {
		try {
			log.info("Fetching session info | {sessionName}=" + sessionName);

			// Retrieve the param from BODY
			String session = (String) sessionName.get("sessionName");

			// If the session exists
			if (this.mapSessions.get(session) != null && this.mapSessionNamesTokens.get(session) != null) {
				Session s = this.mapSessions.get(session);
				boolean changed = s.fetch();
				log.info("Any change: " + changed);
				return new ResponseEntity<>(this.sessionToJson(s), HttpStatus.OK);
			} else {
				// The SESSION does not exist
				log.info("Problems in the app server: the SESSION does not exist");
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			e.printStackTrace();
			return getErrorResponse(e);
		}
	}

	@RequestMapping(value = "/fetch-all", method = RequestMethod.GET)
	public ResponseEntity<?> fetchAll() {
		try {
			log.info("Fetching all session info");
			boolean changed = this.openVidu.fetch();
			log.info("Any change: " + changed);
			JsonArray jsonArray = new JsonArray();
			for (Session s : this.openVidu.getActiveSessions()) {
				jsonArray.add(this.sessionToJson(s));
			}
			return new ResponseEntity<>(jsonArray, HttpStatus.OK);
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			e.printStackTrace();
			return getErrorResponse(e);
		}
	}

	@RequestMapping(value = "/force-disconnect", method = RequestMethod.DELETE)
	public ResponseEntity<JsonObject> forceDisconnect(@RequestBody Map<String, Object> params) {
		try {
			// Retrieve the param from BODY
			String session = (String) params.get("sessionName");
			String connectionId = (String) params.get("connectionId");

			// If the session exists
			if (this.mapSessions.get(session) != null && this.mapSessionNamesTokens.get(session) != null) {
				Session s = this.mapSessions.get(session);
				s.forceDisconnect(connectionId);
				return new ResponseEntity<>(HttpStatus.OK);
			} else {
				// The SESSION does not exist
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			e.printStackTrace();
			return getErrorResponse(e);
		}
	}

	@RequestMapping(value = "/force-unpublish", method = RequestMethod.DELETE)
	public ResponseEntity<JsonObject> forceUnpublish(@RequestBody Map<String, Object> params) {
		try {
			// Retrieve the param from BODY
			String session = (String) params.get("sessionName");
			String streamId = (String) params.get("streamId");

			// If the session exists
			if (this.mapSessions.get(session) != null && this.mapSessionNamesTokens.get(session) != null) {
				Session s = this.mapSessions.get(session);
				s.forceUnpublish(streamId);
				return new ResponseEntity<>(HttpStatus.OK);
			} else {
				// The SESSION does not exist
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			e.printStackTrace();
			return getErrorResponse(e);
		}
	}

	/*******************/
	/** Recording API **/
	/*******************/

	@RequestMapping(value = "/recording/start", method = RequestMethod.POST)
	public ResponseEntity<?> startRecording(@RequestBody Map<String, Object> params) {
		log.info("Starting recording | {session, outputMode, hasAudio, hasVideo}=" + params);
		String sessionId = (String) params.get("session");
		Recording.OutputMode outputMode = Recording.OutputMode.valueOf((String) params.get("outputMode"));
		boolean hasAudio = (boolean) params.get("hasAudio");
		boolean hasVideo = (boolean) params.get("hasVideo");
		String fanMeetingIdStr = params.getOrDefault("fanMeetingId", "1").toString();
		Long fanMeetingId = Long.valueOf(fanMeetingIdStr.equals("undefined") ? "1" : fanMeetingIdStr);
		String fan =(String) params.get("fan");
		String fileName = (String) params.get("name");
		String idol = (String) params.get("idol");

		RecordingProperties properties = new RecordingProperties.Builder().outputMode(outputMode).hasAudio(hasAudio)
				.hasVideo(hasVideo).name(fileName).build();

		log.info("Starting recording for session " + sessionId + " with properties {outputMode=" + outputMode
				+ ", hasAudio=" + hasAudio + ", hasVideo=" + hasVideo + "fan=" + fan + "idol=" + idol, "fileName=" + fileName + "}"	);

//		onApplicationStart();

		for (int retryCount = 0; retryCount < 2; retryCount++) {
			try {
				openVidu.fetch();
				Recording recording = this.openVidu.startRecording(sessionId, properties);
				log.info("레코딩 정보: " + recording);
				HashMap<String, String> sessionIdMap = new HashMap<>();
				sessionIdMap.put("sessionId", sessionId);
				this.sessionRecordings.put(sessionId, true);
				this.sessionIdRecordingsMap.put(sessionId, recording);
				recordingInfoService.saveRecordingInfo(fanMeetingId, fan, idol, fileName, recording.getId());
				log.info("여기까지 오면 레코딩 정보 저장 완료");
				return new ResponseEntity<>(recording, HttpStatus.OK);
			} catch (OpenViduJavaClientException | OpenViduHttpException e) {
				if (retryCount < 1) {
					log.warn("레코딩 시작 시도 중 오류 발생. 재시도 중... (재시도 횟수: " + (retryCount + 1) + ")");
					// 예외 발생 시 잠시 대기하고 다시 시도
					try {
						Thread.sleep(1000); // 1초 대기
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				} else {
					log.error("레코딩 시작 중 오류 발생 (최대 재시도 횟수 초과)", e);
					return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
				}
			}
		}

		return new ResponseEntity<>("레코딩 시작 중 오류 발생 (최대 재시도 횟수 초과)", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/recording/stop", method = RequestMethod.POST)
	public ResponseEntity<?> stopRecording(@RequestBody Map<String, Object> params) {
		String recordingId = (String) params.get("recording");

		log.info("Stoping recording | {recordingId}=" + recordingId);

		try {
			Recording recording = this.openVidu.stopRecording(recordingId);
			this.sessionRecordings.remove(recording.getSessionId());
			return new ResponseEntity<>(recording, HttpStatus.OK);
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}

	}

	@RequestMapping(value = "/recording/get", method = RequestMethod.POST)
	public ResponseEntity<?> getRecording(@RequestBody Map<String, Object> params) {
		String fanMeetingIdStr = params.getOrDefault("fanMeetingId", "1").toString();
		Long fanMeetingId = Long.valueOf(fanMeetingIdStr.equals("undefined") ? "1" : fanMeetingIdStr);
		String fan =(String) params.get("fan");
		String idol = (String) params.get("idol");


		try {
			String recordingId = recordingInfoService.findRecordingId(fanMeetingId, fan, idol);
			Recording recording = this.openVidu.getRecording(recordingId);
			return new ResponseEntity<>(recording, HttpStatus.OK);
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/recordings/get", method = RequestMethod.POST)
	public ResponseEntity<?> getRecordings(@RequestBody Map<String, Object> params) {
		String fanMeetingIdStr = params.getOrDefault("fanMeetingId", "1").toString();
		Long fanMeetingId = Long.valueOf(fanMeetingIdStr.equals("undefined") ? "1" : fanMeetingIdStr);
		String fan =(String) params.get("fan");

		try {
			List<RecordingInfo> recordingInfos = recordingInfoService.findRecordingInfos(fanMeetingId, fan);
			Map<String, Recording> recordings = new HashMap<>();
			for (RecordingInfo recordingInfo : recordingInfos){
				String idolNickname = recordingInfo.getIdol().getUserCommons().getNickname();
				Recording recording = this.openVidu.getRecording(recordingInfo.getRecordingId());
				recordings.put(idolNickname,recording);
			}
			return new ResponseEntity<>(recordings, HttpStatus.OK);
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/recording/delete", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteRecording(@RequestBody Map<String, Object> params) {
		String recordingId = (String) params.get("recording");

		log.info("Deleting recording | {recordingId}=" + recordingId);

		try {
			this.openVidu.deleteRecording(recordingId);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}



//	@RequestMapping(value = "/recording/get/{recordingId}", method = RequestMethod.GET)
//	public ResponseEntity<?> getRecording(@PathVariable(value = "recordingId") String recordingId) {
//
//		log.info("Getting recording | {recordingId}=" + recordingId);
//
//		try {
//			Recording recording = this.openVidu.getRecording(recordingId);
//			return new ResponseEntity<>(recording, HttpStatus.OK);
//		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
//			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
//		}
//	}

	@RequestMapping(value = "/recording/list", method = RequestMethod.GET)
	public ResponseEntity<?> listRecordings() {

		log.info("Listing recordings");

		try {
			List<Recording> recordings = this.openVidu.listRecordings();

			return new ResponseEntity<>(recordings, HttpStatus.OK);
		} catch (OpenViduJavaClientException | OpenViduHttpException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	private ResponseEntity<JsonObject> getErrorResponse(Exception e) {
		JsonObject json = new JsonObject();
		json.addProperty("cause", e.getCause().toString());
		json.addProperty("error", e.getMessage());
		json.addProperty("exception", e.getClass().getCanonicalName());
		return new ResponseEntity<>(json, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	protected JsonObject sessionToJson(Session session) {
		Gson gson = new Gson();
		JsonObject json = new JsonObject();
		json.addProperty("sessionId", session.getSessionId());
		json.addProperty("customSessionId", session.getProperties().customSessionId());
		json.addProperty("recording", session.isBeingRecorded());
		json.addProperty("mediaMode", session.getProperties().mediaMode().name());
		json.addProperty("recordingMode", session.getProperties().recordingMode().name());
		json.add("defaultRecordingProperties",
				gson.toJsonTree(session.getProperties().defaultRecordingProperties()).getAsJsonObject());
		JsonObject connections = new JsonObject();
		connections.addProperty("numberOfElements", session.getConnections().size());
		JsonArray jsonArrayConnections = new JsonArray();
		session.getConnections().forEach(con -> {
			JsonObject c = new JsonObject();
			c.addProperty("connectionId", con.getConnectionId());
			c.addProperty("role", con.getRole().name());
			c.addProperty("token", con.getToken());
			c.addProperty("clientData", con.getClientData());
			c.addProperty("serverData", con.getServerData());
			JsonArray pubs = new JsonArray();
			con.getPublishers().forEach(p -> {
				pubs.add(gson.toJsonTree(p).getAsJsonObject());
			});
			JsonArray subs = new JsonArray();
			con.getSubscribers().forEach(s -> {
				subs.add(s);
			});
			c.add("publishers", pubs);
			c.add("subscribers", subs);
			jsonArrayConnections.add(c);
		});
		connections.add("content", jsonArrayConnections);
		json.add("connections", connections);
		return json;
	}

}
