package com.orderservices.services;

import com.orderservices.dto.InventoryResponse;
import com.orderservices.dto.OrderRequest;
import com.orderservices.model.Order;
import com.orderservices.model.OrderLineItems;
import com.orderservices.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public OrderService(OrderRepository orderRepository, WebClient.Builder webClient) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClient;
    }

    public void placeOrder(OrderRequest orderRequest) {
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

        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build()
                )
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse :: getInStock);

        if (allProductsInStock){
            orderRepository.save(order);
        }else{
            throw new IllegalArgumentException("Not enough stock");
        }

        log.info("Order Placed Successfully {}",order.getId());
    }
}
