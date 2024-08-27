package com.example;

import java.util.Arrays;
import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscriptionProps;
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
    
    private Topic createSNSTopic(String name, String topicName, Function lambdaToGrantPublish, List<Queue> subscribeQueue) {
    	var topic = Topic.Builder.create(this, name)
    			.topicName(topicName)
    			.build();
    	
    	topic.grantPublish(lambdaToGrantPublish);
    	
    	for (var queue: subscribeQueue) {
    		var subscription = new SqsSubscription(queue, SqsSubscriptionProps.builder().rawMessageDelivery(true).build());
    		topic.addSubscription(subscription);
    	}
    	
    	return topic;
    	
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
        
        var processPaymentServiceFunction = createLambdaFunction(
        		"Lambda_ProcessPaymentService", 
        		"process-payment-service", 
        		"../software/microservices/process-payment-service/target/process-payment-service.jar");
        
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
        
        var orderCreatedForPaymentQueue = createSQSQueue(
        		"SQS_OrderCreatedForPaymentQueue",
        		"OrderCreatedForPaymentQueue",
        		processPaymentServiceFunction,
        		null);
        
        var inventoryReservedQueue = createSQSQueue(
        		"SQS_InventoryReservedQueue",
        		"InventoryReservedQueue",
        		null,
        		reserveInventoryServiceFunction);
        
        var paymentProcessedQueue = createSQSQueue(
        		"SQS_PaymentProcessedQueue",
        		"PaymentProcessedQueue",
        		null,
        		processPaymentServiceFunction);
        
        var orderCreatedTopic = createSNSTopic(
        		"SNS_OrderCreatedTopic",
        		"OrderCreatedTopic", 
        		newOrderServiceFunction,
        		Arrays.asList(orderCreatedForInventoryQueue, orderCreatedForPaymentQueue));
    	
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
        
        new CfnOutput(this, "SQS_OrderCreatedForPaymentQueue_Arn", CfnOutputProps.builder()
                .value(orderCreatedForPaymentQueue.getQueueArn())
                .build());
        
        new CfnOutput(this, "SQS_InventoryReservedQueue_Arn", CfnOutputProps.builder()
                .value(inventoryReservedQueue.getQueueArn())
                .build());
        
        new CfnOutput(this, "SQS_PaymentProcessedQueue_Arn", CfnOutputProps.builder()
                .value(paymentProcessedQueue.getQueueArn())
                .build());
        
        new CfnOutput(this, "SQS_OrderCreatedTopicArn", CfnOutputProps.builder()
                .value(orderCreatedTopic.getTopicArn())
                .build());
    }
}
