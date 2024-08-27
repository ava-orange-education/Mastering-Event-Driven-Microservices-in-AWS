package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.dto.OrderRequest;
import com.google.gson.Gson;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class LambdaHandler implements RequestHandler<SQSEvent, Void> {
	
private final Region region = Region.EU_CENTRAL_1;
	
	private SqsClient sqsClient = SqsClient.builder()
            .region(region)
            .build();

	private Gson gson = new Gson();
	
	@Override
	public Void handleRequest(SQSEvent event, Context context) {
		System.out.println("Received event from SQS");

		for (var message : event.getRecords()) {
			// get order event
			var order = gson.fromJson(message.getBody(), OrderRequest.class);
			
			processPayment(order);
			
			// send PaymentProcessed message to SQS
			var getQueueRequest = GetQueueUrlRequest.builder()
	                .queueName("PaymentProcessedQueue")
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
	
	private void processPayment(OrderRequest order) {
		// process payment logic
	}
}
