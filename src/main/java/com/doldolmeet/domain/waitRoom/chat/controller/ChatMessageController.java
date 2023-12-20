package com.doldolmeet.domain.waitRoom.chat.controller;

import com.doldolmeet.domain.waitRoom.chat.dto.ChatMessageDto;
import com.doldolmeet.domain.waitRoom.chat.repository.ChatRoomRepository;
import com.doldolmeet.domain.waitRoom.config.RedisPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class ChatMessageController {

    private final RedisPublisher redisPublisher;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        if (ChatMessageDto.MessageType.ENTER.equals(message.getType())) {
            chatRoomRepository.enterChatRoom(message.getRoomId());
        }
        // Websocket에 발행된 메시지를 redis로 발행한다(publish)
        redisPublisher.publish(chatRoomRepository.getTopic(message.getRoomId()), message);
    }

    // WebSocket "/pub/chat/message"로 들어오는 메시지를 처리한다.
//    @MessageMapping("/chat/message")
//    public void message(ChatMessage message) {
//        if (ENTER.equals(message.getType())) {
//            if (chatRoomRepository.enterChatRoom(message.getRoomId())) {
//                // 채팅방에 처음 입장할 때만 알림 메시지를 전체에게 보내기
//                message.setMessage(message.getSender() + "님이 입장하셨습니다.");
////                messagingTemplate.convertAndSend("/topic/chatRoom/" + message.getRoomId(), message);
//            }
//        }
//
//        // 메시지를 저장
////        chatRoomRepository.saveChatMessage(message.getRoomId(), message);
//
//        // WebSocket에 발행된 메세지를 Redis로 발행
//        redisPublisher.publish(chatRoomRepository.getTopic(message.getRoomId()), message);
//    }
}
