package com.example;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkExampleApp {
    public static void main(final String[] args) {
        App app = new App();
        
        var env = Environment.builder()
        		.region("eu-central-1")
        		.build();

        new CdkExampleStack(app, "CdkExampleStack", StackProps.builder()
        		.env(env)
                .build());

        app.synth();
    }
}

