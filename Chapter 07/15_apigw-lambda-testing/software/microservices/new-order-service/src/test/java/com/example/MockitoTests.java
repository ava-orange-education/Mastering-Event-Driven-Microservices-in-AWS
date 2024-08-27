package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

@ExtendWith(MockitoExtension.class)
public class MockitoTests {

	@Mock
	private APIGatewayV2HTTPEvent event;
	
	private String body = "{\"customerId\":\"123\",\"items\":[\"item1\",\"item2\",\"item3\"],\"total\":\"74.5\",\"paymentDetails\":{\"paymentMethod\":\"CARD\",\"paymentTransactionId\":\"FDS232SGFDF341934\"},\"shippingDetails\":{\"receiverName\":\"John Doe\",\"receiverPhoneNumber\":\"123-456-7890\",\"shippingAddressLine1\":\"123 Main St\",\"shippingAddressLine2\":\"Apt 101\",\"shippingCity\":\"Anytown\",\"shippingState\":\"CA\",\"shippingZipCode\":\"12345\",\"shippingCountry\":\"USA\"}}";
	
	@Test
	public void testLoadApiGatewayRestEvent() throws IOException {
		Mockito.when(event.getBody()).thenReturn(body);

		assertThat(event).isNotNull();

		var response = new LambdaHandler().handleRequest(event, null);

		assertThat(response).isNotNull();
		assertThat(response.getBody()).isNotNull();
	}
}
