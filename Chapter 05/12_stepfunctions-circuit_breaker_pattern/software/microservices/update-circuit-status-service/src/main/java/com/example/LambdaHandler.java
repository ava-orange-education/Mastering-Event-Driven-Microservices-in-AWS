package com.example;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
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
		System.out.println("Update circuit status" + gson.toJson(event));
		
		HashMap<String,AttributeValue> itemValues = new HashMap<>();
		itemValues.put("serviceName",  AttributeValue.builder().s(event.get("serviceName").toString()).build());
		itemValues.put("state",  AttributeValue.builder().s("OPEN").build());
		itemValues.put("expiry",  AttributeValue.builder().s(Long.toString(System.currentTimeMillis() + 30000)).build());
		
		var request = PutItemRequest.builder()
	            .tableName("circuit-breaker")
	            .item(itemValues)
	            .build();

		ddb.putItem(request);

 		return "OK";
	}
	
}
