package com.example;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.example.dto.OrderRequest;
import com.example.model.Order;
import com.example.model.OrderStatus;
import com.google.gson.Gson;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class LambdaHandler implements RequestHandler<SQSEvent, Void> {
	
	private DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1)
            .build();
	
	private Gson gson = new Gson();
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
    
	@Override
	public Void handleRequest(SQSEvent event, Context context) {
		System.out.println("Received order request from SQS");

		for (var message : event.getRecords()) {
			// get order event
			var order = toOrder(message);
			
			// save Order to DynamoDB
			HashMap<String,AttributeValue> itemValues = new HashMap<>();
			itemValues.put("orderId",  AttributeValue.builder().s(order.getOrderId()).build());
			itemValues.put("customerId",  AttributeValue.builder().s(order.getCustomerId()).build());
			itemValues.put("orderDate",  AttributeValue.builder().s(sdf.format(order.getOrderDate())).build());
			itemValues.put("status",  AttributeValue.builder().s(order.getStatus().toString()).build());
			itemValues.put("items",  AttributeValue.builder().ss(order.getItems()).build());
			itemValues.put("total",  AttributeValue.builder().n(order.getTotal()).build());
			
			var request = PutItemRequest.builder()
		            .tableName("orders")
		            .item(itemValues)
		            .build();

			ddb.putItem(request);
		}
		

 		return null;
	}
	
	private Order toOrder(SQSMessage event) {
		var orderRequest = gson.fromJson(event.getBody(), OrderRequest.class);
		var order = new Order();
		order.setOrderId(UUID.randomUUID().toString());
		order.setCustomerId(orderRequest.getCustomerId());
		order.setOrderDate(new Date());
		order.setStatus(OrderStatus.PLACED);
		order.setItems(orderRequest.getItems());
		order.setTotal(orderRequest.getTotal());
		return order;
	}
}
