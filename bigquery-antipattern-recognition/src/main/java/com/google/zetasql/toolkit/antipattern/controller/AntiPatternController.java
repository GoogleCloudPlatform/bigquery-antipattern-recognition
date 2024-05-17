package com.google.zetasql.toolkit.antipattern.controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AntiPatternController {

    @PostMapping("/")
    String hello() {
        return "Hello !";
      }
}
