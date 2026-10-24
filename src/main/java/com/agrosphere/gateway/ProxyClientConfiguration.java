package com.agrosphere.gateway;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.*;
@Configuration
class ProxyClientConfiguration {
    @Bean ClientHttpRequestFactory gatewayRequestFactory(@Value("${GATEWAY_CONNECT_TIMEOUT:2s}") Duration connect,
            @Value("${GATEWAY_READ_TIMEOUT:10s}") Duration read) {
        if(connect.isZero()||connect.isNegative()||read.isZero()||read.isNegative())throw new IllegalArgumentException("Gateway timeouts must be positive");
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(connect)
            .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(read);return factory;
    }
}
