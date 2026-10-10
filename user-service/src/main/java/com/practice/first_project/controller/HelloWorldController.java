package com.practice.first_project.controller;

import com.practice.first_project.bean.HelloWorld;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 0 warm-up endpoints (first Spring Boot app). Not used by any other class.
 * Not routed by api-gateway (only /auth/** and /jpa/** are), so call them on port 8080 with a token:
 * SecurityConfig's anyRequest().authenticated() applies. Returns HelloWorld (bean package) as JSON.
 */
@RestController
public class HelloWorldController {

    @GetMapping("hello-world")
    public String helloWorld(){
        return "Hello world!";
    }

    @GetMapping("hello-world-bean")
    public HelloWorld helloWorldBean(){
        return new HelloWorld("Hello world!");
    }

    @GetMapping("hello-world-param/{name}/{surname}")
    public HelloWorld helloWorldWithParam(@PathVariable String name, @PathVariable String surname){
        return new HelloWorld("Hello "+name+" "+surname+"!");
    }
}
