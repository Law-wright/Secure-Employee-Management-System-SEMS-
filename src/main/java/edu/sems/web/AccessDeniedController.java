package edu.sems.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AccessDeniedController {

    @RequestMapping(value = "/access-denied", method = {RequestMethod.GET, RequestMethod.POST})
    public String denied() {
        return "access-denied";
    }
}
