package com.example;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.kinesis.Stream;
import software.amazon.awscdk.services.kinesis.StreamMode;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.eventsources.KinesisEventSource;
import software.amazon.awscdk.services.lambda.eventsources.KinesisEventSourceProps;
import software.constructs.Construct;

public class CdkExampleStack extends Stack {
    
    public CdkExampleStack(final Construct scope, final String id) {
        this(scope, id, null);
    }
    
    private Function createLambdaFunction(String name, String functionName, String codePath) {
    	var lambda = Function.Builder.create(this, name)
    			.functionName(functionName)
                .runtime(Runtime.JAVA_21)
                .memorySize(512)
                .timeout(Duration.seconds(15))
                .code(Code.fromAsset(codePath))
                .handler("com.example.LambdaHandler")
                .build();
        
        return lambda;
    }
        
    private Table createDynamoDBTable(String name, String tableName, String partitionKey, Function lambdaToGrantAccess) {
    	var table = Table.Builder.create(this, name)
    			.tableName(tableName)
                .partitionKey(Attribute.builder()
                    .name(partitionKey)
                    .type(AttributeType.STRING)
                    .build())
                .build();
    	
    	table.grantReadWriteData(lambdaToGrantAccess);
    	
    	return table;
    }
    
    private Stream createKinesisDataStream(String name, String streamName, Function lambdaToGrantAccess) {
    	var stream = Stream.Builder.create(this, name)
    			.streamName(streamName)
    			.streamMode(StreamMode.ON_DEMAND)
    			.build();
    	
    	stream.grantRead(lambdaToGrantAccess);
    	lambdaToGrantAccess.addEventSource(new KinesisEventSource(stream, 
    			KinesisEventSourceProps.builder()
    			.startingPosition(StartingPosition.TRIM_HORIZON)
    			.build()));
    	
    	return stream;
    }
    
    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var clickstreamServiceFunction = createLambdaFunction(
        		"Lambda_ClickstreamService", 
        		"clickstream-service", 
        		"../software/microservices/clickstream-service/target/clickstream-service.jar");
        
        var clickstreamsTable = createDynamoDBTable(
        		"DynamoDB_ClickstreamsTable",
        		"clickstreams",
        		"clickstreamId",
        		clickstreamServiceFunction);        
        
        var stream = createKinesisDataStream(
        		"KinesisDataStream_OrderingSystemClickstream",
        		"OrderingSystemClickstream",
        		clickstreamServiceFunction);
        
        new CfnOutput(this, "Lambda_ClickstreamService_ARN", CfnOutputProps.builder()
                .value(clickstreamServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_ClickstreamsTable_ARN", CfnOutputProps.builder()
                .value(clickstreamsTable.getTableArn())
                .build());
        
        new CfnOutput(this, "KinesisDataStream_OrderingSystemClickstream_ARN", CfnOutputProps.builder()
                .value(stream.getStreamArn())
                .build());
    }
}
