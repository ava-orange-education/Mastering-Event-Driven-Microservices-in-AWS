package com.example;

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
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

public class CdkExampleStack extends Stack {
    
    public CdkExampleStack(final Construct scope, final String id) {
        this(scope, id, null);
    }
    
    private Function createNewOrderServiceLambdaFunction() {
    	var lambda = Function.Builder.create(this, "Lambda_NewOrderService")
    			.functionName("new-order-service")
                .runtime(Runtime.JAVA_21)
                .memorySize(512)
                .timeout(Duration.seconds(15))
                .code(Code.fromAsset("../software/microservices/new-order-service/target/new-order-service.jar"))
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
    
    private Table createOrdersTable(Function newOrderServiceLambdaFunction) {
    	var table = Table.Builder.create(this, "DynamoDB_OrdersTable")
    			.tableName("orders")
                .partitionKey(Attribute.builder()
                    .name("idempotencyKey")
                    .type(AttributeType.STRING)
                    .build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    	
    	table.grantReadWriteData(newOrderServiceLambdaFunction);
    	
    	return table;
    }

    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var newOrderServiceLambdaFunction = createNewOrderServiceLambdaFunction();
        var orderingPlatformApi = createOrderingPlatformApi(newOrderServiceLambdaFunction);
        var ordersTable = createOrdersTable(newOrderServiceLambdaFunction);

        new CfnOutput(this, "ApiGateway_OrderingPlarformApi_Endpoint", CfnOutputProps.builder()
                .value(orderingPlatformApi.getUrl())
                .build());
        
        new CfnOutput(this, "Lambda_NewOrderService_ARN", CfnOutputProps.builder()
                .value(newOrderServiceLambdaFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "DynamoDB_OrdersTable_Arn", CfnOutputProps.builder()
                .value(ordersTable.getTableArn())
                .build());
    }
}
