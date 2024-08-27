package com.example;

import java.util.Arrays;
import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.events.ApiDestination;
import software.amazon.awscdk.services.events.Authorization;
import software.amazon.awscdk.services.events.Connection;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.IRuleTarget;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.sqs.Queue;
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
    
    private RestApi createOrderingPlatformApi(Function newOrderServiceLambdaFunction) {
        var restApi = LambdaRestApi.Builder.create(this, "ApiGateway_OrderingPlarformApi")
                .restApiName("ordering-platform-api")
                .handler(newOrderServiceLambdaFunction)
                .proxy(true)
                .build();

        var apiResource = restApi.getRoot().addResource("orders");
        apiResource.addMethod("POST", new LambdaIntegration(newOrderServiceLambdaFunction));
        
        return restApi;
    }
    
    private Table createDynamoDBTable(String name, String tableName, String partitionKey, Function lambdaToGrantAccess) {
    	var table = Table.Builder.create(this, name)
    			.tableName(tableName)
                .partitionKey(Attribute.builder()
                    .name(partitionKey)
                    .type(AttributeType.STRING)
                    .build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    	
    	table.grantReadWriteData(lambdaToGrantAccess);
    	
    	return table;
    }
    
    private Queue createSQSQueue(String name, String queueName, Function lambdaToGrantConsumeMessage, Function lambdaToGrantSendMessage) {
    	var queue = Queue.Builder.create(this, name)
    			.queueName(queueName)
    			.build();
    	
    	if (lambdaToGrantConsumeMessage != null) {
    		queue.grantConsumeMessages(lambdaToGrantConsumeMessage);
    		lambdaToGrantConsumeMessage.addEventSource(new SqsEventSource(queue));
    	}
    	
    	if (lambdaToGrantSendMessage != null) {
    		queue.grantSendMessages(lambdaToGrantSendMessage);
    	}
    	
    	
    	return queue;
    }
    
    private EventBus createOrderingSystemEventBus(String eventBusId, String eventBusName, String ruleId, String ruleName, List<IRuleTarget> targets) {
    	var customEventBus = EventBus.Builder.create(this, eventBusId)
    		.eventBusName(eventBusName)
    		.build();
    		
    	Rule.Builder.create(this, ruleId)
    			.ruleName(ruleName)
    			.eventPattern(EventPattern.builder()
        				.source(List.of("com.example.new-order-service"))
        				.detailType(List.of("OrderCreated"))
    					.build())
    			.eventBus(customEventBus)
    			.targets(targets)
    			.build();

    	return customEventBus;
    }
    
    private ApiDestination createAPIDestination(String name, String apiDestinationName) {
    	 var connection = Connection.Builder.create(this, name + "Connection")
    			 .connectionName(apiDestinationName + "Connection")
    	         .authorization(Authorization.basic("username", new SecretValue("password")))
    	         .build();

    	 var destination = ApiDestination.Builder.create(this, name)
    			 .apiDestinationName(apiDestinationName)
    	         .connection(connection)
    	         .endpoint("https://example.com/v1/*")
    	         .build();
    	 
    	 return destination;
    }

    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var newOrderServiceFunction = createLambdaFunction(
        		"Lambda_NewOrderService", 
        		"new-order-service", 
        		"../software/microservices/new-order-service/target/new-order-service.jar");
        
        var orderingPlatformApi = createOrderingPlatformApi(newOrderServiceFunction);
        
        var ordersTable = createDynamoDBTable(
        		"DynamoDB_OrdersTable",
        		"orders",
        		"orderId",
        		newOrderServiceFunction);
        
        var reserveInventoryServiceFunction = createLambdaFunction(
        		"Lambda_ReserveInventoryService", 
        		"reserve-inventory-service", 
        		"../software/microservices/reserve-inventory-service/target/reserve-inventory-service.jar");

        var productsInventoryTable = createDynamoDBTable(
        		"DynamoDB_ProductsInventoryTable",
        		"products-inventory",
        		"inventoryId",
        		reserveInventoryServiceFunction);
        
        var orderCreatedForInventoryQueue = createSQSQueue(
        		"SQS_OrderCreatedForInventoryQueue",
        		"OrderCreatedForInventoryQueue",
        		reserveInventoryServiceFunction,
        		null);
        
        var inventoryReservedQueue = createSQSQueue(
        		"SQS_InventoryReservedQueue",
        		"InventoryReservedQueue",
        		null,
        		reserveInventoryServiceFunction);
        
        var apiDestination = createAPIDestination(
        		"APIDestination_StripePaymentAPI",
        		"StripePaymentAPI");

        var orderingSystemEventBus = createOrderingSystemEventBus(
        		"EventBridge_OrderingSystemEventBus",
        		"OrderingSystemEventBus",
        		"EventBridge_OrderCreatedRule",
        		"OrderCreatedRule",
        		List.of(
        				new SqsQueue(orderCreatedForInventoryQueue),
        				new software.amazon.awscdk.services.events.targets.ApiDestination(apiDestination)));
        
        orderingSystemEventBus.grantPutEventsTo(newOrderServiceFunction);
    	
        new CfnOutput(this, "ApiGateway_OrderingPlarformApi_Endpoint", CfnOutputProps.builder()
                .value(orderingPlatformApi.getUrl())
                .build());
        
        new CfnOutput(this, "Lambda_NewOrderService_ARN", CfnOutputProps.builder()
                .value(newOrderServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_ReserveInventoryService_ARN", CfnOutputProps.builder()
                .value(reserveInventoryServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_OrdersTable_Arn", CfnOutputProps.builder()
                .value(ordersTable.getTableArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_ProductsInventoryTable_Arn", CfnOutputProps.builder()
                .value(productsInventoryTable.getTableArn())
                .build());
        
        new CfnOutput(this, "SQS_OrderCreatedForInventoryQueue_Arn", CfnOutputProps.builder()
                .value(orderCreatedForInventoryQueue.getQueueArn())
                .build());

        new CfnOutput(this, "SQS_InventoryReservedQueue_Arn", CfnOutputProps.builder()
                .value(inventoryReservedQueue.getQueueArn())
                .build());
        
        new CfnOutput(this, "SQS_OrderingSystemEventBusArn", CfnOutputProps.builder()
                .value(orderingSystemEventBus.getEventBusArn())
                .build());
    }
}
