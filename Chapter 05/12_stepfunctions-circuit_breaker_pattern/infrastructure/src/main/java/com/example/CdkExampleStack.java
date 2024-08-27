package com.example;

import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
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
        
    private Table createDynamoDBTable(String name, String tableName, String partitionKey, List<Function> lambdaToGrantAccess) {
    	var table = Table.Builder.create(this, name)
    			.tableName(tableName)
                .partitionKey(Attribute.builder()
                    .name(partitionKey)
                    .type(AttributeType.STRING)
                    .build())
                .build();
    	
    	lambdaToGrantAccess.forEach(l -> table.grantReadWriteData(l));
    	
    	return table;
    }
    
    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var newOrderServiceFunction = createLambdaFunction(
        		"Lambda_NewOrderService", 
        		"new-order-service", 
        		"../software/microservices/new-order-service/target/new-order-service.jar");
        
        var ordersTable = createDynamoDBTable(
        		"DynamoDB_OrdersTable",
        		"orders",
        		"orderId",
        		List.of(newOrderServiceFunction));
        
        var getCircuitStatusServiceFunction = createLambdaFunction(
        		"Lambda_GetCircuitStatusService", 
        		"get-circuit-status-service", 
        		"../software/microservices/get-circuit-status-service/target/get-circuit-status-service.jar");
        
        var updateCircuitStatusServiceFunction = createLambdaFunction(
        		"Lambda_UpdateCircuitStatusService", 
        		"update-circuit-status-service", 
        		"../software/microservices/update-circuit-status-service/target/update-circuit-status-service.jar");
        
        var circuitBreakerTable = createDynamoDBTable(
        		"DynamoDB_CircuitBreakerTable",
        		"circuit-breaker",
        		"serviceName",
        		List.of(getCircuitStatusServiceFunction, updateCircuitStatusServiceFunction));
        
        new CfnOutput(this, "Lambda_NewOrderService_ARN", CfnOutputProps.builder()
                .value(newOrderServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_GetCircuitStatusService_ARN", CfnOutputProps.builder()
                .value(getCircuitStatusServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_UpdateCircuitStatusService_ARN", CfnOutputProps.builder()
                .value(updateCircuitStatusServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_OrdersTable_Arn", CfnOutputProps.builder()
                .value(ordersTable.getTableArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_CircuitBreakerTable_Arn", CfnOutputProps.builder()
                .value(circuitBreakerTable.getTableArn())
                .build());
    }
}
