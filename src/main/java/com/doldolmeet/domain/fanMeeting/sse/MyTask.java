package com.doldolmeet.domain.fanMeeting.sse;

import com.doldolmeet.domain.fanMeeting.entity.FanMeetingRoomOrder;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRoomOrderRepository;
import com.doldolmeet.domain.openvidu.service.OpenviduService;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.exception.CustomException;
import com.doldolmeet.recording.controller.MyRecordingController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.openvidu.java.client.OpenVidu;

import static com.doldolmeet.exception.ErrorCode.*;

@Slf4j
public class MyTask implements Runnable {
    private String body;
    private OpenviduService openviduService;

    private ObjectMapper objectMapper;
    private FanMeetingRoomOrderRepository fanMeetingRoomOrderRepository;
    private OpenVidu openvidu;
    private IdolRepository idolRepository;
    private SseService sseService;

    public MyTask(String body, OpenviduService openviduService, ObjectMapper objectMapper, FanMeetingRoomOrderRepository fanMeetingRoomOrderRepository, OpenVidu openvidu, IdolRepository idolRepository, SseService sseService) {
        this.body = body;
        this.openviduService = openviduService;
        this.objectMapper = objectMapper;
        this.fanMeetingRoomOrderRepository = fanMeetingRoomOrderRepository;
        this.openvidu = openvidu;
        this.idolRepository = idolRepository;
        this.sseService = sseService;
    }

    @Override
    public void run() {
        long timeLimit = 50000;
        long endNotice = 10000;

        try {
            // 게임 시작 전까지 자기
            Thread.sleep(timeLimit - endNotice); // 40초 대화
            log.info("---------아이돌방 쓰레드 안꺼지고 계속 도는 중: {}", body);

        } catch (InterruptedException e) {
            throw new CustomException(THREAD_INTERRUPTED);
        }

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        log.info("jsonNode : " + jsonNode);

        String sessionId = jsonNode.get("sessionId").asText();
        String connectionId = jsonNode.get("connectionId").asText();

        String username = parseUsername(body);
        Long fanMeetingId = parseFanMeetingId(body);
        String idolName = parseIdolName(body);

        Idol idol = idolRepository.findByUserCommonsNickname(idolName).orElseThrow(() -> new CustomException(IDOL_NOT_FOUND));

        SseEmitter fanEmitter;
        if (SseService.emitters.get(fanMeetingId).get(username) == null) {
            log.info("--------- Fan의 emitter가 null임, 아이돌방: {}", idol.getUserCommons().getNickname());
        } else {
            fanEmitter = SseService.emitters.get(fanMeetingId).get(username);
            log.info("fanEmitter : " + fanEmitter);

            // 종료 알림을 보내기
            sseService.sendEvent(fanMeetingId, username, "endNotice", new HashMap<>());
        }

        SseEmitter idolEmitter;
        if (SseService.emitters.get(fanMeetingId).get(idol.getUserCommons().getUsername()) == null) {
            log.info("--------- 아이돌의 emitter가 null임, 아이돌방: {}", idol.getUserCommons().getNickname());
        } else {
            idolEmitter = SseService.emitters.get(fanMeetingId).get(idol.getUserCommons().getUsername());
            log.info("idolEmitter : " + idolEmitter);
            // 종료 알림을 보내기
            sseService.sendEvent(fanMeetingId, idol.getUserCommons().getUsername(), "idolEndNotice", new HashMap<>());
        }

        try {
            Thread.sleep(endNotice); // 종료알림 보내고 10초 후 끝
        } catch (InterruptedException e) {
            throw new CustomException(THREAD_INTERRUPTED);
        }

        log.info("-------종료되는 connectionId: " + connectionId);
        Session session = null;
        try {
            session = openviduService.getSession(sessionId);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException("--------- 세션 조회 실패", e);
        }
        String recordingId = MyRecordingController.sessionIdRecordingsMap.get(sessionId).getId();

        try {
            this.openvidu.stopRecording(recordingId);
        } catch (OpenViduHttpException | OpenViduJavaClientException e) {
            log.error("--------- 녹화 종료 실패", e);
//            throw new RuntimeException(e);
        }

        MyRecordingController.sessionRecordings.remove(sessionId);
        // 연결 끊기

        try {
            session.forceDisconnect(connectionId);
            log.info("--------- 연결 끊기 성공, 커넥션ID: {}", connectionId);
        } catch (Exception e) {
            log.info("--------- 연결 끊기 실패, 커넥션ID: {}", connectionId);
            log.error("--------- 연결 끊기 실패, 에러메시지: {}", e.getMessage());
        }

        Optional<FanMeetingRoomOrder> currFanMeetingRoomOrderOpt = fanMeetingRoomOrderRepository.findByFanMeetingIdAndCurrentRoom(fanMeetingId, sessionId);
        // 없으면 예외
        if (currFanMeetingRoomOrderOpt.isEmpty()) {
            throw new CustomException(NOT_FOUND_FANMEETING_ROOM_ORDER);
        }
        FanMeetingRoomOrder currRoomOrder = currFanMeetingRoomOrderOpt.get();

        // 다음 방으로 이동
        FanMeetingRoomOrder nextRoomOrder = fanMeetingRoomOrderRepository.findByFanMeetingIdAndCurrentRoom(fanMeetingId, currRoomOrder.getNextRoom()).orElseThrow(() -> new CustomException(NOT_FOUND_FANMEETING_ROOM_ORDER));

        Map<String, String> params = new HashMap<>();
        params.put("nextRoomId", currRoomOrder.getNextRoom());
        params.put("currRoomType", currRoomOrder.getType());
        params.put("nextRoomType", nextRoomOrder.getType());

        log.info("-----newEmitter: " + SseService.emitters.get(fanMeetingId).get(username));

//        try {
//            SseService.emitters.get(fanMeetingId).get(username).send(SseEmitter.event().name("moveToWaitRoom").data(params));
//        } catch (IOException e) {
//            throw new RuntimeException("--------- moveToWaitRoom 이벤트 보내기 실패", e);
//        }
        sseService.sendEvent(fanMeetingId, username, "moveToWaitRoom", params);
        log.info("Task " + " is running on thread " + Thread.currentThread().getName());
    }

