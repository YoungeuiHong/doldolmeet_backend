package com.doldolmeet.domain.openvidu.controller;

import java.util.HashMap;
import java.util.Map;

import com.doldolmeet.domain.openvidu.dto.request.ConnUpdateRequestDto;
import com.doldolmeet.domain.openvidu.service.OpenviduService;
import com.doldolmeet.utils.Message;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.PostConstruct;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.openvidu.java.client.Connection;
import io.openvidu.java.client.ConnectionProperties;
import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.Session;
import io.openvidu.java.client.SessionProperties;

//@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@ApiResponse
public class OpenviduController {
    private final OpenviduService openviduService;

    /**
     * @param params The Session properties
     * @return The Session ID
     */

    // 화상통화방을 만드는 API
    @PostMapping("/api/sessions/{fanMeetingId}")
    public ResponseEntity<String> initializeSession(@RequestBody(required = false) Map<String, Object> params, @PathVariable Long fanMeetingId)
            throws OpenViduJavaClientException, OpenViduHttpException {
        return openviduService.initializeSession(params, fanMeetingId);
    }

    /**
     * @param sessionId The Session in which to create the Connection
     * @param params    The Connection properties
     * @return The Token associated to the Connection
     */

    // 화상통화방<->유저간 커넥션 생성 API
    @PostMapping("/api/sessions/{sessionId}/connections")
    public ResponseEntity<String> createConnection(@PathVariable("sessionId") String sessionId,
                                                   @RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        return openviduService.createConnection(sessionId, params);
    }

    // 팬미팅 입장버튼 API(팬, 아이돌 로직 분기처리됨)
    @GetMapping("/fanMeetings/{fanMeetingId}/session")
    public ResponseEntity<Message> enterFanMeeting(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return openviduService.enterFanMeeting(fanMeetingId, request);
    }

    // AWS 로드밸런서 헬스체크용 API
    @GetMapping("/health-check")
    public String healthCheck() {
        return "OK";
    }

    //들어간 팬을 대기 방에서 삭제하는 API
    @PostMapping("/idolName/{idolName}/deleteFanParticipated")
    public ResponseEntity<Message> deleteFanParticipatedWait(@PathVariable String idolName, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return openviduService.deleteWaitRoomFan(idolName, request);
    }

    @PostMapping("/idolName/{idolName}/deleteFanParticipated/Tele")
    public ResponseEntity<Message> deleteFanParticipatedTele(@PathVariable String idolName, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return openviduService.deleteTeleRoomFan(idolName, request);
    }

    //대기방에 들어온 팬을 저장하는 API
    @PostMapping("/username/{username}/waitRoomId/{waitRoomId}/fanMeetingId/{fanMeetingId}/saveFanWaiting")
    public ResponseEntity<Message> saveFanWaiting(@PathVariable String waitRoomId, @PathVariable Long fanMeetingId, @PathVariable String username) throws OpenViduJavaClientException, OpenViduHttpException {
        return openviduService.saveFanWaiting(waitRoomId, fanMeetingId, username);
    }

    //화상방에 들어온 팬을 저장하는 API
    @PostMapping("/username/{username}/teleRoomId/{teleRoomId}/fanMeetingId/{fanMeetingId}/saveFanTeleing")
    public ResponseEntity<Message> saveFanTeleing(@PathVariable String teleRoomId, @PathVariable Long fanMeetingId, @PathVariable String username) throws OpenViduJavaClientException, OpenViduHttpException {
        return openviduService.saveFanTeleing(teleRoomId, fanMeetingId, username);
    }

    // TeleRoomFan과 WaitRoomFan을 찾아서 커넥션Id와 커넥션토큰을 저장해야 한다.
    @PostMapping("/updateConnection")
    public ResponseEntity<Message> updateConnection(@RequestBody ConnUpdateRequestDto requestDto, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        return openviduService.updateConnection(requestDto, request);
    }

    // 팬미팅 방 생성 API
    @PostMapping("/fanMeetings/{fanMeetingId}/rooms")
    public ResponseEntity<Message> createFanMeetingRooms(@PathVariable Long fanMeetingId, HttpServletRequest request) throws OpenViduJavaClientException, OpenViduHttpException{
        return openviduService.createFanMeetingRooms(fanMeetingId, request);
    }
}