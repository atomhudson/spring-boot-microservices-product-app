package com.orderservices.dto;

public class InventoryResponse {
    private String skuCode;
    private Boolean isInStock;

    public InventoryResponse() {}

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Boolean getInStock() {
        return isInStock;
    }

    public void setInStock(Boolean inStock) {
        isInStock = inStock;
    }
}
