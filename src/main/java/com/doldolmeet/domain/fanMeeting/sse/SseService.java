package com.doldolmeet.domain.fanMeeting.sse;

import com.doldolmeet.domain.fanMeeting.entity.FanMeeting;
import com.doldolmeet.domain.fanMeeting.entity.FanToFanMeeting;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRepository;
import com.doldolmeet.domain.fanMeeting.repository.FanMeetingRoomOrderRepository;
import com.doldolmeet.domain.fanMeeting.repository.FanToFanMeetingRepository;
import com.doldolmeet.exception.CustomException;
import io.openvidu.java.client.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.doldolmeet.exception.ErrorCode.NOT_FOUND_FANTOFANMEETING;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {
    private final FanToFanMeetingRepository fanToFanMeetingRepository;
    private final FanMeetingRoomOrderRepository fanMeetingRoomOrderRepository;
    private final FanMeetingRepository fanMeetingRepository;

    // 팬미팅ID, 유저이름, 에미터
    public static Map<Long, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public static Map<Long, Map<String, SortedSet<UserNameAndOrderNumber>>> waitingRooms = new ConcurrentHashMap<>();

    public static Map<Long, Map<String, Session>> Rooms = new ConcurrentHashMap<>();

    // 팬미팅ID, 아이돌username, 아이돌이 팬미팅에 들어왔는지 여부
    public static Map<Long, Map<String, Boolean>> isIdolsEntered = new ConcurrentHashMap<>();

    // 팬미팅ID, 해당 방에 들어 있는 팬들의 username
    public static Map<Long, List<String>> gameRooms = new ConcurrentHashMap<>();

    // username을 통해서 event 리스트 찾고, lastEventId로 그 이후 이벤트들만 다시 보내기
    public static Map<String, List<SseEvent>> sseEvents = new ConcurrentHashMap<>();

    // SSE로 보낸 이벤트 저장하는 자료구조
    // username에게 보낸 이벤트들, 그리고 전송여부 true/false
    // fanMeetingId, username, eventName, isSent
    public static Map<Long, Map<String, Map<String, Boolean>>> events = new ConcurrentHashMap<>();

    //Emitter 추가
    public SseEmitter createEmitter(Long fanMeetingId, String username, String lastEventId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//        SseEmitter emitter = new SseEmitter; // 10초
        if (emitters.get(fanMeetingId) == null) {
            emitters.put(fanMeetingId, new ConcurrentHashMap<>());
        }

        emitters.get(fanMeetingId).put(username, emitter);

//
        // 타임아웃 발생시 complete() 호출 -> onCompletion() 호출됨.
        emitter.onTimeout(() -> {
            log.info("onTimeout callback");
            emitter.complete();
        });

        // 에러 발생시 complete() 호출 -> onCompletion() 호출됨.
        emitter.onError(throwable -> {
            log.info("onError callback");
            log.info("onError Message: {}", throwable.getMessage());
//            throwable.printStackTrace();
            emitter.complete();
        });

        // 에미터 삭제됨.
        emitter.onCompletion(() -> {
            log.info("onCompletion callback");
            if (emitters.get(fanMeetingId) != null && emitters.get(fanMeetingId).get(username) != null) {
                emitters.get(fanMeetingId).remove(username);    // 만료되면 리스트에서 삭제
            }
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .id(username+System.currentTimeMillis())
                    .data("connected!"));
        } catch (IOException e) {
            log.error("connect Event sent failed. {}", e.getMessage());
            throw new RuntimeException(e);
        }

        sseEvents.computeIfAbsent(username, k -> new ArrayList<>());

        if (!lastEventId.isBlank()) {
            // lastEventId가 있으면, 그 이후의 이벤트들만 다시 보내기
            // lastEventID보다 큰 이벤트만 필터링. lastEventId < X 인 것들만.

            for (SseEvent sseEvent : sseEvents.get(username)) {
                if (sseEvent.getId().compareTo(lastEventId) > 0) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(sseEvent.getName())
                                .id(sseEvent.getId())
                                .data(sseEvent.getData()));
                    } catch (IOException e) {
                        log.error("lastEventId 이후의 이벤트 다시 보내기 실패. {}", sseEvent.getName());
                        // 못 보낸 이후부터 다시 보내야 하기 때문에 바로 break
                        break;
                    }
                }
            }
        }

        log.info("SseService.addEmitter() called");
        return emitter;
    }

    //waitingRoom에 waiter 추가
    @Transactional
    public void addwaiter(String username, Long fanMeetingId, String sessionId) {
        log.info("SseService.addWaiter() called");

        // fanMeetingId(PK값), 대기방Id(세션ID) 구해서 넣기
        if (waitingRooms.get(fanMeetingId) == null) {
            waitingRooms.put(fanMeetingId, new ConcurrentHashMap<>());
        }

        if (waitingRooms.get(fanMeetingId).get(sessionId) == null) {
            Comparator<UserNameAndOrderNumber> comparator = new OrderNumberComparator();
            SortedSet<UserNameAndOrderNumber> sortedSet = new TreeSet<>(comparator);
            waitingRooms.get(fanMeetingId).put(sessionId, sortedSet);
        }

        Optional<FanToFanMeeting> ftfm = fanToFanMeetingRepository.findByFanMeetingIdAndFanUsername(fanMeetingId, username);

        if (ftfm.isEmpty()) {
            throw new CustomException(NOT_FOUND_FANTOFANMEETING);
        }

        boolean flag = false;
        if (waitingRooms.get(fanMeetingId).get(sessionId) != null) {
            SortedSet<UserNameAndOrderNumber> users = waitingRooms.get(fanMeetingId).get(sessionId);
            for (UserNameAndOrderNumber user : users) {
                if (user.getUsername().equals(username)) {
                    user.setCnt(user.getCnt() + 1);
                    flag = true;
                    break;
                }
            }
        }

        if (!flag) {
            UserNameAndOrderNumber userInfo = new UserNameAndOrderNumber(username, ftfm.get().getOrderNumber(), 1L);
            waitingRooms.get(fanMeetingId).get(sessionId).add(userInfo);
        }
    }

    //waitingRoom에 waiter 제거
    public void removeWaiter(String username, Long fanMeetingId, String sessionId) {
        log.info("SseService.removeWaitingRoom() called");

        SortedSet<UserNameAndOrderNumber> watingFans = waitingRooms.get(fanMeetingId).get(sessionId);

        // sortedSet에서 해당 user의 UserNameAndOrderNumber를 찾아서 제거

        if (watingFans != null) {
            for (UserNameAndOrderNumber userNameAndOrderNumber : watingFans) {
                if (userNameAndOrderNumber.getUsername().equals(username)) {
                    userNameAndOrderNumber.setCnt(userNameAndOrderNumber.getCnt() - 1);

                    if (userNameAndOrderNumber.getCnt() == 0) {
                        watingFans.remove(userNameAndOrderNumber);
                        break;
                    }
                }
            }
        }
    }



    //mainWaitingRoom에서 내 앞에 대기하는 인원 수 알려주기
    @Scheduled(fixedRate = 1000)
    public void noticeNumberOfPeopleAhead() {
        // fanMeetingId를 알아야함.
        // main 대기방이 뭔지 알아야함.
        List<FanMeeting> fanMeetings = fanMeetingRepository.findAll();
        for (FanMeeting fanMeeting : fanMeetings) {
//            System.out.println("@@@@@@@@@ 1 @@@@@@@@@");
            Long fanMeetingId = fanMeeting.getId();
//            System.out.println("@@@@@@@@@ 2 @@@@@@@@@");
            String mainWaitingRoomId = fanMeetingRoomOrderRepository.getMainWaitingRoomfindByFanMeetingId(fanMeetingId).get().getCurrentRoom();
//            System.out.println("@@@@@@@@@ 3 @@@@@@@@@");
            if (waitingRooms.get(fanMeetingId) == null) {
                continue;
            }

            if (waitingRooms.get(fanMeetingId).get(mainWaitingRoomId) == null) {
                continue;
            }

            SortedSet<UserNameAndOrderNumber> waitersInMainWaitingRoom = waitingRooms.get(fanMeetingId).get(mainWaitingRoomId);
//            System.out.println("@@@@@@@@@ 4 @@@@@@@@@");
            // mainwaitingroom에 있는 사람들에게 이벤트를 보내서, 자기가 몇번째 대기자인지 알려준다.
            waitersInMainWaitingRoom.forEach(waiter -> {
                Long myOrder = waiter.getOrderNumber();
                // myOrder 보다 작은 orderNumber를 가진 사람들의 수를 구한다.
                Long numberOfPeopleAhead = waitersInMainWaitingRoom.stream().filter(userNameAndOrderNumber -> userNameAndOrderNumber.getOrderNumber() < myOrder).count();

                if (emitters.get(fanMeetingId).get(waiter.getUsername()) == null) {
                    log.info("팬의 emitter가 null임. 재접속이거나 나간 것.");
//                    if (waitingRooms.get(fanMeetingId).get(mainWaitingRoomId).contains(waiter)) {
//                        removeWaiter(waiter.getUsername(), fanMeetingId, mainWaitingRoomId);
//                    }
                } else {
                    sendEvent(fanMeetingId, waiter.getUsername(), "numberOfPeopleAhead", numberOfPeopleAhead);
                    System.out.println("waiter" + waiter.getUsername() + "numberOfPeopleAhead : " + numberOfPeopleAhead);
//                        emitters.get(fanMeetingId).get(waiter.getUsername()).send(SseEmitter.event().name("numberOfPeopleAhead").data(numberOfPeopleAhead));
                }

//                catch (IOException e) {
//                    log.info("PeopleAhead: send 실패. {}", e.getMessage());
//                    if (emitters.get(fanMeetingId).get(waiter.getUsername()) != null) {
//                        emitters.get(fanMeetingId).remove(waiter.getUsername());
//                    }
//                }
            });
        }
    }

    @Scheduled(initialDelay = 3000, fixedRate = 3000)
    public void printemitter() {
        log.info("emitter : " + emitters);
        log.info("waitingRoom : " + waitingRooms);
        log.info("gameRoom : " + gameRooms);
        log.info("isIdolsEntered : " + isIdolsEntered);
    }

    @Transactional
    public String eventReceived(Long fanMeetingId, String username, String event) {
        if (SseService.events.get(fanMeetingId) == null) {
            SseService.events.put(fanMeetingId, new ConcurrentHashMap<>());
        }

        if (SseService.events.get(fanMeetingId).get(username) == null) {
            SseService.events.get(fanMeetingId).put(username, new ConcurrentHashMap<>());
        }

        SseService.events.get(fanMeetingId).get(username).put(event, true);
        return "이벤트를 잘 받았다는 메시지가 서버에게 잘 전송됨";
    }

    public boolean sendEvent(Long fanMeetingId, String username, String eventName, Object data) {
        log.info("{}에게 {} 이벤트 보내기, data: {}", username, eventName, data);

        SseEvent sseEvent = new SseEvent();
        sseEvent.setUsername(username);
        sseEvent.setId(username + System.currentTimeMillis());
        sseEvent.setName(eventName);
        sseEvent.setData(data);

        SseService.sseEvents.get(username).add(sseEvent);

        if (SseService.emitters.get(fanMeetingId) == null || SseService.emitters.get(fanMeetingId).get(username) == null) {
            log.error("SSE send 실패. 유저 이름: {}, 이벤트 이름: {}", username, eventName);
            return false;
        }

        try {
            emitters.get(fanMeetingId).get(username).send(
                    SseEmitter.event()
                            .name(eventName)
                            .id(sseEvent.getId())
                            .data(data)
            );
            return true;
        } catch (IOException e) {
            log.error("SSE Event send 실패. 유저 이름: {}, 이벤트 이름: {}, 에러 메시지: {}", username, eventName, e.getMessage());
            if (SseService.emitters.get(fanMeetingId).get(username) != null) {
                SseService.emitters.get(fanMeetingId).remove(username);
            }
            return false;
        } catch (IllegalStateException e) {
            log.error("emitter가 이미 종료되었습니다. 유저이름: {}, 이벤트 이름: {}, 에러 메시지: {}",username, eventName, e.getMessage());
            if (emitters.get(fanMeetingId).get(username) != null) {
                emitters.get(fanMeetingId).remove(username);
            }
            return false;
        }
    }
}
