package com.example;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.example.dto.Clickstream;
import com.example.dto.ClickstreamEvent;
import com.example.dto.TransformedRecord;
import com.example.dto.TransformedRecords;
import com.google.gson.Gson;

public class LambdaHandler implements RequestHandler<KinesisFirehoseEvent, TransformedRecords> {
	
	private Gson gson = new Gson();
	
	@Override
	public TransformedRecords handleRequest(KinesisFirehoseEvent event, Context context) {
		System.out.println("Received input from Kinesis Firehose" + event);
		
		var transformedRecords = new ArrayList<TransformedRecord>();
			
        for (var record : event.getRecords()) {
        	// convert to clickstream event
            var data = new String(record.getData().array());
    
    		// get clickstream event
    		var clickstream = toClickStream(data);
    		
    		// create transformed record
            var transformedRecord = new TransformedRecord(
            		record.getRecordId(),
            		"Ok", 
            		ByteBuffer.wrap(clickstream.toString().getBytes()));
    		    		
            transformedRecords.add(transformedRecord);
        	
        }
        
        var response = new TransformedRecords();
        response.setRecords(transformedRecords);
        return response;
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
