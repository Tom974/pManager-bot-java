package dev.tom974;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(@Nullable MessageBrokerRegistry config) {
        assert config != null;
        config.enableSimpleBroker("/porto");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@Nullable StompEndpointRegistry registry) {
        assert registry != null;
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
//        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
