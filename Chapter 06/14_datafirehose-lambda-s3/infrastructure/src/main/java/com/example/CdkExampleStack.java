package com.example;

import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.kinesis.Stream;
import software.amazon.awscdk.services.kinesis.StreamMode;
import software.amazon.awscdk.services.kinesisfirehose.alpha.DeliveryStream;
import software.amazon.awscdk.services.kinesisfirehose.alpha.LambdaFunctionProcessor;
import software.amazon.awscdk.services.kinesisfirehose.destinations.alpha.S3Bucket;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
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

    private Stream createKinesisDataStream(String name, String streamName) {
    	var stream = Stream.Builder.create(this, name)
    			.streamName(streamName)
    			.streamMode(StreamMode.ON_DEMAND)
    			.build();
    	
    	return stream;
    }
    
    private DeliveryStream createDataFirehoseStream(String name, String streamName, Stream source, Bucket destination, LambdaFunctionProcessor transformerLambda) {
    	var firehose = DeliveryStream.Builder.create(this, name)
    			.deliveryStreamName(streamName)
    			.sourceStream(source)
    			.destinations(List.of(S3Bucket.Builder
    					.create(destination)
    					.processor(transformerLambda)
    					.dataOutputPrefix("clickstream-raw/")
    					.errorOutputPrefix("clickstream-raw-error/")
    					.build()))
    			.build();
    	
    	return firehose;
    }
    
    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var clickstreamTransformationServiceFunction = createLambdaFunction(
        		"Lambda_ClickstreamTransformationService", 
        		"clickstream-transformation-service", 
        		"../software/microservices/clickstream-transformation-service/target/clickstream-transformation-service.jar");
        
        var sourceStream = createKinesisDataStream(
        		"KinesisDataStream_OrderingSystemClickstream",
        		"OrderingSystemClickstream");
        
        var destinationBucket = new Bucket(this, "ClickstreamBucket");
        
    	var lambdaProcessor = LambdaFunctionProcessor.Builder.create(clickstreamTransformationServiceFunction)
                .build();
        
        var firehose = createDataFirehoseStream(
        		"DataFirehoseStream_OrderingSystemClickstream-Firehose", 
        		"OrderingSystemClickstream-Firehose", 
        		sourceStream, 
        		destinationBucket,
        		lambdaProcessor);
        
        new CfnOutput(this, "Lambda_ClickstreamTransformationService_ARN", CfnOutputProps.builder()
                .value(clickstreamTransformationServiceFunction.getFunctionArn())
                .build());
        
        new CfnOutput(this, "KinesisDataStream_OrderingSystemClickstream_ARN", CfnOutputProps.builder()
                .value(sourceStream.getStreamArn())
                .build());
        
        new CfnOutput(this, "KinesisDataStream_ClickstreamBucket_ARN", CfnOutputProps.builder()
                .value(destinationBucket.getBucketName())
                .build());
        
        new CfnOutput(this, "DataFirehoseStream_OrderingSystemClickstream-Firehose_ARN", CfnOutputProps.builder()
                .value(firehose.getDeliveryStreamArn())
                .build());
    }
}