    private String parseIdolName(String eventMessage) {
        try {
            JsonNode jsonNode = objectMapper.readTree(eventMessage);
            jsonNode = objectMapper.readTree(jsonNode.get("clientData").asText());
            jsonNode = objectMapper.readTree(jsonNode.get("clientData").asText());
            String idolName = jsonNode.get("idolName").asText();
            log.info("--------- idolName: " + idolName);

            return idolName;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private String parseGameType(String eventMessage) {
        try {
            JsonNode jsonNode = objectMapper.readTree(eventMessage);
            jsonNode = objectMapper.readTree(jsonNode.get("clientData").asText());
            jsonNode = objectMapper.readTree(jsonNode.get("clientData").asText());
            String gameType = jsonNode.get("gameType").asText();
            log.info("--------- gameType: " + gameType);

            return gameType;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Long parseFanMeetingId(String eventMessage) {
        try {
            JsonNode jsonNode = objectMapper.readTree(eventMessage);
            jsonNode = objectMapper.readTree(jsonNode.get("clientData").asText());
            jsonNode = objectMapper.readTree(jsonNode.get("clientData").asText());
            Long fanMeetingId = jsonNode.get("fanMeetingId").asLong();
            log.info("--------- Fan Meeting ID: " + fanMeetingId);

            return fanMeetingId;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String parseUsername(String eventMessage) {
        try {
            JsonNode jsonNode = objectMapper.readTree(eventMessage);
            jsonNode = objectMapper.readTree(jsonNode.get("clientData").asText());
            jsonNode = objectMapper.readTree(jsonNode.get("clientData").asText());
            String username = jsonNode.get("userName").asText();
            log.info("--------- User Name: " + username);

            return username;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
