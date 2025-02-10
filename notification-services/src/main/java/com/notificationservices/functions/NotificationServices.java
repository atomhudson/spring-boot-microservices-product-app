package com.notificationservices.functions;

import com.notificationservices.event.OrderPlacedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
public class NotificationServices {

    @Bean
    public Supplier<String> testing(){
        return () -> "This is Testing";
    }

    @Bean
    public Function<String,String> sayHello(){
        return (message) -> "Hello How Are You ? " + message;
    }

    @Bean
    public Function<OrderPlacedEvent, String> orderPlacedEvent(){
        return (orderPlacedEvent) -> {
            sendNotification(orderPlacedEvent);
            System.out.println("orderPlacedEvent");
            System.out.println("Order ID: " + orderPlacedEvent.getOrderId());
            System.out.println("Order Number: " + orderPlacedEvent.getOrderNumber());
            return orderPlacedEvent.getOrderNumber();
        };
    }

    public void sendNotification(OrderPlacedEvent orderPlacedEvent){
        System.out.println("Notification Sent Successfully: "+orderPlacedEvent);
    }

}
