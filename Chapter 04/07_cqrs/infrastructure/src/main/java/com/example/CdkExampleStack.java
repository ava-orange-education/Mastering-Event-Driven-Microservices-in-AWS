package com.example;

import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.StreamViewType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.pipes.CfnPipe;
import software.amazon.awscdk.services.pipes.CfnPipe.PipeSourceDynamoDBStreamParametersProperty;
import software.amazon.awscdk.services.pipes.CfnPipe.PipeSourceParametersProperty;
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
    
    private RestApi createOrderingPlatformApi(Function orderCommandServiceLambdaFunction) {
        var restApi = LambdaRestApi.Builder.create(this, "ApiGateway_OrderingPlarformApi")
                .restApiName("ordering-platform-api")
                .handler(orderCommandServiceLambdaFunction)
                .proxy(true)
                .build();

        var apiResource = restApi.getRoot().addResource("orders");
        apiResource.addMethod("POST", new LambdaIntegration(orderCommandServiceLambdaFunction));
        
        return restApi;
    }
    
    private Table createDynamoDBTable(String name, String tableName, String partitionKey, Function lambdaToGrantAccess) {
    	var table = Table.Builder.create(this, name)
    			.tableName(tableName)
                .partitionKey(Attribute.builder()
                    .name(partitionKey)
                    .type(AttributeType.STRING)
                    .build())
                .stream(StreamViewType.NEW_AND_OLD_IMAGES)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    	
    	table.grantReadWriteData(lambdaToGrantAccess);
    	
    	return table;
    }
    
    private CfnPipe createEventBridgePipe(String name, String pipeName, String sourceArn, String targetArn) {
    	// this is only for testing - in production systems try to apply the least privilege principle
    	var eventBridgePipeRole = Role.Builder.create(this, name + "Role")
    			.roleName(pipeName + "Role")
    			.assumedBy(new ServicePrincipal("pipes.amazonaws.com"))
    	        .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("AdministratorAccess"))) 
    	        .build();
    	
    	var eventBridgePipe = CfnPipe.Builder.create(this, pipeName)
    			.roleArn(eventBridgePipeRole.getRoleArn())
    	        .source(sourceArn)
    	        .sourceParameters(PipeSourceParametersProperty.builder()
    	        		.dynamoDbStreamParameters(PipeSourceDynamoDBStreamParametersProperty.builder()
    	                         .startingPosition("LATEST")
    	                         .build())
    	        		.build())
    	        .target(targetArn)
    	        .build();
    	
    	return eventBridgePipe;
    }

    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var orderCommandServiceFunction = createLambdaFunction(
        		"Lambda_OrderCommandService", 
        		"order-command-service", 
        		"../software/microservices/order-command-service/target/order-command-service.jar");
        
        var orderingPlatformApi = createOrderingPlatformApi(orderCommandServiceFunction);
        
        var ordersWriteTable = createDynamoDBTable(
        		"DynamoDB_OrdersWriteTable",
        		"orders-write",
        		"orderId",
        		orderCommandServiceFunction);
        
        var orderQueryServiceFunction = createLambdaFunction(
        		"Lambda_OrderQueryService", 
        		"order-query-service", 
        		"../software/microservices/order-query-service/target/order-query-service.jar");

        var ordersReadTable = createDynamoDBTable(
        		"DynamoDB_OrdersReadTable",
        		"orders-read",
        		"orderId",
        		orderQueryServiceFunction);
        
        var orderTransformServiceFunction = createLambdaFunction(
        		"Lambda_OrderTransformService", 
        		"order-transform-service", 
        		"../software/microservices/order-transform-service/target/order-transform-service.jar");
        
        var eventBridgePipe = createEventBridgePipe(
        		"EventBridgePipe_NewOrdersPipe",
        		"NewOrdersPipe",
        		ordersWriteTable.getTableStreamArn(),
        		orderTransformServiceFunction.getFunctionArn());
        
        new CfnOutput(this, "ApiGateway_OrderingPlarformApi_Endpoint", CfnOutputProps.builder()
                .value(orderingPlatformApi.getUrl())
                .build());
        
        new CfnOutput(this, "Lambda_OrderCommandService_ARN", CfnOutputProps.builder()
                .value(orderCommandServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_OrderQueryService_ARN", CfnOutputProps.builder()
                .value(orderQueryServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_OrderTransformService_ARN", CfnOutputProps.builder()
                .value(orderTransformServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_ReserveInventoryService_ARN", CfnOutputProps.builder()
                .value(orderQueryServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_OrdersWriteTable_Arn", CfnOutputProps.builder()
                .value(ordersWriteTable.getTableArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_OrdersReadTable_Arn", CfnOutputProps.builder()
                .value(ordersReadTable.getTableArn())
                .build());
        
        new CfnOutput(this, "EventBridgePipe_NewOrdersPipe_Arn", CfnOutputProps.builder()
                .value(eventBridgePipe.getAttrArn())
                .build());
    }
}
