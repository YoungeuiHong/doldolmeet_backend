package com.doldolmeet.domain.waitRoom.chat.repository;

import com.doldolmeet.domain.waitRoom.chat.dto.ChatMessageDto;
import com.doldolmeet.domain.waitRoom.chat.dto.ChatRoomDto;
import com.doldolmeet.domain.waitRoom.config.RedisSubscriber;
import com.doldolmeet.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatRoomRepository {

    private final RedisMessageListenerContainer redisMessageListener;
    private final RedisSubscriber redisSubscriber;

    private final RedisTemplate redisTemplate;
    private HashOperations<String, String, ChatRoomDto> opsHashChatRoom;
    private Map<String, ChannelTopic> topics = new HashMap<>();
    // (방번호, 팬리스트)
//    private Map<String, List<Fan>> fansInRoom = new ConcurrentHashMap<>();
    private JwtUtil jwtUtil;
    private Claims claims;
//    private FanRepository fanRepository;
//    private FanToFanMeetingRepository fanToFanMeetingRepository;
//    private FanMeetingRepository fanMeetingRepository;

    @PostConstruct
    private void init() {
        opsHashChatRoom = redisTemplate.opsForHash();
    }

    private static final String CHAT_ROOMS = "CHAT_ROOM";


    public List<ChatRoomDto> findAllRoom() {
        return opsHashChatRoom.values(CHAT_ROOMS);
    }

    public ChatRoomDto findRoomById(String id) {
        return opsHashChatRoom.get(CHAT_ROOMS, id);
    }

    // 채팅방 생성 : 서버간 채팅방 공유를 위해 redis에 hash구조로 저장한다.
    public ChatRoomDto createChatRoom(String name) {
        ChatRoomDto room = ChatRoomDto.create(name);
        // roomId값을 FanToChatRoom에 저장
        log.info("createChatRoom: {}", name);
        opsHashChatRoom.put(CHAT_ROOMS, room.getRoomId(), room);
        return room;
    }

    // 채팅방 입장 : Redis Topic을 만들고 pub/sub 통신을 하기 위해서 리스너를 설정한다.
    public boolean enterChatRoom(String roomId) {
        log.info("enterChatRoom: {}", roomId);
        ChannelTopic topic = topics.get(roomId);


        // 방이 없으면 생성
        if (topic == null) {
            log.info("enterChatRoom topic == null roomId: {}", roomId);

            topic = new ChannelTopic(roomId);
            log.info("enterChatRoom topic == null topic: {}", roomId);

            MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisSubscriber);
            redisMessageListener.addMessageListener(messageListenerAdapter, topic);
            topics.put(roomId, topic); // (방번호, 방번호에 해당하는 토픽)

            // 입장한 팬 정보 얻기

            // 얻어서 fansInRoom에 저장

            // 해당 방 팬카운트 + 1
            return true; // 처음 입장할 때만 true 반환
        }

        return false; // 이미 채팅방이 존재할 경우 false 반환
    }

//        public String getWaitRoomId(Long fanMeetingId, HttpServletRequest request) {
//            String jwtToken = jwtUtil.resolveToken(request);
//
//            if (jwtToken == null) {
//                throw new IllegalArgumentException("jwt 토큰 없음");
//            }
//
//            if (!jwtUtil.validateToken(jwtToken)) {
//                throw new IllegalArgumentException("유효하지 않은 jwt입니다.");
//            }
//
//            claims = jwtUtil.getUserInfoFromToken(jwtToken);
//            String username = claims.getSubject();
//
//            // username으로 User id
//            Optional<Fan> fan = fanRepository.findByUsername(username);
//
//            if (!fan.isPresent()) {
//                throw new IllegalArgumentException("없는 팬입니다.");
//            }
//
//            Optional<FanMeeting> fanMeeting = fanMeetingRepository.findById(fanMeetingId);
//
//            if (!fanMeeting.isPresent()) {
//                throw new IllegalArgumentException("없는 팬미팅입니다.");
//            }
//
//            // 존재하면, fantofanmeeting 찾아서 당첨확인
//            FanToFanMeeting fanToFanMeeting = fanToFanMeetingRepository.findFanToFanMeetingByFanIdAndFanMeetingId(fan.get().getId(), fanMeetingId);
//
//            if (fanToFanMeeting == null) {
//                throw new IllegalArgumentException("해당 팬미팅에 신청하지 않은 팬입니다.");
//            }
//
//            if (fanToFanMeeting.getFanToFanMeetingStatusEnum().equals(FanToFanMeetingStatusEnum.DISAPPROVED)) {
//                throw new IllegalArgumentException("미당첨된 팬입니다.");
//            }
//
//            // 이제 방번호 알려주기
//            return fanMeeting.get().getChatRoom().getRoomId();
//        }

    // com.websocket.chat.repository.ChatRoomRepository

    public List<ChatMessageDto> getChatMessages(String roomId) {
        return redisTemplate.opsForList().range(roomId, 0, -1);
    }

    public void saveChatMessage(String roomId, ChatMessageDto chatMessage) {
        redisTemplate.opsForList().rightPush(roomId, chatMessage);
    }

    public ChannelTopic getTopic(String roomId) {
        log.info("getTopic: {}", roomId);
        return topics.get(roomId);
    }
}
