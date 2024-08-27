package com.example;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {
	
	private Gson gson = new Gson();
	
	@Override
	public String handleRequest(Map<String, Object> event, Context context) {
		var eventJson = gson.toJson(event);
		
		System.out.println("Received input from Step Functions" + eventJson);

		processPayment(eventJson);

 		return "OK";
	}
	
	private void processPayment(String event) {
		// process payment logic
	}
}
