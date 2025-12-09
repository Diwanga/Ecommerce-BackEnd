package com.diwangaamasith.CloudGateway.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @GetMapping("/orderServiceFallBack")
    public String orderServiceFallBack(){
        return "ORDER service is Down";
    }
    @GetMapping("/productServiceFallBack")
    public String productServiceFallBack(){
        return "PRODUCT service is Down";
    }
    @GetMapping("/paymentServiceFallBack")
    public String paymentServiceFallBack(){
        return "PAYMENT service is Down";
    }
}
