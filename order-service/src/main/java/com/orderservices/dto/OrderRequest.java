package com.orderservices.dto;

import java.util.List;

public class OrderRequest {
    private List<OrderLineItemsDTO> orderLineItemsDTO;

    public OrderRequest() {}

    public OrderRequest(List<OrderLineItemsDTO> orderLineItemsDTO) {
        this.orderLineItemsDTO = orderLineItemsDTO;
    }

    public List<OrderLineItemsDTO> getOrderLineItemsDTO() {
        return orderLineItemsDTO;
    }

    public void setOrderLineItemsDTO(List<OrderLineItemsDTO> orderLineItemsDTO) {
        this.orderLineItemsDTO = orderLineItemsDTO;
    }
}
