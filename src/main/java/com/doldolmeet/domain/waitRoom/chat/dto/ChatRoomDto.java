package com.doldolmeet.domain.waitRoom.chat.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class ChatRoomDto implements Serializable {

    private static final long serialVersionUID = 6494678977089006639L;

    private String roomId;
    private String name;

    public static ChatRoomDto create(String name) {
        ChatRoomDto chatRoomDto = new ChatRoomDto();
        chatRoomDto.roomId = UUID.randomUUID().toString();
        chatRoomDto.name = name;
        return chatRoomDto;
    }
    // 채팅방은 입장한 클라이언트들의 정보를 가지고 있어야 하기 때문에 WebSocketSession 정보 리스트를 멤버 필드로 갖는다.
    // v2. pub/sub 방식을 이용하면 구독자 관리가 알아서되기 때문에 웹 소켓 세션관리가 필요없어진다.
    // 또한 발송의 구현도 알아서 해결되기 때문에 일일이 클라이언트에게 메세지를 발송하는 구현이 필요없어진다.
//    private Set<WebSocketSession> sessions = new HashSet<>();
//
//    @Builder
//    public ChatRoomDto(String roomId, String name) {
//        this.roomId = roomId;
//        this.name = name;
//    }

    // v2.
//    public static ChatRoomDto create(String name) {
//        return ChatRoomDto.builder()
//                .roomId(UUID.randomUUID().toString())
//                .name(name)
//                .build();
//    }

//    /**
//     * v1.
//     * 채팅방에는 입장, 대화하기 기능이 있기 때문에 handlAction을 통해 분기처리를한다.
//     * 입장시에는 채팅방의 세션정보에 클라이언트의 세션을 추가해놓았다가 채팅방에 메세지가 도착할 경우 모든 세션에 메세지를 발송한다.
//     * @param session
//     * @param message
//     * @param service
//     */
//    public void handleActions(WebSocketSession session, ChatMessage message, ChatService service) {
//
//        if(message.getType().equals(ENTER)) {
//            sessions.add(session);
//            message.setMessage(message.getSender() + "님이 입장했습니다.");
//        }
//
//        sendMessage(message, service);
//    }
//
//    public <T> void sendMessage(T message, ChatService service) {
//        sessions.parallelStream().forEach(session -> service.sendMessage(session, message));
//    }
}