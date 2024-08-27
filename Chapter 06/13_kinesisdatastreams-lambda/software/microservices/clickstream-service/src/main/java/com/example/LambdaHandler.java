package com.example;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.example.dto.ClickstreamEvent;
import com.example.model.Clickstream;
import com.google.gson.Gson;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class LambdaHandler implements RequestHandler<KinesisEvent, Void> {
	
private final Region region = Region.EU_CENTRAL_1;
	
	private DynamoDbClient ddb = DynamoDbClient.builder()
            .region(region)
            .build();
	
	private Gson gson = new Gson();
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
	
	@Override
	public Void handleRequest(KinesisEvent event, Context context) {
		System.out.println("Received input from Kinesis" + event);
		
		if (event.getRecords().isEmpty()) {
			System.out.println("No Kinesis event received");
            return null;
        }
		
        for (var record : event.getRecords()) {
        	// convert to clickstream event
            var data = new String(record.getKinesis().getData().array());
    
    		// get clickstream event
    		var clickstream = toClickStream(data);
    		
    		// save Clickstream to DynamoDB
    		HashMap<String,AttributeValue> itemValues = new HashMap<>();
    		itemValues.put("clickstreamId",  AttributeValue.builder().s(clickstream.getClickstreamId()).build());
    		itemValues.put("session",  AttributeValue.builder().s(clickstream.getSession()).build());
    		itemValues.put("timestamp",  AttributeValue.builder().s(sdf.format(clickstream.getTimestamp())).build());
    		itemValues.put("pageUrl",  AttributeValue.builder().s(clickstream.getPageUrl().toString()).build());
    		
    		var request = PutItemRequest.builder()
    	            .tableName("clickstreams")
    	            .item(itemValues)
    	            .build();

    		ddb.putItem(request);
        }
        
        return null;
	}
	
	private Clickstream toClickStream(String event) {
		var request = gson.fromJson(event, ClickstreamEvent.class);
		var clickstream = new Clickstream();
		clickstream.setClickstreamId(UUID.randomUUID().toString());
		clickstream.setSession(request.getSession());
		clickstream.setTimestamp(new Date());
		clickstream.setPageUrl(request.getPageUrl());
		return clickstream;
	}
}
