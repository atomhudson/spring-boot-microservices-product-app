package com.orderservices.services;

import brave.Span;
import brave.Tracer;
import brave.propagation.CurrentTraceContext;
import com.orderservices.dto.InventoryResponse;
import com.orderservices.dto.OrderRequest;
import com.orderservices.event.OrderPlacedEvent;
import com.orderservices.model.Order;
import com.orderservices.model.OrderLineItems;
import com.orderservices.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public OrderService(OrderRepository orderRepository, WebClient.Builder webClient, Tracer tracer, KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClient;
        this.tracer = tracer;
        this.kafkaTemplate = kafkaTemplate;
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

//        Call Inventory Service, and place order if product is in stock
        Span span = tracer.nextSpan().name("InventoryServiceLookup").start();
        try (Tracer.SpanInScope scope = tracer.withSpanInScope(span)) {

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
                log.info("Order Placed Successfully {}", order.getId());
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
                return "Order placed successfully";
            } else {
                throw new IllegalArgumentException("Not enough stock");
            }
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.finish();
        }
    }
}
