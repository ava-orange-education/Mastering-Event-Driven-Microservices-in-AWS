package com.example;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

public class LambdaHandler implements RequestHandler<Map<String, Object>, Object> {
	
	private Gson gson = new Gson();
	
	@Override
	public Object handleRequest(Map<String, Object> event, Context context) {
		var eventJson = gson.toJson(event);
		
		System.out.println("Received input from Step Functions" + eventJson);

		// fulfil order logic

 		return event;
	}
	
}
