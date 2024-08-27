package com.example;

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
        		newOrderServiceFunction);
        
        var reserveInventoryServiceFunction = createLambdaFunction(
        		"Lambda_ReserveInventoryService", 
        		"reserve-inventory-service", 
        		"../software/microservices/reserve-inventory-service/target/reserve-inventory-service.jar");
        
        var processPaymentServiceFunction = createLambdaFunction(
        		"Lambda_ProcessPaymentService", 
        		"process-payment-service", 
        		"../software/microservices/process-payment-service/target/process-payment-service.jar");
        
        var fulfilOrderServiceFunction = createLambdaFunction(
        		"Lambda_FulfilOrderService", 
        		"fulfil-order-service", 
        		"../software/microservices/fulfil-order-service/target/fulfil-order-service.jar");
        
        
        var productsInventoryTable = createDynamoDBTable(
        		"DynamoDB_ProductsInventoryTable",
        		"products-inventory",
        		"inventoryId",
        		reserveInventoryServiceFunction);
        
        new CfnOutput(this, "Lambda_NewOrderService_ARN", CfnOutputProps.builder()
                .value(newOrderServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_ReserveInventoryService_ARN", CfnOutputProps.builder()
                .value(reserveInventoryServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_ProcessPaymentService_ARN", CfnOutputProps.builder()
                .value(processPaymentServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "Lambda_FulfilOrderService_ARN", CfnOutputProps.builder()
                .value(fulfilOrderServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_OrdersTable_Arn", CfnOutputProps.builder()
                .value(ordersTable.getTableArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_ProductsInventoryTable_Arn", CfnOutputProps.builder()
                .value(productsInventoryTable.getTableArn())
                .build());
    }
}
