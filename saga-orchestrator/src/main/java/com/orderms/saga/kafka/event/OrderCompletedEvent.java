package com.orderms.saga.kafka.event;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class OrderCompletedEvent {
//
//    private Long orderId;
//    private String userId;
//    private String message;
//}.


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {

    private Long orderId;
    private String userId;
    private boolean success;  // ← ADD THIS: true = completed, false = cancelled
    private String message;
//    private LocalDateTime timestamp;
}