package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaHandler implements RequestHandler<Object, Void> {
	
	@Override
	public Void handleRequest(Object event, Context context) {
		System.out.println("Query items from DynamoDB orders-read table" + event);

 		return null;
	}
}
