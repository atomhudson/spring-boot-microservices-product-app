package com.orderservices.services;

import brave.Tracer;
import com.orderservices.dto.InventoryResponse;
import com.orderservices.dto.OrderRequest;
import com.orderservices.model.Order;
import com.orderservices.model.OrderLineItems;
import com.orderservices.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final StreamBridge streamBridge;

    public OrderService(OrderRepository orderRepository, WebClient.Builder webClient, StreamBridge streamBridge) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClient;
        this.streamBridge = streamBridge;
    }

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();

        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItem = orderRequest.getOrderLineItemsDTO()
                .stream()
                .map(orderLineItemsDTO -> {
                    OrderLineItems newOrderLineItems = new OrderLineItems();
                    newOrderLineItems.setId(orderLineItemsDTO.getId());
                    newOrderLineItems.setSkuCode(orderLineItemsDTO.getSkuCode());
                    newOrderLineItems.setPrice(orderLineItemsDTO.getPrice());
                    newOrderLineItems.setQuantity(orderLineItemsDTO.getQuantity());
                    return newOrderLineItems;
                }).collect(Collectors.toList());

        order.setOrderLineItemsList(orderLineItem);

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

//      Call Inventory Service, and place order if product is in stock
        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-services/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build()
                )
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponses)
                .allMatch(InventoryResponse::getInStock);

        if (allProductsInStock) {
            log.info("Order Placed Successfully {}", order.getOrderNumber());
            orderRepository.save(order);
            orderCreatedNotification(order);
            return "Order placed successfully";
        } else {
            throw new IllegalArgumentException("Not enough stock");
        }
    }

    private void orderCreatedNotification(Order orderDetails) {
        boolean send = streamBridge.send("orderCreatedEvent-out-1", orderDetails);
        if (send) {
            log.info("Order created successfully {}", orderDetails.getId());
        }else{
            log.warn("Order creation Failed {}", orderDetails.getId());
        }
    }
}
