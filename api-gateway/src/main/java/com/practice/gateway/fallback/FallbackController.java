package com.practice.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Target of the routes' CircuitBreaker filters (fallbackUri=forward:/fallback/{service}).
 * When a service is down, has no registered instance, or doesn't answer in time, clients get an
 * immediate 503 in the same JSON shape the services use, instead of a 500 after a timeout.
 * @RequestMapping (all methods): the forward keeps the original method, e.g. POST /comments.
 *
 *   called by: the gateway itself, never by a client directly. application.properties:
 *              routes[0] (user-service)    ...filters[0].args.fallbackUri=forward:/fallback/user-service
 *              routes[1] (comment-service) ...filters[0].args.fallbackUri=forward:/fallback/comment-service
 *   test     : ApiGatewayApplicationTest
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> serviceUnavailable(@PathVariable String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "timeStamp", LocalDateTime.now().toString(),
                "message", service + " is unavailable. Please try again shortly.",
                "details", "uri=/fallback/" + service,
                "errors", List.of()));
    }
}
