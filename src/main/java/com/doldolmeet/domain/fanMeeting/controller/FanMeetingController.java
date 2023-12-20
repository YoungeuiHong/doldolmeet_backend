package com.doldolmeet.domain.fanMeeting.controller;

import com.doldolmeet.domain.fanMeeting.dto.request.FanMeetingRequestDto;
import com.doldolmeet.domain.fanMeeting.service.FanMeetingService;
import com.doldolmeet.utils.Message;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@ApiResponse
public class FanMeetingController {
    private final FanMeetingService fanMeetingService;

    // 팬미팅 생성 API
    @PostMapping("/fanMeetings")
    public ResponseEntity<Message> createFanMeeting(@RequestBody FanMeetingRequestDto requestDto, HttpServletRequest request) {
        return fanMeetingService.createFanMeeting(requestDto, request);
    }

    // option에 해당하는 모든 팬미팅 조회(로그인 안한 사람도 가능)
    @GetMapping("/fanMeetings")
    @ResponseBody
    public ResponseEntity<Message> getFanMeetings(@RequestParam String option) {
        return fanMeetingService.getFanMeetings(option);
    }

    // 팬이 신청했던 팬미팅 중 option에 따라 조회
    @GetMapping("/fanMeetings/my")
    public ResponseEntity<Message> getMyFanMeetings(@RequestParam String option, HttpServletRequest request) {
        return fanMeetingService.getMyFanMeetings(option, request);
    }

    // 해당 팬미팅 신청 API
    @PostMapping("/fanMeetings/{fanMeetingId}")
    public ResponseEntity<Message> applyFanMeeting(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingService.applyFanMeeting(fanMeetingId, request);
    }

    // 내가 신청한 팬미팅 중 예정된 가장 첫번째 팬미팅
    @GetMapping("/fanMeetings/today")
    public ResponseEntity<Message> getMyTodayFanMeeting(HttpServletRequest request) {
        return fanMeetingService.getMyTodayFanMeeting(request);
    }

    // 팬미팅 들어갈 수 있는지 유무
    @GetMapping("/fanMeetings/{fanMeetingId}/canEnter")
    public ResponseEntity<Message> canEnterFanMeeting(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingService.canEnterFanMeeting(fanMeetingId, request);
    }

    // 팬미팅의 가장 첫번째 대기방의 roomId 가져오는 API
    @GetMapping("/fanMeetings/{fanMeetingId}/mainWaitRoom")
    public ResponseEntity<Message> getMainWaitRoom(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingService.getMainWaitRoom(fanMeetingId, request);
    }

    // 아이돌 입장에서 다음에 들어올 팬 데이터 가져오는 API
    @GetMapping("/fanMeetings/{fanMeetingId}/nextFan")
    public ResponseEntity<Message> getNextFan(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return fanMeetingService.getNextFan(fanMeetingId, request);
    }

    // 아이돌 입장에서 자기 방에 있는 팬 다음 방으로 가라는 API
    @GetMapping("/fanMeetings/{fanMeetingId}/nextWaitRoom")
    public ResponseEntity<Message> getNextWaitRoomId(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return fanMeetingService.getNextWaitRoomId(fanMeetingId, request);
    }

    // 현재 자신이 위치한 방의 sessionId 반환 API
    @GetMapping("/fanMeetings/{fanMeetingId}/currentRoom")
    public ResponseEntity<Message> getCurrentRoomId(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return fanMeetingService.getCurrentRoomId(fanMeetingId, request);
    }

    // 팬미팅 조회 API
    @GetMapping("/fanMeetings/{fanMeetingId}")
    public ResponseEntity<Message> getFanMeeting(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingService.getFanMeeting(fanMeetingId, request);
    }
    // 해당 팬미팅의 모든 방ID 반환 API
    @GetMapping("/fanMeetings/{fanMeetingId}/roomsId")
    public ResponseEntity<Message> getRoomsId(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return fanMeetingService.getRoomsId(fanMeetingId, request);
    }

    // fan_to_fan_meeting 조회 API
    @GetMapping("/fanMeetings/{fanMeetingId}/fanToFanMeeting")
    public ResponseEntity<Message> getFanToFanMeeting(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingService.getFanToFanMeeting(fanMeetingId, request);
    }
    // 해당 팬미팅 방 생성되었다고 알려주는 API
    @PostMapping("/fanMeetings/{fanMeetingId}/roomCreated")
    public ResponseEntity<Message> roomCreated(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return fanMeetingService.roomCreated(fanMeetingId, request);
    }

    // 해당 팬미팅 방 삭제되었다고 알려주는 API
    @PostMapping("/fanMeetings/{fanMeetingId}/roomDeleted")
    public ResponseEntity<Message> roomDeleted(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return fanMeetingService.roomDeleted(fanMeetingId, request);
    }

    @Operation(summary = "팬미팅 시작", description = "팬미팅 시작")
    // 관리자가 팬미팅 시작하는 API
    @PostMapping("/fanMeetings/{fanMeetingId}/start")
    public ResponseEntity<Message> startFanMeeting(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return fanMeetingService.startFanMeeting(fanMeetingId, request);
    }

    // 관리자가 팬미팅 종료하는 API
    @PostMapping("/fanMeetings/{fanMeetingId}/close")
    public ResponseEntity<Message> closeFanMeeting(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return fanMeetingService.closeFanMeeting(fanMeetingId, request);
    }

    // 게임 점수 저장 API
    @PostMapping("/fanMeetings/{fanMeetingId}/gameScore")
    public ResponseEntity<Message> saveGameScore(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingService.saveGameScore(fanMeetingId, request);
    }

    // 게임 점수 불러오기 API
    @GetMapping("/fanMeetings/{fanMeetingId}/gameScore")
    public ResponseEntity<Message> getGameScore(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingService.getGameScore(fanMeetingId, request);
    }

    /**
     * 팬미팅 아이디를 받아서 해당 팬미팅의 게임방 세션 아이디를 리턴하는 API
     */
    @GetMapping("/fanMeetings/{fanMeetingId}/get-game-room-id")
    public ResponseEntity<Message> getGameRoomId(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingService.getGameRoomSessionId(fanMeetingId, request);
    }
}
