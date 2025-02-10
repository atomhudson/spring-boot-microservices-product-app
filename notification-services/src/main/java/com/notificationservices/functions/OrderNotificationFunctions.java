package com.notificationservices.functions;

import com.notificationservices.event.OrderPlacedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class OrderNotificationFunctions {

    @Bean
    public Function<OrderPlacedEvent, String> orderEventReceiver(){
        return (orderPlacedEvent -> {
            System.out.println("Sending Notification");
            logicToSendEmailAndMessageToUser(orderPlacedEvent.getOrderId(),orderPlacedEvent.getOrderNumber());
            return "Order Placed";
        });
    }

    private void logicToSendEmailAndMessageToUser(String orderId, String orderNumber) {
        System.out.println("Sending Email to User for Order ID: " + orderId + " and Order Number: " + orderNumber);
    }
}
