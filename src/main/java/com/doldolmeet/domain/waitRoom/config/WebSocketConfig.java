package com.doldolmeet.domain.waitRoom.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
//@EnableWebSocket
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//    v1
//    private final WebSocketHandler webSocketHandler;
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(webSocketHandler, "/ws/chat").setAllowedOrigins("*");
//    }


    /**
     * pub/sub 메세징을 구현하기 위해 메세지를 발행하는 요청의 prefix는 /pub로 시작하고 메세지를 구독하는 요청은 /sub으로 시작하도록 설정한다.
     * STOMP WebSocket의 EndPoint는 /ws-stomp로 설정한다.
     * 결과적으로 개발서버의 접속 주소는 다음과 같다. ws://localhost:8080/ws-stomp
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub");
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOrigins("https://www.doldolmeet.shop")
//                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
