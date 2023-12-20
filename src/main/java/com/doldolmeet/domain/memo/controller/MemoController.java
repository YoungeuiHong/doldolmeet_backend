package com.doldolmeet.domain.memo.controller;

import com.doldolmeet.domain.memo.dto.MemoRequestDto;
import com.doldolmeet.domain.memo.dto.MemoResponseDto;
import com.doldolmeet.domain.memo.service.MemoService;
import com.doldolmeet.utils.Message;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// MemoController.java

@RestController
@RequiredArgsConstructor
@ApiResponse
public class MemoController {
    private final MemoService memoService;

    // 메모 생성
    @PostMapping("/memos")
    public ResponseEntity<Message> createMemo(@RequestBody @Valid MemoRequestDto requestDto, HttpServletRequest request) {
        return memoService.createMemo(requestDto, request);
    }

    // 최신순으로 정렬된 모든 메모 조회
    @GetMapping("/memos")
    public ResponseEntity<List<MemoResponseDto>> getAllMemos() {
        return memoService.getAllMemos();
    }
    // 해당 fan이 작성한 전체 메모 조회 필요하면 만들어야 함

    // 현재 접속 중인 fan의 전체 메모 조회
    @GetMapping("/memos/my")
    public ResponseEntity<List<MemoResponseDto>> getMyMemos(HttpServletRequest request) {
        return memoService.getMyMemos(request);
    }

    // 선택한 메모 조회
    @GetMapping("/memos/{id}")
    public ResponseEntity<?> getMemo(@PathVariable Long id) {
        return memoService.getMemo(id);
    }

    // 메모 수정
    @PutMapping("/memos/{id}")
    public ResponseEntity<Message> updateMemo(@PathVariable Long id, @RequestBody @Valid MemoRequestDto requestDto) {
        return memoService.updateMemo(id, requestDto);
    }

    // 메모 삭제
    @DeleteMapping("/memos/{id}")
    public ResponseEntity<Message> deleteMemo(@PathVariable Long id) {
        return memoService.deleteMemo(id);
    }
}