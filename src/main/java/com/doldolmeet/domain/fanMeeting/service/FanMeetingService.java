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
                .nextOrder(1L)
                .build();


        List<Idol> idols = team.get().getIdols();

        int sz = idols.size() * 2;

        // 메인 대기방 생성
        FanMeetingRoomOrder roomOrder;
        roomOrder = FanMeetingRoomOrder.builder()
                .currentRoom(UUID.randomUUID().toString())
                .nextRoom(null)
                .fanMeeting(fanMeeting)
                .nickname("main")
                .type("mainWaitRoom")
                .build();

        fanMeeting.getFanMeetingRoomOrders().add(roomOrder);

        for (int i = 0; i < sz; i++) {
            if (i == sz - 1) {
                roomOrder = FanMeetingRoomOrder.builder()
                        .currentRoom(UUID.randomUUID().toString())
                        .nextRoom("END")
                        .fanMeeting(fanMeeting)
                        .nickname(idols.get(i/2).getUserCommons().getNickname())
                        .type("teleRoom")
                        .build();
                fanMeeting.getFanMeetingRoomOrders().get(i).setNextRoom(roomOrder.getCurrentRoom());

            } else {
                String myRoomId = UUID.randomUUID().toString();;

                roomOrder = FanMeetingRoomOrder.builder()
                        .currentRoom(myRoomId)
                        .nextRoom(null)
                        .fanMeeting(fanMeeting)
                        .nickname(idols.get(i/2).getUserCommons().getNickname())
                        .type(i % 2 == 0 ? "waitRoom" : "teleRoom")
                        .build();

                fanMeeting.getFanMeetingRoomOrders().get(i).setNextRoom(myRoomId);
            }

            fanMeeting.getFanMeetingRoomOrders().add(roomOrder);
        }

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

        FanToFanMeeting fanToFanMeeting = FanToFanMeeting.builder()
                .fanMeetingApplyStatus(FanMeetingApplyStatus.APPROVED)
                .fan(fan)
                .fanMeeting(fanMeeting)
                .orderNumber(fanMeeting.getNextOrder())
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
                .build();

        return new ResponseEntity<>(new Message("팬미팅 신청 성공", responseDto), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Message> getMyTodayFanMeeting(HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Optional<Fan> fan = fanRepository.findByUserCommonsUsername(claims.getSubject());
        Optional<Idol> idol = idolRepository.findByUserCommonsUsername(claims.getSubject());

        Optional<FanMeeting> fanMeetingOpt;

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime midNightTime = currentTime.with(LocalTime.MIN);
        LocalDateTime tomorrowMidNightTime = midNightTime.plusDays(1);

        log.info("현재시간: " + currentTime);
        log.info("자정시간: " + midNightTime);

        if (fan.isPresent()) {
            fanMeetingOpt = fanMeetingRepository.findFanMeetingsByFan(fan.get(), midNightTime, currentTime, tomorrowMidNightTime);
        }

        else if (idol.isPresent()) {
            fanMeetingOpt = fanMeetingRepository.findFanMeetingsByTeamOne(idol.get().getTeam(), midNightTime, currentTime, tomorrowMidNightTime);
//            fanMeetingOpt = fanMeetingRepository.findFanMeetingsByIdol(idol.get(), midNightTime, currentTime);
        }

        else {
            throw new CustomException(USER_NOT_FOUND);
        }

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
                .build();

        return new ResponseEntity<>(new Message("나의 예정된 팬미팅 중 가장 최신 팬미팅 받기 성공", responseDto), HttpStatus.OK);
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

//    @Transactional
//    public ResponseEntity<Message> createFanMeetingRooms(Long fanMeetingId, HttpServletRequest request) {
//        // 어드민인지 체크
//        claims = jwtUtil.getClaims(request);
//        userUtils.checkIfAdmin(claims);
//
//        FanMeeting fanMeeting = fanMeetingRepository.findById(fanMeetingId).orElseThrow(() -> new CustomException(FANMEETING_NOT_FOUND));
//
//        // 팬미팅에서 팀을 뽑아낸 후,팀의 아이돌을 뽑아낸 후, FanMeetingRoomOrder 생성
//        Team team = fanMeeting.getTeam();
//        List<Idol> idols = team.getIdols();
//
//        int sz = idols.size() * 2;
//
//        for (int i = 0; i < sz; i++) {
//            FanMeetingRoomOrder roomOrder;
//            if (i == sz - 1) {
//                roomOrder = FanMeetingRoomOrder.builder()
//                        .currentRoom(UUID.randomUUID().toString())
//                        .nextRoom("END")
//                        .fanMeeting(fanMeeting)
//                        .build();
//
//            } else {
//                roomOrder = FanMeetingRoomOrder.builder()
//                        .currentRoom(UUID.randomUUID().toString())
//                        .nextRoom(UUID.randomUUID().toString())
//                        .fanMeeting(fanMeeting)
//                        .build();
//            }
//
//            fanMeetingRoomOrderRepository.save(roomOrder);
//        }
//
//        return new ResponseEntity<>(new Message("팬미팅에 대해 각 통화방과 대기방 생성 성공", null), HttpStatus.OK);
//    }
}
