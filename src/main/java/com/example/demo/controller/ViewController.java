package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ViewController {

    @RequestMapping(value = {
        "/",
        "/login",
        "/register",
        "/cart",
        "/orders",
        "/wishlist",
        "/addresses",
        "/admin",
        "/admindashboard"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
