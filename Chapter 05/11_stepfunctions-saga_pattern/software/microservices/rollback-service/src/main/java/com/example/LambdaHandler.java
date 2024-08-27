package com.example;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaHandler implements RequestHandler<Map<String, Object>, String> {
	
	@Override
	public String handleRequest(Map<String, Object> event, Context context) {
		System.out.println("Rollback service");
		// write rollback logic here
		
 		return "OK";
	}
}
