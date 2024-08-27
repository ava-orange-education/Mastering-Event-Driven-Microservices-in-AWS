package com.example;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {
	
private final Region region = Region.EU_CENTRAL_1;
	
	private DynamoDbClient ddb = DynamoDbClient.builder()
            .region(region)
            .build();
	
	private Gson gson = new Gson();
	
	@Override
	public String handleRequest(Map<String, Object> event, Context context) {
		System.out.println("Get circuit status" + gson.toJson(event));
		
		var keyToGet = new HashMap<String,AttributeValue>();

        keyToGet.put("serviceName", AttributeValue.builder()
        		.s(event.get("serviceName").toString()).build());
		
		var request = GetItemRequest.builder()
				.tableName("circuit-breaker")
				.key(keyToGet)
				.build();
		
		var item = ddb.getItem(request).item();
		
		if (item != null) {
			var expiry = item.get("expiry");
			var now = System.currentTimeMillis();
			
			if (expiry != null && Long.parseLong(expiry.s()) > now) {
				return "OPEN";
			}
		}

 		return "";
	}
	
}
