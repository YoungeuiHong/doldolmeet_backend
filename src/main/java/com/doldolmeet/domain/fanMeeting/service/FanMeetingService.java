package com.doldolmeet.domain.fanMeeting.service;

import com.doldolmeet.domain.fanMeeting.dto.request.FanMeetingRequestDto;
import com.doldolmeet.domain.fanMeeting.dto.response.*;
import com.doldolmeet.domain.fanMeeting.entity.*;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRoomOrderRepository;
import com.doldolmeet.domain.fanMeeting.repository.FanToFanMeetingRepository;
import com.doldolmeet.domain.teleRoom.repository.TeleRoomFanRepository;
import com.doldolmeet.domain.team.entity.Team;
import com.doldolmeet.domain.team.repository.TeamRepository;
import com.doldolmeet.domain.teleRoom.entity.TeleRoomFan;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.domain.users.fan.repository.FanRepository;
import com.doldolmeet.domain.users.idol.entity.Idol;
import com.doldolmeet.domain.users.idol.repository.IdolRepository;
import com.doldolmeet.domain.waitRoom.chat.repository.ChatRoomRepository;
import com.doldolmeet.domain.waitRoom.entity.WaitRoom;
import com.doldolmeet.domain.waitRoom.entity.WaitRoomFan;
import com.doldolmeet.domain.waitRoom.repository.WaitRoomFanRepository;
import com.doldolmeet.domain.waitRoom.repository.WaitRoomRepository;
import com.doldolmeet.exception.CustomException;
import com.doldolmeet.security.jwt.JwtUtil;
import com.doldolmeet.utils.Message;
import com.doldolmeet.utils.UserUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

