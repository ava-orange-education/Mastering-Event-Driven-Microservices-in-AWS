package com.example.dto;

import java.util.List;

public class OrderRequest {

	private String orderId;
	
	private String customerId;
	
	private List<String> items;
	
	private String total;
	
	private OrderPaymentDetails paymentDetails;
	
	private OrderShippingDetails shippingDetails;
	
	public String getOrderId() {
		return orderId;
	}
	
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public List<String> getItems() {
		return items;
	}

	public void setItems(List<String> items) {
		this.items = items;
	}

	public String getTotal() {
		return total;
	}

	public void setTotal(String total) {
		this.total = total;
	}
	
	public OrderPaymentDetails getPaymentDetails() {
		return paymentDetails;
	}
	
	public void setPaymentDetails(OrderPaymentDetails paymentDetails) {
		this.paymentDetails = paymentDetails;
	}
	
	public OrderShippingDetails getShippingDetails() {
		return shippingDetails;
	}
	
	public void setShippingDetails(OrderShippingDetails shippingDetails) {
		this.shippingDetails = shippingDetails;
	}

}
