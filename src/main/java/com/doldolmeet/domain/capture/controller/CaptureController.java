package com.doldolmeet.domain.capture.controller;

import com.doldolmeet.domain.capture.service.CaptureService;
import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.utils.Message;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/captures")
@ApiResponse
public class CaptureController {
    private final CaptureService captureService;

    // Capture Upload to S3
    @PostMapping("/upload/{fanMeetingId}/{nickname}")
    public ResponseEntity<Message> uploadCapture(@PathVariable Long fanMeetingId, @PathVariable String nickname, @RequestPart MultipartFile file, HttpServletRequest request) {
        return captureService.uploadCapture(fanMeetingId, nickname, file, request);
    }

    // fan이 찍은 모든 capture 조회
    @GetMapping("/")
    public ResponseEntity<Message> getAllCapture(HttpServletRequest request) {
        return captureService.getAllCapture(request);
    }

    // fan이 fanMeeting에서 찍은 모든 capture 조회
    @GetMapping("/{fanMeetingId}")
    public ResponseEntity<Message> getCaptrueFanMeeting(@PathVariable Long fanMeetingId, HttpServletRequest request) {
        return captureService.getCaptureFanMeeting(fanMeetingId, request);
    }

    // fan이 fanMeeting에서 Idol과 찍은 모든 capture 조회
    @GetMapping("/{fanMeetingId}/{idolId}")
    public ResponseEntity<Message> getCaptureIdol(@PathVariable Long fanMeetingId, @PathVariable Long idolId, HttpServletRequest request) {
        return captureService.getCaptureIdol(fanMeetingId, idolId, request);
    }
}
