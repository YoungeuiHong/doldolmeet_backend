package com.doldolmeet.domain.fanMeeting.service;

import com.doldolmeet.domain.fanMeeting.dto.response.FanMeetingRoomOrderDto;
import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.entity.FanMeetingRoomOrder;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRoomOrderRepository;
import com.doldolmeet.domain.users.idol.entity.Idol;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.doldolmeet.exception.ErrorCode.FANMEETING_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class FanMeetingRoomOrderService {
    private final FanMeetingRepository fanMeetingRepository;
    private final FanMeetingRoomOrderRepository fanMeetingRoomOrderRepository;
    private final JwtUtil jwtUtil;
    private final UserUtils userUtils;
    private Claims claims;

    // fanMeetingId, roomType과 token안의 idol정보를 바탕으로 fanMeetingRoomOrder조회
    public ResponseEntity<Message> getFanMeetingRoomOrder(Long fanMeetingId, HttpServletRequest request) {
        claims = jwtUtil.getClaims(request);
        Idol idol = userUtils.getIdol(claims.getSubject());

        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (!fanMeetingOpt.isPresent()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        Optional<FanMeetingRoomOrder> idolRoomOrders = fanMeetingRoomOrderRepository
                .findByFanMeetingIdAndTypeAndNickname(fanMeetingId, "idolRoom", idol.getUserCommons().getNickname());

        FanMeetingRoomOrder fanMeetingRoomOrder = idolRoomOrders.get();

        FanMeetingRoomOrderDto responseDto = FanMeetingRoomOrderDto.builder()
                .id(fanMeetingRoomOrder.getId())
                .motionType(fanMeetingRoomOrder.getMotionType())
                .currentRoom(fanMeetingRoomOrder.getCurrentRoom())
                .idolName(fanMeetingRoomOrder.getNickname())
                .type(fanMeetingRoomOrder.getType())
                .fanMeetingId(fanMeetingId)
                .build();

        return new ResponseEntity<>(new Message("roomOrders 반환 성공!!", responseDto), HttpStatus.OK);
    }

    public ResponseEntity<Message> getAllFanMeetingRoomOrder(Long fanMeetingId) {
        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (fanMeetingOpt.isEmpty()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        List<FanMeetingRoomOrder> idolRoomOrders = fanMeetingRoomOrderRepository.findByFanMeetingId(fanMeetingId);

        List<FanMeetingRoomOrderDto> fanMeetingRoomOrderDtos = idolRoomOrders.stream()
                .map(order -> FanMeetingRoomOrderDto.builder()
                        .id(order.getId())
                        .motionType(order.getMotionType())
                        .currentRoom(order.getCurrentRoom())
                        .idolName(order.getNickname())
                        .type(order.getType())
                        .fanMeetingId(fanMeetingId)
                        .build())
                .collect(Collectors.toList());

        return new ResponseEntity<>(new Message("List<roomOrders> 반환 성공!!", fanMeetingRoomOrderDtos), HttpStatus.OK);
    }

    public ResponseEntity<Message> getAllIdolRoom(Long fanMeetingId) {
        Optional<FanMeeting> fanMeetingOpt = fanMeetingRepository.findById(fanMeetingId);

        if (fanMeetingOpt.isEmpty()) {
            throw new CustomException(FANMEETING_NOT_FOUND);
        }

        List<FanMeetingRoomOrder> idolRoomOrders = fanMeetingRoomOrderRepository
                .findByFanMeetingIdAndType(fanMeetingId, "idolRoom");

        List<FanMeetingRoomOrderDto> fanMeetingRoomOrderDtos = idolRoomOrders.stream()
                .map(order -> FanMeetingRoomOrderDto.builder()
                        .id(order.getId())
                        .motionType(order.getMotionType())
                        .currentRoom(order.getCurrentRoom())
                        .idolName(order.getNickname())
                        .type(order.getType())
                        .fanMeetingId(fanMeetingId)
                        .build())
                .collect(Collectors.toList());

        return new ResponseEntity<>(new Message("List<idolRoom> 반환 성공!!", fanMeetingRoomOrderDtos), HttpStatus.OK);
    }
}
