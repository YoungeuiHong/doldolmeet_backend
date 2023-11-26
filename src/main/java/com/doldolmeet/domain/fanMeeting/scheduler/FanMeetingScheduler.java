package com.doldolmeet.domain.fanMeeting.scheduler;


import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.entity.FanMeetingRoomOrder;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.fanMeeting.sse.SseService;
import com.doldolmeet.domain.fanMeeting.sse.UserNameAndOrderNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FanMeetingScheduler {
    private final FanMeetingRepository fanMeetingRepository;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 예제를 위해 간단하게 맵에 팬미팅 시작시간과 특정 로직을 설정
//    private static final Runnable fanMeetingTask = (FanMeeting fanMeeting) -> {
//        System.out.println("FanMeeting started! Perform specific logic.");
//        // 여기에 팬미팅에 대한 특정 로직을 추가하세요.
//    };

    @Scheduled(fixedRate = 60 * 1000) // 매 분마다 실행
    @Transactional
    public void checkFanMeetingStartTime() throws IOException {
        // 오늘 열려있는 팬미팅들 조회

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime midNightTime = currentTime.with(LocalTime.MIN);
        LocalDateTime tomorrowMidNightTime = midNightTime.plusDays(1);

        List<FanMeeting> fanMeetings = fanMeetingRepository.findTodayFanMeetings(midNightTime, tomorrowMidNightTime);

        log.info("현재 시간: " + currentTime);

        // 오늘 열리는 팬미팅들 순회.
        for (FanMeeting fanMeeting : fanMeetings) {
            // 시작해야 하는 팬미팅들 순회.
            if (fanMeeting.getStartTime().isBefore(currentTime)) {
                log.info("FanMeeting start time: " + fanMeeting.getStartTime());

                // 관리자가 방 생성하지 않았으면 스케쥴링 안함.
                if (!fanMeeting.getIsRoomsCreated()) {
                    log.info(fanMeeting.getFanMeetingName() + "아직 관리자가 방 생성 안함.");
                }

                // 관리자가 방 생성한 경우,
                else {

                    // 아직 팬이 메인 대기방 안들어왔으면 스케쥴링 안함.
                    if (SseService.waitingRooms.get(fanMeeting.getId()) == null) {
                        log.info(fanMeeting.getFanMeetingName() + "아직 팬이 안 들어와서 대기방 생성도 안됨");
                        continue;
                    }

                    FanMeetingRoomOrder roomOrder = fanMeeting.getFanMeetingRoomOrders().get(0); // TODO: NullPointerException
                    String mainRoomId = roomOrder.getCurrentRoom();

                    // 팬이 들어와서 메인 대기방 자료구조는 생성되었는데, 아무도 없으면 스케쥴링 안함.
                    if (SseService.waitingRooms.get(fanMeeting.getId()).get(mainRoomId).isEmpty()) {
                        log.info(fanMeeting.getFanMeetingName() + "메인 대기방에 아무도 없음.");
                        continue;
                    }

                    UserNameAndOrderNumber userInfo = SseService.waitingRooms.get(fanMeeting.getId()).get(mainRoomId).first();

                    String username = userInfo.getUsername();
                    SseEmitter emitter = SseService.emitters.get(fanMeeting.getId()).get(username);

                    Map<String, String> params = new HashMap<>();
                    params.put("nextRoomId", roomOrder.getNextRoom());
                    params.put("currRoomType", roomOrder.getType());

                    try {
                        emitter.send(SseEmitter.event().name("moveToFirstIdolWaitRoom").data(params));
                        // 쏘고 나면, 클라이언트에서 이 이벤트를 받아 처리한다.(화면 전환 + 해당 세션에 입장)
                        // 입장시, joined 이벤트 발생 -> 웹훅 -> 대기방에 추가됨.

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    log.info(fanMeeting.getFanMeetingName() + "스케쥴링 완료");
                }
            } else {
                log.info("FanMeeting start time: " + fanMeeting.getStartTime() + "아직 시작 안함.");
            }
        }
    }
}
