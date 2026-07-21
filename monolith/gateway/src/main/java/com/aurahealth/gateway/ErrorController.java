package com.aurahealth.gateway;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ErrorController {

    @RequestMapping("/error")
    public Mono<String> handleError(ServerWebExchange exchange) {
        // This prints out any background exception directly onto your screen instead of the Whitelabel fallback
        return Mono.just("OAuth Callback Processing Stopped. Check console logs or network exchanges.");
    }
}