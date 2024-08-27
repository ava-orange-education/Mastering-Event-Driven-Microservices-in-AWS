package com.example;

import java.util.HashMap;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.dto.OrderRequest;
import com.google.gson.Gson;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class LambdaHandler implements RequestHandler<SQSEvent, Void> {
	
private final Region region = Region.EU_CENTRAL_1;
	
	private DynamoDbClient ddb = DynamoDbClient.builder()
            .region(region)
            .build();
	
	private SqsClient sqsClient = SqsClient.builder()
            .region(region)
            .build();

	private Gson gson = new Gson();
	
	@Override
	public Void handleRequest(SQSEvent event, Context context) {
		System.out.println("Received event from SQS");

		for (var message : event.getRecords()) {
			System.out.println("Message" + message.getBody());
			
			// get order event
			var order = gson.fromJson(message.getBody(), OrderRequest.class);
			
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
			
			// send InventoryReserved message to SQS
			var getQueueRequest = GetQueueUrlRequest.builder()
	                .queueName("InventoryReservedQueue")
	                .build();
			
			var queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();
			
			var sendMsgRequest = SendMessageRequest.builder()
	                .queueUrl(queueUrl)
	                .messageBody(message.getBody())
	                .build();

	        sqsClient.sendMessage(sendMsgRequest);
		}

 		return null;
	}
}
