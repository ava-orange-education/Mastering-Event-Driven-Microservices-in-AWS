package com.example.dto;

public class OrderPaymentDetails {

	private String paymentMethod;
	
    private String paymentTransactionId;
    
    public String getPaymentMethod() {
		return paymentMethod;
	}
    
    public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
    
    public String getPaymentTransactionId() {
		return paymentTransactionId;
	}
    
    public void setPaymentTransactionId(String paymentTransactionId) {
		this.paymentTransactionId = paymentTransactionId;
	}
	
}
