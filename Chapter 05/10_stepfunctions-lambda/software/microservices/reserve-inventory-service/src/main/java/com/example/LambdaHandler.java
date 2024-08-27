package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.dto.OrderRequest;
import com.google.gson.Gson;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {
	
private final Region region = Region.EU_CENTRAL_1;
	
	private DynamoDbClient ddb = DynamoDbClient.builder()
            .region(region)
            .build();

	private Gson gson = new Gson();
	
	@Override
	public String handleRequest(Map<String, Object> event, Context context) {
		var eventJson = gson.toJson(event);
		
		System.out.println("Received input from Step Functions" + eventJson);

		var order = gson.fromJson(eventJson, OrderRequest.class);
		
		// reserve inventory to DynamoDB
		HashMap<String,AttributeValue> itemValues = new HashMap<>();
		itemValues.put("inventoryId",  AttributeValue.builder().s(UUID.randomUUID().toString()).build());
		itemValues.put("orderId",  AttributeValue.builder().s(order.getOrderId()).build());
		itemValues.put("items",  AttributeValue.builder().ss(order.getItems()).build());
		
		var request = PutItemRequest.builder()
	            .tableName("products-inventory")
	            .item(itemValues)
	            .build();

		ddb.putItem(request);
		
 		return "OK";
	}
}