import static com.doldolmeet.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FanMeetingService {
    private final FanMeetingRepository fanMeetingRepository;
    private final TeamRepository teamRepository;
    private final FanToFanMeetingRepository fanToFanMeetingRepository;
    private final FanRepository fanRepository;
    private final IdolRepository idolRepository;
    private final WaitRoomFanRepository waitRoomFanRepository;
    private final WaitRoomRepository waitRoomRepository;
    private final TeleRoomFanRepository teleRoomFanRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final FanMeetingRoomOrderRepository fanMeetingRoomOrderRepository;
    private final JwtUtil jwtUtil;
    private final UserUtils userUtils;
    private Claims claims;

    @Transactional
    public ResponseEntity<Message> createFanMeeting(FanMeetingRequestDto requestDto, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        userUtils.checkIfAdmin(claims);

        Optional<Team> team = teamRepository.findByTeamName(requestDto.getTeamName());

        if (!team.isPresent()) {
            throw new CustomException(TEAM_NOT_FOUND);
        }

        String chatRoomId = chatRoomRepository.createChatRoom(requestDto.getFanMeetingName()).getRoomId();

        FanMeeting fanMeeting = FanMeeting.builder()
                .startTime(requestDto.getStartTime())
                .endTime(requestDto.getEndTime())
                .capacity(requestDto.getCapacity())
                .fanMeetingName(requestDto.getFanMeetingName())
                .fanMeetingImgUrl(requestDto.getFanMeetingImgUrl())
                .team(team.get())
                .waitRooms(new ArrayList<>())
                .fanToFanMeetings(new ArrayList<>())
                .teleRooms(new ArrayList<>())
                .fanMeetingRoomOrders(new ArrayList<>())
                .isRoomsCreated(false)
                .isStarted(false)
                .nextOrder(1L)
                .chatRoomId(chatRoomId)
                .build();


        List<Idol> idols = team.get().getIdols();

        int sz = idols.size() * 2;

        // 메인 대기방 생성, 1개
        FanMeetingRoomOrder roomOrder;
        roomOrder = FanMeetingRoomOrder.builder()
                .currentRoom(UUID.randomUUID().toString())
                .nextRoom(null)
                .fanMeeting(fanMeeting)
                .nickname("main")
                .type("mainWaitRoom")
                .motionType(null)
                .build();

        fanMeeting.getFanMeetingRoomOrders().add(roomOrder);

        int cnt = 1;
        for (int i = 0; i < sz; i++) { // sz개
            String myRoomId = UUID.randomUUID().toString();;

            roomOrder = FanMeetingRoomOrder.builder()
                    .currentRoom(myRoomId)
                    .nextRoom(null)
                    .fanMeeting(fanMeeting)
                    .nickname(idols.get(i/2).getUserCommons().getNickname())
                    .roomThumbnail(idols.get(i/2).getUserCommons().getThumbNailImgUrl())
                    .type(i % 2 == 0 ? "waitRoom" : "idolRoom")
                    .motionType(i%4 == 0 || i%4 == 1 ? "halfHeart" : "bigHeart")
//                    .gameType(""+cnt)
                    .build();

            if (i % 2 == 1) {
                cnt += 1;
            }

            fanMeeting.getFanMeetingRoomOrders().get(i).setNextRoom(myRoomId);
            fanMeeting.getFanMeetingRoomOrders().add(roomOrder);
        }

        roomOrder = FanMeetingRoomOrder.builder()
                .currentRoom(UUID.randomUUID().toString())
                .nextRoom("END")
                .fanMeeting(fanMeeting)
                .nickname("gameRoom")
                .roomThumbnail("gameRoomUrl")
                .type("gameRoom")
                .motionType("noMotion")
//                .gameType("noGameType")
                .build();

        fanMeeting.getFanMeetingRoomOrders().get(sz).setNextRoom(roomOrder.getCurrentRoom());
        fanMeeting.getFanMeetingRoomOrders().add(roomOrder);

        fanMeetingRepository.save(fanMeeting);
        Map<String, Long> result = new HashMap<>();
        result.put("id", fanMeeting.getId());

        return new ResponseEntity<>(new Message("팬미팅 생성 완료", result), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> getFanMeetings(String option) {
        List<FanMeetingResponseDto> result = new ArrayList<>();
        List<FanMeeting> fanMeetings;

        if (option.equals(FanMeetingSearchOption.OPENED.value())) {
            fanMeetings = fanMeetingRepository.findFanMeetingsByEndTimeAfter(LocalDateTime.now());
        }
        else if (option.equals(FanMeetingSearchOption.CLOSED.value())) {
            fanMeetings = fanMeetingRepository.findFanMeetingsByEndTimeBefore(LocalDateTime.now());
        }

        else {
            fanMeetings = fanMeetingRepository.findAll();
        }

        for (FanMeeting fanMeeting : fanMeetings) {
            FanMeetingResponseDto responseDto = FanMeetingResponseDto.builder()
                    .id(fanMeeting.getId())
                    .imgUrl(fanMeeting.getFanMeetingImgUrl())
                    .title(fanMeeting.getFanMeetingName())
                    .startTime(fanMeeting.getStartTime())
                    .endTime(fanMeeting.getEndTime())
                    .chatRoomId(fanMeeting.getChatRoomId())
                    .teamName(fanMeeting.getTeam().getTeamName())
                    .build();

            result.add(responseDto);
        }

        return new ResponseEntity<>(new Message("팬미팅 조회 성공", result), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> applyFanMeeting(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Fan fan = userUtils.getFan(claims.getSubject());

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        FanMeeting fanMeeting = fanMeetingOpt.get();

        String chatRoomId = chatRoomRepository.createChatRoom(fanMeeting.getFanMeetingName()).getRoomId();

        FanToFanMeeting fanToFanMeeting = FanToFanMeeting.builder()
                .fanMeetingApplyStatus(FanMeetingApplyStatus.APPROVED)
                .fan(fan)
                .fanMeeting(fanMeeting)
                .orderNumber(fanMeeting.getNextOrder())
                .chatRoomId(chatRoomId)
                .gameScore(0L)
                .build();

        fanMeeting.setNextOrder(fanMeeting.getNextOrder() + 1L);
        fan.getFanToFanMeetings().add(fanToFanMeeting);
        fanMeeting.getFanToFanMeetings().add(fanToFanMeeting);

        fanToFanMeetingRepository.save(fanToFanMeeting);

        FanToFanMeetingResponseDto responseDto = FanToFanMeetingResponseDto.builder()
                .id(fanToFanMeeting.getId())
                .fanMeetingId(fanMeetingId)
                .fanId(fan.getId())
                .orderNumber(fanToFanMeeting.getOrderNumber())
                .fanMeetingApplyStatus(FanMeetingApplyStatus.APPROVED)
                .chatRoomId(chatRoomId)
                .teamName(fanMeeting.getTeam().getTeamName())
                .gameScore(fanToFanMeeting.getGameScore())
                .build();

        return new ResponseEntity<>(new Message("팬미팅 신청 성공", responseDto), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> getMyTodayFanMeeting(HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Optional<Fan> fan = fanRepository.findByUserCommonsUsername(claims.getSubject());
        Optional<Idol> idol = idolRepository.findByUserCommonsUsername(claims.getSubject());

        Optional<FanMeeting> fanMeetingOpt;
//
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime midNightTime = currentTime.with(LocalTime.MIN);
        LocalDateTime tomorrowMidNightTime = midNightTime.plusDays(1);

        log.info("현재시간: " + currentTime);
        log.info("자정시간: " + midNightTime);
//
        FanMeeting fanMeeting = fanMeetingRepository.findAll().get(0);

//        if (fan.isPresent()) {
//            FanMeeting fanMeeting = fanMeetingRepository.findAll().get(0);
//
////            fanMeetingOpt = fanMeetingRepository.findFanMeetingsByFan(fan.get(), midNightTime, currentTime, tomorrowMidNightTime);
////
////            // 팬미팅에 대한 현재 사용자의 신청 여부 확인
////            Optional<FanToFanMeeting> fanToFanMeetingOpt = fanToFanMeetingRepository.findByFanAndFanMeeting(fan.orElse(null), fanMeetingOpt.orElse(null));
////
////            if (!fanToFanMeetingOpt.isPresent()) {
////                // 현재 사용자가 해당 팬미팅에 신청하지 않은 경우
////                throw new CustomException(FANMEETING_NOT_APPLIED);
////            }
//        } else if (idol.isPresent()) {
//            Fa
////            fanMeetingOpt = fanMeetingRepository.findFanMeetingsByTeamOne(idol.get().getTeam(), midNightTime, currentTime, tomorrowMidNightTime);
//        } else {
//            throw new CustomException(USER_NOT_FOUND);
//        }
//
//        if (!fanMeetingOpt.isPresent()) {
//            throw new CustomException(FANMEETING_NOT_FOUND);
//        }

//        FanMeeting fanMeeting = fanMeetingOpt.get();

        if (fanMeeting.getIsRoomsCreated()) {
            FanMeetingResponseDto responseDto = FanMeetingResponseDto.builder()
                    .id(fanMeeting.getId())
                    .imgUrl(fanMeeting.getFanMeetingImgUrl())
                    .title(fanMeeting.getFanMeetingName())
                    .startTime(fanMeeting.getStartTime())
                    .endTime(fanMeeting.getEndTime())
                    .chatRoomId(fanMeeting.getChatRoomId())
                    .teamName(fanMeeting.getTeam().getTeamName())
                    .build();

            return new ResponseEntity<>(new Message("나의 예정된 팬미팅 중 가장 최신 팬미팅 받기 성공", responseDto), HttpStatus.OK);
        }
        else {
            throw new CustomException(FANMEETING_ROOMS_NOT_CREATED);
        }
    }


    @Transactional
    public ResponseEntity<Message> canEnterFanMeeting(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Fan fan = userUtils.getFan(claims.getSubject());

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        FanMeeting fanMeeting = fanMeetingOpt.get();

        Optional<FanToFanMeeting> fanToFanMeetingOpt = fanToFanMeetingRepository.findByFanAndFanMeeting(fan, fanMeeting);

        if (!fanToFanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        FanToFanMeeting fanToFanMeeting = fanToFanMeetingOpt.get();

        if (!fanToFanMeeting.getFanMeetingApplyStatus().equals(FanMeetingApplyStatus.APPROVED)) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        return new ResponseEntity<>(new Message("팬미팅 입장 가능", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> getMainWaitRoom(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        FanMeeting fanMeeting = fanMeetingOpt.get();

        String mainWaitRoomId = fanMeeting.getFanMeetingRoomOrders().get(0).getCurrentRoom();

        MainWaitRoomResponseDto responseDto = MainWaitRoomResponseDto.builder()
                .roomId(mainWaitRoomId)
                .build();

        return new ResponseEntity<>(new Message("팬미팅의 메인 대기방 데이터 조회 성공", responseDto), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> getNextFan(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Idol idol = userUtils.getIdol(claims.getSubject());

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);
        Optional<WaitRoom> waitRoomOpt = waitRoomRepository.findByRoomId(idol.getWaitRoomId());

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        if (!waitRoomOpt.isPresent()) {
            throw new CustomException(WAITROOM_NOT_FOUND);
        }

        WaitRoom waitRoom = waitRoomOpt.get();
        Optional<WaitRoomFan> waitRoomFan = waitRoomFanRepository.findFirstByWaitRoomOrderByOrderAsc(waitRoom);

        if (!waitRoomFan.isPresent()) {
            throw new CustomException(WAITROOMFAN_NOT_FOUND);
        }

        Fan fan = waitRoomFan.get().getFan();

        NextFanResponseDto responseDto = NextFanResponseDto.builder()
                .username(fan.getUserCommons().getUsername())
                .connectionId(waitRoomFan.get().getConnectionId())
                .waitRoomId(waitRoom.getRoomId())
                .roomType(RoomType.WAITING_ROOM)
                .build();

        return new ResponseEntity<>(new Message("다음에 참여할 팬 조회 성공", responseDto), HttpStatus.OK);
    }

    public ResponseEntity<Message> getNextWaitRoomId(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Idol idol = userUtils.getIdol(claims.getSubject());

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);
        Optional<WaitRoom> waitRoomOpt = waitRoomRepository.findByRoomId(idol.getWaitRoomId());

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        if (!waitRoomOpt.isPresent()) {
            throw new CustomException(WAITROOM_NOT_FOUND);
        }

        FanMeeting fanMeeting = fanMeetingOpt.get();
        WaitRoom waitRoom = waitRoomOpt.get();

        int waitRoomIdx = fanMeeting.getWaitRooms().indexOf(waitRoom);

        NextWaitRoomResponseDto responseDto = new NextWaitRoomResponseDto();

        // 마지막 대기열이었을 경우,
        if (waitRoomIdx == fanMeeting.getWaitRooms().size() - 1) {
            responseDto.setRoomId("END");
            return new ResponseEntity<>(new Message("마지막 대기열입니다.", responseDto), HttpStatus.OK);
        }

        // 그 외엔 다음 대기열세션ID 반환
        WaitRoom nextWaitRoom = fanMeeting.getWaitRooms().get(waitRoomIdx + 1);

        responseDto.setRoomId(nextWaitRoom.getRoomId());
        responseDto.setRoomType(RoomType.WAITING_ROOM);

        return new ResponseEntity<>(new Message("다음 대기열ID 반환 성공", responseDto), HttpStatus.OK);
    }

    public ResponseEntity<Message> getCurrentRoomId(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        String role = (String)claims.get("auth");

        Fan fan = userUtils.getFan(claims.getSubject());
        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        Optional<WaitRoomFan> waitRoomFan = waitRoomFanRepository.findByFanIdAndWaitRoomId(fan.getId(), fanMeetingId);
        Optional<TeleRoomFan> teleRoomFan = teleRoomFanRepository.findByFanIdAndTeleRoomId(fan.getId(), fanMeetingId);

        if (waitRoomFan.isPresent()) {
            String roomId = waitRoomFan.get().getWaitRoom().getRoomId();
            CurrRoomInfoResponseDto responseDto = CurrRoomInfoResponseDto.builder()
                    .roomId(roomId)
                    .build();

            return new ResponseEntity<>(new Message("현재 위치한 방(대기방)의 세션ID 반환 성공", responseDto), HttpStatus.OK);
        }

        else if (teleRoomFan.isPresent()) {
            String roomId = teleRoomFan.get().getTeleRoom().getRoomId();
            CurrRoomInfoResponseDto responseDto = CurrRoomInfoResponseDto.builder()
                    .roomId(roomId)
                    .build();

            return new ResponseEntity<>(new Message("현재 위치한 방(화상방)의 세션ID 반환 성공", responseDto), HttpStatus.OK);
        }

        else {
            throw new CustomException(FAN_NOT_IN_ROOM);
        }
    }

    // 팬미팅 조회 함수
    public ResponseEntity<Message> getFanMeeting(Long fanMeetingId, HttpServletRequest request) {
        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        FanMeeting fanMeeting = fanMeetingOpt.get();

        FanMeetingResponseDto responseDto = FanMeetingResponseDto.builder()
                .id(fanMeeting.getId())
                .imgUrl(fanMeeting.getFanMeetingImgUrl())
                .title(fanMeeting.getFanMeetingName())
                .startTime(fanMeeting.getStartTime())
                .endTime(fanMeeting.getEndTime())
                .chatRoomId(fanMeeting.getChatRoomId())
                .teamName(fanMeeting.getTeam().getTeamName())
                .build();

        return new ResponseEntity<>(new Message("팬미팅 조회 성공", responseDto), HttpStatus.OK);
    }


    // FanToFanMeeting 조회 함수: fan의 username과 fan_meeting의 id를 바탕으로 fan_to_fan_meeting을 조회
    public ResponseEntity<Message> getFanToFanMeeting(Long fanMeetingId, HttpServletRequest request) {

        claims = jwtUtil.getClaims(request);
        String username = claims.getSubject();

        Optional<Fan> fanOpt = fanRepository.findByUserCommonsUsername(username);
        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanOpt.isPresent()) {
            throw new CustomException(NOT_USER);
        }

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        Fan fan = fanOpt.get();
        FanMeeting fanMeeting = fanMeetingOpt.get();

        Optional<FanToFanMeeting> fanToFanMeetingOpt = fanToFanMeetingRepository.findByFanAndFanMeeting(fan, fanMeeting);

        if (!fanToFanMeetingOpt.isPresent()) {
            throw new CustomException(FAN_TO_FANMEETING_NOT_FOUND);
        }

        FanToFanMeeting fanToFanMeeting = fanToFanMeetingOpt.get();

        FanToFanMeetingResponseDto responseDto = FanToFanMeetingResponseDto.builder()
                .id(fanToFanMeeting.getId())
                .fanMeetingId(fanMeetingId)
                .fanId(fan.getId())
                .orderNumber(fanToFanMeeting.getOrderNumber())
                .fanMeetingApplyStatus(fanToFanMeeting.getFanMeetingApplyStatus())
                .chatRoomId(fanToFanMeeting.getChatRoomId())
                .teamName(fanMeeting.getTeam().getTeamName())
                .build();

        return new ResponseEntity<>(new Message("FanToFanMeeting 조회 성공", responseDto), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> getRoomsId(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);

        List<FanMeetingRoomOrder> roomOrders = fanMeetingRoomOrderRepository.findByFanMeetingId(fanMeetingId);

        List<String> result = new ArrayList<>();

        for (FanMeetingRoomOrder roomOrder : roomOrders) {
            result.add(roomOrder.getCurrentRoom());
        }

        return new ResponseEntity<>(new Message("방들의 세션ID 반환 성공", result), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> roomCreated(Long fanMeetingId, HttpServletRequest request) {
        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId).orElseThrow(() -> new CustomException(FANMEETING_NOT_FOUND));
        fanMeeting.setIsRoomsCreated(true);

        fanMeetingRepository.save(fanMeeting);
        return new ResponseEntity<>(new Message("관리자가 방 생성 완료", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> roomDeleted(Long fanMeetingId, HttpServletRequest request) {
        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId).orElseThrow(() -> new CustomException(FANMEETING_NOT_FOUND));
        fanMeeting.setIsRoomsCreated(false);

        fanMeetingRepository.save(fanMeeting);
        return new ResponseEntity<>(new Message("관리자가 방 삭제 완료", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> getMyFanMeetings(String option, HttpServletRequest request) {
        Claims claims = jwtUtil.getClaims(request);
        String username = claims.getSubject();
        Fan fan = userUtils.getFan(username);

        List<FanMeetingResponseDto> result = new ArrayList<>();
        List<FanToFanMeeting> fanToFanMeetings;

        if (option.equals(FanMeetingSearchOption.OPENED.value())) {
            fanToFanMeetings = fanToFanMeetingRepository.findFanToFanMeetingsByFanByStartTimeAfter(LocalDateTime.now(), fan);
        }
        else if (option.equals(FanMeetingSearchOption.CLOSED.value())) {
            fanToFanMeetings = fanToFanMeetingRepository.findFanToFanMeetingsByFanByEndTimeBefore(LocalDateTime.now(), fan);
        }
        else if (option.equals(FanMeetingSearchOption.PROGRESS.value())) {
            fanToFanMeetings = fanToFanMeetingRepository.findFanToFanMeetingsByFanByStartTimeBeforeAndEndTimeAfter(LocalDateTime.now(), fan);
        }

        else {
            fanToFanMeetings = fanToFanMeetingRepository.findAllByFan(fan);
        }

        for (FanToFanMeeting fanToFanMeeting : fanToFanMeetings) {
            FanMeeting fanMeeting = fanToFanMeeting.getFanMeeting();

            FanMeetingSearchOption status;

            if (fanMeeting.getStartTime().isAfter(LocalDateTime.now())) {
                status = FanMeetingSearchOption.OPENED;
            }
            else if (fanMeeting.getEndTime().isBefore(LocalDateTime.now())) {
                status = FanMeetingSearchOption.CLOSED;
            }
            else {
                status = FanMeetingSearchOption.PROGRESS;
            }
            FanMeetingResponseDto responseDto = FanMeetingResponseDto.builder()
                    .id(fanMeeting.getId())
                    .imgUrl(fanMeeting.getFanMeetingImgUrl())
                    .title(fanMeeting.getFanMeetingName())
                    .startTime(fanMeeting.getStartTime())
                    .endTime(fanMeeting.getEndTime())
                    .chatRoomId(fanMeeting.getChatRoomId())
                    .teamName(fanMeeting.getTeam().getTeamName())
                    .fanMeetingStatus(status)
                    .build();

            result.add(responseDto);
        }

        return new ResponseEntity<>(new Message("팬의 팬미팅 조회 성공", result), HttpStatus.OK);

    }

    @Transactional
    public ResponseEntity<Message> startFanMeeting(Long fanMeetingId, HttpServletRequest request) {
        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId).orElseThrow(() -> new CustomException(FANMEETING_NOT_FOUND));
        fanMeeting.setIsStarted(true);
        fanMeetingRepository.save(fanMeeting);

        FanMeetingResponseDto responseDto = FanMeetingResponseDto.builder()
                .id(fanMeeting.getId())
                .imgUrl(fanMeeting.getFanMeetingImgUrl())
                .title(fanMeeting.getFanMeetingName())
                .startTime(fanMeeting.getStartTime())
                .endTime(fanMeeting.getEndTime())
                .chatRoomId(fanMeeting.getChatRoomId())
                .teamName(fanMeeting.getTeam().getTeamName())
                .build();

        return new ResponseEntity<>(new Message("팬미팅 시작 성공", responseDto), HttpStatus.OK);
    }


    public ResponseEntity<Message> closeFanMeeting(Long fanMeetingId, HttpServletRequest request) {
        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId).orElseThrow(() -> new CustomException(FANMEETING_NOT_FOUND));
        fanMeeting.setIsStarted(false);
        fanMeetingRepository.save(fanMeeting);

        FanMeetingResponseDto responseDto = FanMeetingResponseDto.builder()
                .id(fanMeeting.getId())
                .imgUrl(fanMeeting.getFanMeetingImgUrl())
                .title(fanMeeting.getFanMeetingName())
                .startTime(fanMeeting.getStartTime())
                .endTime(fanMeeting.getEndTime())
                .chatRoomId(fanMeeting.getChatRoomId())
                .teamName(fanMeeting.getTeam().getTeamName())
                .build();

        return new ResponseEntity<>(new Message("팬미팅 시작 성공", responseDto), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> saveGameScore(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        String username = claims.getSubject();
        Fan fan = userUtils.getFan(username);

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        FanMeeting fanMeeting = fanMeetingOpt.get();

        Optional<FanToFanMeeting> fanToFanMeetingOpt = fanToFanMeetingRepository.findByFanAndFanMeeting(fan, fanMeeting);

        if (!fanToFanMeetingOpt.isPresent()) {
            throw new CustomException(FAN_TO_FANMEETING_NOT_FOUND);
        }

        FanToFanMeeting fanToFanMeeting = fanToFanMeetingOpt.get();

        fanToFanMeeting.setGameScore(fanToFanMeeting.getGameScore() + 1);
        fanToFanMeetingRepository.save(fanToFanMeeting);

        return new ResponseEntity<>(new Message("게임 점수 저장 성공", null), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> getGameScore(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        String username = claims.getSubject();
        Fan fan = userUtils.getFan(username);

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        FanMeeting fanMeeting = fanMeetingOpt.get();

        Optional<FanToFanMeeting> fanToFanMeetingOpt = fanToFanMeetingRepository.findByFanAndFanMeeting(fan, fanMeeting);

        if (!fanToFanMeetingOpt.isPresent()) {
            throw new CustomException(FAN_TO_FANMEETING_NOT_FOUND);
        }

        FanToFanMeeting fanToFanMeeting = fanToFanMeetingOpt.get();

        Long gameScore = fanToFanMeeting.getGameScore();

        return new ResponseEntity<>(new Message("게임 점수 불러오기 성공", gameScore), HttpStatus.OK);

    }

    public ResponseEntity<Message> getGameRoomSessionId(Long fanMeetingId, HttpServletRequest request) {
        Optional<FanMeetingRoomOrder> gameRoomOpt = fanMeetingRoomOrderRepository.getGameRoomFindByFanMeetingId(fanMeetingId);

        if (!gameRoomOpt.isPresent()) {
            throw new CustomException(NOT_FOUND_FANMEETING_ROOM_ORDER);
        }

        FanMeetingRoomOrder gameRoom = gameRoomOpt.get();

        return new ResponseEntity<>(new Message("게임방 세션 아이디 불러오기 성공", gameRoom.getCurrentRoom()), HttpStatus.OK);
    }
}
