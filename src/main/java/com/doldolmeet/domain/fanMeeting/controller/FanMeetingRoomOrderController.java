package com.doldolmeet.domain.fanMeeting.controller;

import com.doldolmeet.domain.fanMeeting.service.FanMeetingRoomOrderService;
import com.doldolmeet.utils.Message;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@ApiResponse
@RequestMapping("/roomOrder")
public class FanMeetingRoomOrderController {
    private final FanMeetingRoomOrderService fanMeetingRoomOrderService;

    @GetMapping("/all/{fanMeetingId}")
    public ResponseEntity<Message> getAllFanMeetingRoomOrder(@PathVariable Long fanMeetingId) {
        return fanMeetingRoomOrderService.getAllFanMeetingRoomOrder(fanMeetingId);
    }

    // fanMeetingId, roomType과 token안의 idol정보를 바탕으로 fanMeetingRoomOrder조회
    @GetMapping("/{fanMeetingId}")
    public ResponseEntity<Message> getFanMeetingRoomOrder(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return fanMeetingRoomOrderService.getFanMeetingRoomOrder(fanMeetingId, request);
    }

    @GetMapping("/idolRoom/{fanMeetingId}")
    public ResponseEntity<Message> getAllIdolRoom(@PathVariable Long fanMeetingId) {
        return fanMeetingRoomOrderService.getAllIdolRoom(fanMeetingId);
    }

}
