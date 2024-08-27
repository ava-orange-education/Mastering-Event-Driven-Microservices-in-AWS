package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;

public class LambdaJavaTests {

	@ParameterizedTest
	@Event(value = "src/test/resources/apigateway_rest_event.json", type = APIGatewayV2HTTPEvent.class)
	public void testLoadApiGatewayRestEvent(APIGatewayV2HTTPEvent event) {
		assertThat(event).isNotNull();

		var response = new LambdaHandler().handleRequest(event, null);

		assertThat(response).isNotNull();
		assertThat(response.getBody()).isNotNull();
	}
}
