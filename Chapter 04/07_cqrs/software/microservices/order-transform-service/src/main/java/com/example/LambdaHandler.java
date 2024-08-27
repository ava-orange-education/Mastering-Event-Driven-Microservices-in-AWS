package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaHandler implements RequestHandler<Object, Void> {
	
	@Override
	public Void handleRequest(Object event, Context context) {
		System.out.println("Transform items from DynamoDB orders-write to orders-read table through the EventBridge pipe" + event);

 		return null;
	}
}
