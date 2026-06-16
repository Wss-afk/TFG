package com.chatroom.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards Vue Router HTML5 history-mode routes to index.html
 * so that direct navigation / page refresh works in production.
 */
@Controller
public class SpaForwardController {

    @RequestMapping(value = {
        "/login",
        "/home",
        "/chat",
        "/events",
        "/admin",
        "/admin/{path}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
