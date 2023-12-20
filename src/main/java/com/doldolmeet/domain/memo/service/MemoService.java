package com.doldolmeet.domain.memo.service;


import com.doldolmeet.domain.memo.dto.MemoRequestDto;
import com.doldolmeet.domain.memo.dto.MemoResponseDto;
import com.doldolmeet.domain.memo.entity.Memo;
import com.doldolmeet.domain.memo.repository.MemoRepository;
import com.doldolmeet.domain.users.fan.entity.Fan;
import com.doldolmeet.security.jwt.JwtUtil;
import com.doldolmeet.utils.Message;
import com.doldolmeet.utils.UserUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoRepository memoRepository;
    private final JwtUtil jwtUtil;
    private final UserUtils userUtils;
    private Claims claims;
    private Fan fan;

    // 메모 생성
    @Transactional
    public ResponseEntity<Message> createMemo(MemoRequestDto requestDto, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        fan = userUtils.getFan(claims.getSubject());

        Memo memo = memoRepository.saveAndFlush(new Memo(requestDto, fan));
        MemoResponseDto responseDto = new MemoResponseDto(memo.getId(), memo.getContents(), memo.getCreatedAt());
        return new ResponseEntity<>(new Message("메모 생성 성공", responseDto), HttpStatus.CREATED);
    }

    // 전체 메모 조회
    @Transactional(readOnly = true)
    public ResponseEntity<List<MemoResponseDto>> getAllMemos() {
        List<Memo> memos = memoRepository.findAllByOrderByCreatedAtDesc();

        List<MemoResponseDto> responseDtos = memos.stream()
                .map(memo -> new MemoResponseDto(memo.getId(), memo.getContents(), memo.getCreatedAt()))
                .collect(Collectors.toList());

        return new ResponseEntity<>(responseDtos, HttpStatus.OK);
    }

    // 해당 fan이 작성한 메모 조회기능도 필요하면 구현해야 함

    // 현재 접속 중인 fan의 전체 메모 조회
    @Transactional(readOnly = true)
    public ResponseEntity<List<MemoResponseDto>> getMyMemos(HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        fan = userUtils.getFan(claims.getSubject());

        List<Memo> myMemos = memoRepository.findByFanOrderByCreatedAtAsc(fan);

        List<MemoResponseDto> responseDtos = myMemos.stream()
                .map(memo -> new MemoResponseDto(memo.getId(), memo.getContents(), memo.getCreatedAt()))
                .collect(Collectors.toList());

        return new ResponseEntity<>(responseDtos, HttpStatus.OK);
    }

    // 선택한 메모 조회(memo_id를 통해서)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMemo(Long id) {
        Optional<Memo> optionalMemo = memoRepository.findById(id);

        if (optionalMemo.isPresent()) {
            Memo memo = optionalMemo.get();
            MemoResponseDto responseDto = new MemoResponseDto(memo.getId(), memo.getContents(), memo.getCreatedAt());
            return new ResponseEntity<>(responseDto, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new Message("메모를 찾을 수 없습니다.", null), HttpStatus.NOT_FOUND);
        }
    }

    // 메모 수정
    @Transactional
    public ResponseEntity<Message> updateMemo(Long id, MemoRequestDto requestDto) {
        Optional<Memo> optionalMemo = memoRepository.findById(id);

        if (optionalMemo.isPresent()) {
            Memo memo = optionalMemo.get();
            memo.update(requestDto);
            memoRepository.save(memo);
            MemoResponseDto responseDto = new MemoResponseDto(memo.getId(), memo.getContents(), memo.getCreatedAt());
            return new ResponseEntity<>(new Message("메모 업데이트 성공", responseDto), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new Message("메모를 찾을 수 없습니다.", null), HttpStatus.NOT_FOUND);
        }
    }

    // 메모 삭제
    @Transactional
    public ResponseEntity<Message> deleteMemo(Long id) {
        Optional<Memo> optionalMemo = memoRepository.findById(id);

        if (optionalMemo.isPresent()) {
            memoRepository.deleteById(id);
            return new ResponseEntity<>(new Message("메모 삭제 성공", null), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new Message("메모를 찾을 수 없습니다.", null), HttpStatus.NOT_FOUND);
        }
    }
}
