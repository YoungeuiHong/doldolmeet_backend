package com.doldolmeet.domain.fanMeeting.sse;

import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.entity.FanMeetingRoomOrder;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRoomOrderRepository;
import com.doldolmeet.domain.openvidu.service.OpenviduService;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.exception.CustomException;
import com.doldolmeet.exception.ErrorCode;
import com.doldolmeet.utils.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.doldolmeet.exception.ErrorCode.*;


@CrossOrigin(origins = "https://youngeui-in-jungle.store/")
@RestController
@RequiredArgsConstructor
@Slf4j
public class SseController {
    public final SseService sseService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FanMeetingRoomOrderRepository fanMeetingRoomOrderRepository;
    private final FanMeetingRepository fanMeetingRepository;
    private final OpenviduService openviduService;
    private final IdolRepository idolRepository;

    private OpenVidu openvidu = new OpenVidu("https://youngeui-in-jungle.store/", "MY_SECRET");


    @GetMapping(path = "/fanMeetings/{fanMeetingId}/sse/{username}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createEmitter(@PathVariable Long fanMeetingId, @PathVariable String username, @RequestHeader(name = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        // Add the emitter to a list of subscribers or handle it in another way
        log.info("SseController.subscribe() 호출됨");
        log.info("fanMeetingId: " + fanMeetingId);
        log.info("username: " + username);
        log.info("lastEventId: " + lastEventId);

        return sseService.createEmitter(fanMeetingId, username, lastEventId);
    }

    @Async
    @PostMapping("/my_webhook")
    public String webhook(@RequestBody String eventMessage) throws OpenViduJavaClientException, OpenViduHttpException{

        log.info("Webhook received!");
        log.info("EVENTMESSAGE:" + eventMessage);

        // 필요하지 않은 이벤트들 그냥 리턴.
        if (eventMessage.contains("sessionCreated") || eventMessage.contains("sessionDestroyed") || eventMessage.contains("webrtcConnectionCreated") || eventMessage.contains("webrtcConnectionDestroyed") || eventMessage.contains("streamCreated") || eventMessage.contains("streamDestroyed") || eventMessage.contains("recordingStatusChanged")) {
            return eventMessage;
        }

        // 어드민이면 무시
        if (eventMessage.contains("ADMIN")) {
            return eventMessage;
        }

        String username = parseUsername(eventMessage);
        Long fanMeetingId = parseFanMeetingId(eventMessage);
        String sessionId = parseSessionId(eventMessage);

        // 아이돌인 경우
        if (eventMessage.contains("IDOL")) {
            // 게임방에 들어왔을 경우
            if (eventMessage.contains("participantJoined") && eventMessage.contains("gameRoom")) {
                if (SseService.isIdolsEntered.get(fanMeetingId) == null) {
                    SseService.isIdolsEntered.put(fanMeetingId, new ConcurrentHashMap<>());
                }

                if (SseService.isIdolsEntered.get(fanMeetingId).get(username) == null) {
                    SseService.isIdolsEntered.get(fanMeetingId).put(username, true);
                }
                SseService.isIdolsEntered.get(fanMeetingId).put(username, true);

                // keyset 크기가 2이고, 모두 들어왔으면 해당 방의 모든 팬의 에미터에게 다 들어왔다는 이벤트를 쏴주기
                FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId).orElseThrow(() -> new CustomException(FANMEETING_NOT_FOUND));
                Integer teamSize = fanMeeting.getTeam().getTeamSize();

                if (SseService.isIdolsEntered.get(fanMeetingId).size() == teamSize && SseService.isIdolsEntered.get(fanMeetingId).values().stream().allMatch(isEntered -> isEntered)) {
//                    try {
                        log.info("모두 들어왔으니까 팬들에게 다 들어왔다고 알려주기");
                        // TODO: 지금은 해당 팬미팅에 접속한 모든 팬들에게 다 들어왔다고 알려주는데, 나중에는 해당 아이돌 방에 들어온 팬들에게만 알려주는게 좋을듯.

                    SseService.gameRooms.get(fanMeetingId).forEach(fanUsername -> {
                        sseService.sendEvent(fanMeetingId, fanUsername, "allIdolEntered", new HashMap<>());
                    });

                    // 해당 방에 있는 모든 아이돌한테도 알려주기
                    SseService.isIdolsEntered.get(fanMeetingId).keySet().forEach(idolUsername -> {
                        sseService.sendEvent(fanMeetingId, idolUsername, "allIdolEntered", new HashMap<>());
                        });
                }
            }
            // 게임방에서 나갔을 경우
            else if (eventMessage.contains("participantLeft") && eventMessage.contains("gameRoom")) {
                if (SseService.isIdolsEntered.get(fanMeetingId) == null) {
                    SseService.isIdolsEntered.put(fanMeetingId, new ConcurrentHashMap<>());
                }
                if (SseService.isIdolsEntered.get(fanMeetingId).get(username) != null) {
                    SseService.isIdolsEntered.get(fanMeetingId).put(username, false);
                }
            }
            else {
                return eventMessage;
            }
        }

