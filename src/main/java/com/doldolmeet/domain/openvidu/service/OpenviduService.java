package com.doldolmeet.domain.openvidu.service;

import com.doldolmeet.domain.commons.Role;
import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.entity.FanMeetingRoomOrder;
import com.doldolmeet.domain.fanMeeting.entity.FanToFanMeeting;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRoomOrderRepository;
import com.doldolmeet.domain.fanMeeting.repository.FanToFanMeetingRepository;
import com.doldolmeet.domain.fanMeeting.sse.SseService;
import com.doldolmeet.domain.openvidu.dto.request.ConnUpdateRequestDto;
import com.doldolmeet.domain.openvidu.dto.response.EnterResponseDto;
import com.doldolmeet.domain.openvidu.dto.response.FanMeetingRoomsResponseDto;
import com.doldolmeet.domain.teleRoom.entity.TeleRoom;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.teleRoom.entity.TeleRoomFan;
import com.doldolmeet.domain.teleRoom.repository.TeleRoomFanRepository;
import com.doldolmeet.domain.teleRoom.repository.TeleRoomRepository;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.fan.repository.FanRepository;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.waitRoom.entity.WaitRoom;
import com.doldolmeet.domain.waitRoom.entity.WaitRoomFan;
import com.doldolmeet.domain.waitRoom.repository.WaitRoomFanRepository;
import com.doldolmeet.domain.waitRoom.repository.WaitRoomRepository;
import com.doldolmeet.exception.CustomException;
import com.doldolmeet.security.jwt.JwtUtil;
import com.doldolmeet.utils.Message;
import com.doldolmeet.utils.UserUtils;
import io.jsonwebtoken.Claims;
import io.openvidu.java.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.doldolmeet.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenviduService {
    private final JwtUtil jwtUtil;
    private final UserUtils userUtils;
    private final FanRepository fanRepository;
    private final FanMeetingRepository fanMeetingRepository;
    private final WaitRoomRepository waitRoomRepository;
    private final TeleRoomRepository teleRoomRepository;
    private final WaitRoomFanRepository waitRoomFanRepository;
    private final FanToFanMeetingRepository fanToFanMeetingRepository;
    private final TeleRoomFanRepository teleRoomFanRepository;
    private final FanMeetingRoomOrderRepository fanMeetingRoomOrderRepository;

    private Claims claims;
    @Value("${OPENVIDU_URL}")
    private String OPENVIDU_URL;

    @Value("${OPENVIDU_SECRET}")
    private String OPENVIDU_SECRET;

    public OpenVidu openvidu;

    @PostConstruct
    public void init() {
        this.openvidu = new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);
    }

    public ResponseEntity<String> initializeSession(Map<String, Object> params, Long fanMeetingId) throws OpenViduJavaClientException, OpenViduHttpException {
        SessionProperties properties = SessionProperties.fromJson(params).build();
        Session session = openvidu.createSession(properties);

        if (SseService.Rooms.get(fanMeetingId) == null) {
            SseService.Rooms.put(fanMeetingId, new ConcurrentHashMap<>());
        }
        SseService.Rooms.get(fanMeetingId).put(session.getSessionId(), session);

        return new ResponseEntity<>(session.getSessionId(), HttpStatus.OK);
    }

    public ResponseEntity<String> createConnection(String sessionId, Map<String, Object> params) throws OpenViduJavaClientException, OpenViduHttpException{
        Session session = openvidu.getActiveSession(sessionId);
        if (session == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ConnectionProperties properties = ConnectionProperties.fromJson(params).build();
        Connection connection = session.createConnection(properties);
        return new ResponseEntity<>(connection.getToken(), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> enterFanMeeting(Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        claims = jwtUtil.getClaims(request);
        String role = (String)claims.get("auth");
        EnterResponseDto responseDto = new EnterResponseDto();

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        // 존재하지 않는 팬미팅.
        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }
        FanMeeting fanMeeting = fanMeetingOpt.get();

        // 팬인 경우 재접속 판단.
        if (role.equals(Role.FAN.getKey())) {
            Fan fan = userUtils.getFan(claims.getSubject());
            WaitRoom waitRoom;
            Session waitRoomSession;

            // 일단 재접속 유무 검사.
            Optional<WaitRoomFan> waitRoomFanOpt = waitRoomFanRepository.findByFanIdAndWaitRoomId(fan.getId(), fanMeetingId);
            Optional<TeleRoomFan> teleRoomFanOpt = teleRoomFanRepository.findByFanIdAndTeleRoomId(fan.getId(), fanMeetingId);

            // 재접속이면, 팬은 대기방이었거나, 화상방이었다.

            // 대기방이었으면 그곳으로 간다.
            if (waitRoomFanOpt.isPresent()) {
                WaitRoomFan waitRoomFan = waitRoomFanOpt.get();
                waitRoom = waitRoomFan.getWaitRoom();

                // 다시 연결
                Session session = openvidu.getActiveSession(waitRoom.getRoomId());

                if (session == null) {
                    throw new CustomException(WAITROOM_NOT_FOUND);
                }

                ConnectionProperties properties = ConnectionProperties.fromJson(new HashMap<>()).build();
                Connection connection = session.createConnection(properties);

                waitRoomFan.setConnectionId(connection.getConnectionId());
                waitRoomFan.setConnectionToken(connection.getToken());

                responseDto.setToken(connection.getToken());

                return new ResponseEntity<>(new Message("팬 대기방에서 팅긴 후 대기방으로 재접속 성공", responseDto), HttpStatus.OK);
            }

            // 화상방이었으면 화상방의 대기방으로 간다.
            else if (teleRoomFanOpt.isPresent()) {
                TeleRoomFan teleRoomFan = teleRoomFanOpt.get();
                int idx = fanMeeting.getTeleRooms().indexOf(teleRoomFan.getTeleRoom());

                waitRoom = fanMeeting.getWaitRooms().get(idx);

                // 연결시도
                Session session = openvidu.getActiveSession(waitRoom.getRoomId());

                if (session == null) {
                    throw new CustomException(WAITROOM_NOT_FOUND);
                }

                ConnectionProperties properties = ConnectionProperties.fromJson(new HashMap<>()).build();
                Connection connection = session.createConnection(properties);

                WaitRoomFan waitRoomFan = WaitRoomFan.builder()
                        .orderNumber(teleRoomFan.getOrderNumber())
                        .fan(fan)
                        .waitRoom(waitRoom)
                        .connectionToken(connection.getToken())
                        .connectionId(connection.getConnectionId())
                        .build();

                // teleRoomFan 삭제
                teleRoomFanRepository.delete(teleRoomFan);

                // waitRoomFan 생성
                waitRoom.getWaitRoomFans().add(waitRoomFan);
                waitRoomFanRepository.save(waitRoomFan);

                return new ResponseEntity<>(new Message("팬이 화상방에서 나간 후 해당 아이돌의 대기방 재입장 성공", responseDto), HttpStatus.OK);
            }

            // 존재 안하면 아직 입장 안한것임.
            else{
                    Optional<FanToFanMeeting> fanToFanMeetingOpt = fanToFanMeetingRepository.findByFanAndFanMeeting(fan, fanMeeting);

                    if (!fanToFanMeetingOpt.isPresent()) {
                        throw new CustomException(FAN_TO_FANMEETING_NOT_FOUND);
                    }

                    waitRoom = fanMeeting.getWaitRooms().get(0);

                    // 연결시도
                    Session session = openvidu.getActiveSession(waitRoom.getRoomId());

                    if (session == null) {
                        throw new CustomException(WAITROOM_NOT_FOUND);
                    }

                    ConnectionProperties properties = ConnectionProperties.fromJson(new HashMap<>()).build();
                    Connection connection = session.createConnection(properties);

                    FanToFanMeeting fanToFanMeeting = fanToFanMeetingOpt.get();

                    // 새로 만들기
                    WaitRoomFan newWaitRoomFan = WaitRoomFan.builder()
                            .orderNumber(fanToFanMeeting.getOrderNumber())
                            .fan(userUtils.getFan(claims.getSubject()))
                            .waitRoom(waitRoom)
                            .connectionToken(connection.getToken())
                            .connectionId(connection.getConnectionId())
                            .build();

                    responseDto.setToken(connection.getToken());

                    // waitRoomFan 저장
                    waitRoom.getWaitRoomFans().add(newWaitRoomFan);
                    waitRoomFanRepository.save(newWaitRoomFan);

                    return new ResponseEntity<>(new Message("팬이 팬미팅 첫 입장 성공", responseDto), HttpStatus.OK);
                }
            }

            // 아이돌이 입장버튼 누를시, 자기방 생성
            else if (role.equals(Role.IDOL.getKey())) {
                Idol idol = userUtils.getIdol(claims.getSubject());

                // 이상한 아이돌이 접근시 에러
                if (!fanMeeting.getTeam().getIdols().contains(idol)) {
                    throw new CustomException(IDOL_NOT_IN_FANMEETING);
                }


                // 아이돌이 가지고 있는 TeleRoomId에 해당하는 방이 존재하면 거기로 가기
                if (openvidu.getActiveSession(idol.getTeleRoomId()) != null) {
                    Session session = openvidu.getActiveSession(idol.getTeleRoomId());

                    if (session == null) {
                        throw new CustomException(TELE_ROOM_NOT_FOUND);
                    }

                    ConnectionProperties properties = ConnectionProperties.fromJson(new HashMap<>()).build(); // params에 뭐?
                    Connection connection = session.createConnection(properties);

                    responseDto.setToken(connection.getToken());
                    responseDto.setTeleRoomId(idol.getTeleRoomId());
                    responseDto.setWaitRoomId(idol.getWaitRoomId());

                    return new ResponseEntity<>(new Message("아이돌이 자기방 재입장 성공", responseDto), HttpStatus.OK);
                }

                // 존재하지 않으면 화상방, 대기방 생성

                // teleRoom, waitRoom 생성
                Map<String, Object> param1 = new HashMap<>(); // videoRoom용
                Map<String, Object> param2 = new HashMap<>(); // waitList용

                param1.put("customSessionId", idol.getTeleRoomId());
                param2.put("customSessionId", idol.getWaitRoomId());

                SessionProperties properties1 = SessionProperties.fromJson(param1).build();
                SessionProperties properties2 = SessionProperties.fromJson(param2).build();

                try {
                    Session teleSession = openvidu.createSession(properties1);
                    Session waitSession = openvidu.createSession(properties2);

                    responseDto.setTeleRoomId(teleSession.getSessionId());
                    responseDto.setWaitRoomId(waitSession.getSessionId());

                    WaitRoom waitRoom = new WaitRoom();
                    waitRoom.setRoomId(idol.getWaitRoomId());
                    waitRoom.setFanMeeting(fanMeeting);

                    TeleRoom teleRoom = new TeleRoom();
                    teleRoom.setRoomId(idol.getTeleRoomId());
                    teleRoom.setFanMeeting(fanMeeting);

                    fanMeeting.getWaitRooms().add(waitRoom);
                    fanMeeting.getTeleRooms().add(teleRoom);

                    Session session = openvidu.getActiveSession(teleSession.getSessionId());

                    if (session == null) {
                        throw new CustomException(TELE_ROOM_NOT_FOUND);
                    }

                    ConnectionProperties properties = ConnectionProperties.fromJson(new HashMap<>()).build(); // params에 뭐?
                    Connection connection = session.createConnection(properties);

                    responseDto.setToken(connection.getToken());

                    waitRoomRepository.save(waitRoom);
                    teleRoomRepository.save(teleRoom);

                    return new ResponseEntity<>(new Message("아이돌 방생성 및 입장 성공", responseDto), HttpStatus.OK);
                } catch (OpenViduHttpException e) {
                    if (e.getStatus() == 409) {
                        return new ResponseEntity<>(new Message("이미 존재하는 방입니다.", null), HttpStatus.CONFLICT);
                    } else {
                        return new ResponseEntity<>(new Message("openvidu 오류", null), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            } else {
                throw new CustomException(USER_NOT_FOUND);
            }
        }

    @Transactional
    public ResponseEntity<Message> deleteWaitRoomFan(String IdolName, HttpServletRequest request){
        claims = jwtUtil.getClaims(request);

        Idol idol = userUtils.getIdol(IdolName);

        Optional<WaitRoom> waitRoomOpt = waitRoomRepository.findByRoomId(idol.getWaitRoomId());

        if (!waitRoomOpt.isPresent()) {
            throw new CustomException(WAITROOM_NOT_FOUND);
        }

        WaitRoom waitRoom = waitRoomOpt.get();
        Optional<WaitRoomFan> waitRoomFanOpt = waitRoomFanRepository.findFirstByWaitRoomOrderByOrderAsc(waitRoom);

        if (!waitRoomFanOpt.isPresent()) {
            throw new CustomException(WAITROOMFAN_NOT_FOUND);
        }

        WaitRoomFan waitRoomFan = waitRoomFanOpt.get();

        waitRoomFanRepository.delete(waitRoomFan);

        return new ResponseEntity<>(new Message("대기열에서 팬정보 삭제 성공", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> deleteTeleRoomFan(String IdolName, HttpServletRequest request){
        claims = jwtUtil.getClaims(request);

        Idol idol = userUtils.getIdol(IdolName);

        Optional<TeleRoom> teleRoomOpt = teleRoomRepository.findByRoomId(idol.getTeleRoomId());

        if (!teleRoomOpt.isPresent()) {
            throw new CustomException(TELE_ROOM_NOT_FOUND);
        }

        TeleRoom teleRoom = teleRoomOpt.get();
        TeleRoomFan teleRoomFan = teleRoom.getTeleRoomFan();

        if (teleRoomFan == null) {
            throw new CustomException(TELE_ROOMFAN_NOT_FOUND);
        }

        teleRoomFanRepository.delete(teleRoomFan);

        return new ResponseEntity<>(new Message("화상방에서 팬정보 삭제 성공", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> saveFanWaiting(String waitRoomId,Long fanMeetingId,String username) {
        Fan fan = userUtils.getFan(username);
        WaitRoom waitRoom = userUtils.getWaitRoom(waitRoomId);
        Optional<FanMeeting> fanMeeting = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeeting.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        Optional<FanToFanMeeting> fanToFanMeeting = fanToFanMeetingRepository.findByFanAndFanMeeting(fan, fanMeeting.get());

        if (!fanToFanMeeting.isPresent()) {
            throw new CustomException(FAN_TO_FANMEETING_NOT_FOUND);
        }

        Optional<WaitRoomFan> waitRoomFanOpt = waitRoomFanRepository.findByFanIdAndWaitRoomId(fan.getId(), fanMeetingId);

        if (waitRoomFanOpt.isPresent()) {
            throw new CustomException(WAITROOMFAN_ALREADY_EXIST);
        }

        WaitRoomFan waitRoomFan = WaitRoomFan.builder()
                .fan(fan)
                .waitRoom(waitRoom)
                .orderNumber(fanToFanMeeting.get().getOrderNumber())
                .connectionId(null)
                .connectionToken(null)
                .build();

        waitRoom.getWaitRoomFans().add(waitRoomFan);
        waitRoomFanRepository.save(waitRoomFan);

        return new ResponseEntity<>(new Message("대기열에 추가 성공", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> saveFanTeleing(String teleRoomId, Long fanMeetingId, String username) {
        Fan fan = userUtils.getFan(username);
        TeleRoom teleRoom = userUtils.getTeleRoom(teleRoomId);
        Optional<FanMeeting> fanMeeting = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeeting.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        Optional<FanToFanMeeting> fanToFanMeeting = fanToFanMeetingRepository.findByFanAndFanMeeting(fan, fanMeeting.get());

        if (!fanToFanMeeting.isPresent()) {
            throw new CustomException(FAN_TO_FANMEETING_NOT_FOUND);
        }

        Optional<TeleRoomFan> teleRoomFanOpt = teleRoomFanRepository.findByFanIdAndTeleRoomId(fan.getId(), fanMeetingId);


        if (teleRoomFanOpt.isPresent()) {
            log.info("왜 여기 안옴");
            throw new CustomException(TELEROOMFAN_ALREADY_EXIST);
        }

        TeleRoomFan teleRoomFan = TeleRoomFan.builder()
                .fan(fan)
                .teleRoom(teleRoom)
                .orderNumber(fanToFanMeeting.get().getOrderNumber())
                .connectionId(null)
                .connectionToken(null)
                .build();

        teleRoom.setTeleRoomFan(teleRoomFan);
        teleRoomFanRepository.save(teleRoomFan);

        return new ResponseEntity<>(new Message("화상방에 추가 성공", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> updateConnection(ConnUpdateRequestDto requestDto, HttpServletRequest request) {
        Fan fan = userUtils.getFan(requestDto.getUsername());

        if (requestDto.getType().equals("TELE")) {
            TeleRoom teleRoom = userUtils.getTeleRoom(requestDto.getRoomId());
            Optional<TeleRoomFan> teleRoomFanOpt = teleRoomFanRepository.findByFanIdAndTeleRoomId(fan.getId(), teleRoom.getId());

            if (!teleRoomFanOpt.isPresent()) {
                throw new CustomException(TELE_ROOMFAN_NOT_FOUND);
            }

            TeleRoomFan teleRoomFan = teleRoomFanOpt.get();
            teleRoomFan.setConnectionId(requestDto.getConnectionId());
            teleRoomFan.setConnectionToken(requestDto.getConnectionToken());

            teleRoomFanRepository.save(teleRoomFan);
            return new ResponseEntity<>(new Message("화상방 팬 커넥션 업데이트 성공", null), HttpStatus.OK);
        }
        else if (requestDto.getType().equals("WAIT")) {
            WaitRoom waitRoom = userUtils.getWaitRoom(requestDto.getRoomId());
            Optional<WaitRoomFan> waitRoomFanOpt = waitRoomFanRepository.findByFanIdAndWaitRoomId(fan.getId(), waitRoom.getId());

            if (!waitRoomFanOpt.isPresent()) {
                throw new CustomException(WAITROOMFAN_NOT_FOUND);
            }

            WaitRoomFan waitRoomFan = waitRoomFanOpt.get();
            waitRoomFan.setConnectionId(requestDto.getConnectionId());
            waitRoomFan.setConnectionToken(requestDto.getConnectionToken());

            waitRoomFanRepository.save(waitRoomFan);
            return new ResponseEntity<>(new Message("대기방 팬 커넥션 업데이트 성공", null), HttpStatus.OK);
        }
        else {
            throw new CustomException(UNKNOWN_TYPE);
        }
    }

    @Transactional
    public ResponseEntity<Message> createFanMeetingRooms(Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException{
        // 어드민인지 체크
        claims = jwtUtil.getClaims(request);
        userUtils.checkIfAdmin(claims);

        // 팬미팅을 통해서 팬미팅 룸 오더 받아오기
        List<FanMeetingRoomOrder> roomOrders = fanMeetingRoomOrderRepository.findByFanMeetingId(fanMeetingId);
        FanMeetingRoomsResponseDto responseDto = new FanMeetingRoomsResponseDto();

        for (FanMeetingRoomOrder roomOrder : roomOrders) {
            String roomId = roomOrder.getCurrentRoom();

            Map<String, Object> params = new HashMap<>();
            params.put("customSessionId", roomId);


            initializeSession(params, fanMeetingId);
            createConnection(roomId, new HashMap<>());
            responseDto.getRoomIds().add(roomId);
        }

        return new ResponseEntity<>(new Message("팬미팅 전체 방 생성 성공", responseDto), HttpStatus.OK);
    }

    public List<Connection> getConnections(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
        openvidu.fetch();
        Session session = openvidu.getActiveSession(sessionId);

        if (session == null) {
            throw new CustomException(SESSION_NOT_FOUND);
        }

        return session.getConnections();
    }

    public Session getSession(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
        openvidu.fetch();
        Session session = openvidu.getActiveSession(sessionId);

        if (session == null) {
            throw new CustomException(SESSION_NOT_FOUND);
        }

        return session;
    }
}
