package com.doldolmeet.domain.waitRoom.config;

import com.doldolmeet.domain.waitRoom.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(ChannelTopic topic, ChatMessageDto message) {
        // 기존 publish 메서드에 저장 로직 추가
        redisTemplate.opsForList().rightPush(topic.getTopic(), message);
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }
}