        // 팬인 경우,
        else if (eventMessage.contains("FAN")) {
            // 참가자가 대기방에 들어왔을 때
            if (eventMessage.contains("participantJoined") && eventMessage.contains("waitingRoom")) {
                // 현재 방의 fanMeetingRoomOrder
                Optional<FanMeetingRoomOrder> currFanMeetingRoomOrderOpt = fanMeetingRoomOrderRepository.findByFanMeetingIdAndCurrentRoom(fanMeetingId, sessionId);
                // 없으면 예외
                if (currFanMeetingRoomOrderOpt.isEmpty()) {
                    throw new CustomException(NOT_FOUND_FANMEETING_ROOM_ORDER);
                }
                FanMeetingRoomOrder currRoomOrder = currFanMeetingRoomOrderOpt.get();

                // 다음 방의 fanMeetingRoomOrder
                Optional<FanMeetingRoomOrder> nextFanMeetingRoomOrderOpt = fanMeetingRoomOrderRepository.findByFanMeetingIdAndCurrentRoom(fanMeetingId, currRoomOrder.getNextRoom());
                // 없으면 예외
                if (nextFanMeetingRoomOrderOpt.isEmpty()) {
                    throw new CustomException(NOT_FOUND_FANMEETING_ROOM_ORDER);
                }

                FanMeetingRoomOrder nextRoomOrder = nextFanMeetingRoomOrderOpt.get();

                // 다음 방이 idolRoom이면,
                if (nextRoomOrder.getType().equals("idolRoom")) {
                    // 다음 방의 커넥션 리스트 얻어서,
                    List<Connection> connections = openviduService.getConnections(nextRoomOrder.getCurrentRoom());

                    boolean fanInIdolRoom = false;
                    boolean idolInIdolRoom = false;
                    boolean adminInIdolRoom = false;
                    for (Connection connection:connections){
                        if (connection.getClientData().contains("FAN")) {
                            fanInIdolRoom = true;
                        }
                        if (connection.getClientData().contains("IDOL")) {
                            idolInIdolRoom = true;
                        }
                        if (connection.getClientData().contains("ADMIN")) {
                            adminInIdolRoom = true;
                        }
                    }

                    // 다음 방에 들어갈 수 있는 경우는 Admin과 Idol이 있고 , FAN 이 없는 경우
                    if (!fanInIdolRoom && idolInIdolRoom && adminInIdolRoom) {
                        Map<String, String> params = new HashMap<>();
                        params.put("nextRoomId", currRoomOrder.getNextRoom());
                        params.put("currRoomType", currRoomOrder.getType());
                        params.put("idolNickName", currRoomOrder.getNickname());
                        params.put("roomThumbnail", currRoomOrder.getRoomThumbnail());
                        params.put("motionType", nextRoomOrder.getMotionType());

                        log.info("해당 방에 Admin, Idol만 존재해서 팬 들여보냄.");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new CustomException(THREAD_INTERRUPTED);
                        }

                        openvidu.fetch();
                        Session session = openvidu.getActiveSession(sessionId);
                        session.forceDisconnect(parseConnectionId(eventMessage));

                        sseService.sendEvent(fanMeetingId, username, "moveToIdolRoom", params);
                        return eventMessage;
                        // 쏘고 나면, 클라이언트에서 이 이벤트를 받아 처리한다.(화면 전환 + 해당 세션에 입장)
                        // 입장시, joined 이벤트 발생 -> 웹훅 -> 대기방에 추가됨.

                    }


                    // 관리자만 들어와 있는 경우라고 설정
                    else if (!fanInIdolRoom && !idolInIdolRoom && adminInIdolRoom) {
                        Map<String, String> params = new HashMap<>();
                        params.put("nextRoomId", currRoomOrder.getNextRoom());
                        params.put("currRoomType", currRoomOrder.getType());
                        params.put("idolNickName", currRoomOrder.getNickname());
                        params.put("roomThumbnail", currRoomOrder.getRoomThumbnail());
                        params.put("motionType", nextRoomOrder.getMotionType());

                        log.info("관리자만 있는데 일단 들여보냄");

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new CustomException(THREAD_INTERRUPTED);
                        }

                        openvidu.fetch();
                        Session session = openvidu.getActiveSession(sessionId);
                        session.forceDisconnect(parseConnectionId(eventMessage));

                        sseService.sendEvent(fanMeetingId, username, "moveToIdolRoom", params);
                        return eventMessage;
                        // 쏘고 나면, 클라이언트에서 이 이벤트를 받아 처리한다.(화면 전환 + 해당 세션에 입장)
                        // 입장시, joined 이벤트 발생 -> 웹훅 -> 대기방에 추가됨.
                    }
                    else {
                        log.info("(해당 아이돌 방에 Admin만 있는 경우), (Admin과 Idol이 있고, FAN 이 없는 경우) 를 제외한 경우");
                        sseService.addwaiter(username, fanMeetingId, sessionId);
                        throw new CustomException(INVALID_IDOLROOM_STATE);
                    }
                }

