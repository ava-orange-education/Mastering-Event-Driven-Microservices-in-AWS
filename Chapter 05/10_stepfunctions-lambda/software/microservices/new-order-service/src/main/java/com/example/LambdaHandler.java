package com.example;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.dto.OrderRequest;
import com.example.model.Order;
import com.example.model.OrderStatus;
import com.google.gson.Gson;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class LambdaHandler implements RequestHandler<Map<String, Object>, Object> {
	
private final Region region = Region.EU_CENTRAL_1;
	
	private DynamoDbClient ddb = DynamoDbClient.builder()
            .region(region)
            .build();
	
	private Gson gson = new Gson();
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
	
	@Override
	public Object handleRequest(Map<String, Object> event, Context context) {
		System.out.println("Received input from Step Functions" + gson.toJson(event));

		// get order event
		var order = toOrder(event);
		
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

		// return response to Step Functions
 		return order;
	}
	
	private Order toOrder(Map<String, Object> event) {
		var eventJson = gson.toJson(event);
		var orderRequest = gson.fromJson(eventJson, OrderRequest.class);
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