                // 다음 방도 waitRoom이면 그냥 웨이팅룸에 넣기.
                else if (nextRoomOrder.getType().equals("waitRoom")) {
                    sseService.addwaiter(username, fanMeetingId, sessionId);
                    return eventMessage;
                }

                // waitRoom에 들어갔는데 다음 방이 끝일리 없음
                else {
                    throw new CustomException(INVALID_FANMEETING_END);
                }
            }

            // 참가자가 대기방에서 나갔을 때
            else if (eventMessage.contains("participantLeft") && eventMessage.contains("waitingRoom")) {
                sseService.removeWaiter(username, fanMeetingId, sessionId);
            }

            // 참가자가 아이돌방에 들어왔을 때
            else if (eventMessage.contains("participantJoined") && eventMessage.contains("idolRoom")) {
                log.info("timer 시작");
                //countdown 시작
                // 킥될 때는 바로 대기방B에 들어가는게 좋을듯(그래야 아이돌방A에서 나가는 이벤트가 바로 발생해서, 대기방A에 들어오는 팬과 레이스컨디션 되지 않을듯, 근데 초대 보내졌는데 늦게 클릭하다가 대기방)
                waitAndKick(eventMessage);
            }

            // 참가자가 아이돌방에서 나갔을 때
            else if (eventMessage.contains("participantLeft") & eventMessage.contains("idolRoom")) {
                // 자기 대기방에 있는 팬 중 가장 우선순위 높은 팬에게 쏨
                // 해당 방을 nextRoomId로 가지는 RoomOrder 조회
                Optional<FanMeetingRoomOrder> prevFanMeetingRoomOrderOpt = fanMeetingRoomOrderRepository.findByFanMeetingIdAndNextRoom(fanMeetingId, sessionId);
                // 없으면 예외
                if (prevFanMeetingRoomOrderOpt.isEmpty()) {
                    throw new CustomException(NOT_FOUND_FANMEETING_ROOM_ORDER);
                }
                FanMeetingRoomOrder prevRoomOrder = prevFanMeetingRoomOrderOpt.get();


                if (SseService.waitingRooms.get(fanMeetingId) == null) {
                    SseService.waitingRooms.put(fanMeetingId, new ConcurrentHashMap<>());
                }

                // 해당 대기방 객체가 없으면 만들어주고 리턴.
                if (SseService.waitingRooms.get(fanMeetingId).get(prevRoomOrder.getCurrentRoom()) == null) {
                    Comparator comparator = new OrderNumberComparator();
                    SortedSet<UserNameAndOrderNumber> sortedSet = new TreeSet(comparator);
                    SseService.waitingRooms.get(fanMeetingId).put(sessionId, sortedSet);
                    throw new CustomException(WAITROOM_NOT_FOUND);
                }

                // 자기 대기방에 아무도 없으면 그냥 리턴.
                SortedSet<UserNameAndOrderNumber> waitingRoom = SseService.waitingRooms.get(fanMeetingId).get(prevRoomOrder.getCurrentRoom());
                if (waitingRoom.isEmpty()) {
                    return eventMessage;
                }

                String newUsername = waitingRoom.first().getUsername();

                Optional<FanMeetingRoomOrder> nextFanMeetingRoomOrderOpt = fanMeetingRoomOrderRepository.findByFanMeetingIdAndCurrentRoom(fanMeetingId, prevRoomOrder.getNextRoom());

                if (nextFanMeetingRoomOrderOpt.isEmpty()) {
                    throw new CustomException(NOT_FOUND_FANMEETING_ROOM_ORDER);
                }
                FanMeetingRoomOrder nextRoomOrder = nextFanMeetingRoomOrderOpt.get();

//                try {
                Map<String, String> params = new HashMap<>();
                params.put("nextRoomId", prevRoomOrder.getNextRoom());
                params.put("currRoomType", prevRoomOrder.getType());
                params.put("idolNickName", prevRoomOrder.getNickname());
                params.put("roomThumbnail", prevRoomOrder.getRoomThumbnail());
                params.put("motionType", nextRoomOrder.getMotionType());

                sseService.sendEvent(fanMeetingId, newUsername, "moveToIdolRoom", params);
            }

            // 참가자가 게임방에 들어왔을 때
            else if (eventMessage.contains("participantJoined") && eventMessage.contains("gameRoom")) {
                if (SseService.gameRooms.get(fanMeetingId) == null) {
                    SseService.gameRooms.put(fanMeetingId, new ArrayList<>());
                }
                SseService.gameRooms.get(fanMeetingId).add(username);
            }

            // 참가자가 게임방에서 나갔을 때
            else if (eventMessage.contains("participantLeft") && eventMessage.contains("gameRoom")) {
                if (SseService.gameRooms.get(fanMeetingId).contains(username)) {
                    SseService.gameRooms.get(fanMeetingId).remove(username);
                }
            }
        }

        // 그밖에는 예외
        else {
            throw new CustomException(INVALID_USER_TYPE);
        }

        return eventMessage;
    }

    // 해당 SSE 이벤트를 클라이언트에서 정상적으로 받았을 때 호출해서 db에 기록 저장하는 API
    @PostMapping("/fanMeetings/{fanMeetingId}/sse/{username}/received")
    public String eventReceived(@PathVariable Long fanMeetingId, @PathVariable String username, @RequestBody String event) {
        log.info("SseController.eventReceived() 호출됨");
        log.info("fanMeetingId: " + fanMeetingId);
        log.info("username: " + username);
        log.info("event: " + event);

        return sseService.eventReceived(fanMeetingId, username, event);
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

    private String parseSessionId(String eventMessage) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(eventMessage);
            String sessionId = jsonNode.get("sessionId").asText();
            log.info("------SessionId: " + sessionId);

            return sessionId;
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

    private String parseConnectionId(String eventMessage) {
        try {
            JsonNode jsonNode = objectMapper.readTree(eventMessage);
            String connectionId = jsonNode.get("connectionId").asText();
            log.info("--------- Connection ID: " + connectionId);

            return connectionId;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitAndKick(String body) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        // Submit tasks to the thread pool
//        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//        System.out.println(openvidu);
//        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        executorService.execute(new MyTask(body, openviduService, objectMapper, fanMeetingRoomOrderRepository, openvidu, idolRepository, sseService));
        // Shutdown the thread pool when done
        executorService.shutdown();
    }
}
