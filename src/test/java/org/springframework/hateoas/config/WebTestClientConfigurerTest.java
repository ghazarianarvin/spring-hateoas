package org.springframework.hateoas.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.hateoas.support.Employee;
import org.springframework.hateoas.support.WebFluxEmployeeController;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.MediaTypes.*;
import static org.springframework.hateoas.support.ContextTester.*;

public class WebTestClientConfigurerTest {

	private static MediaType FRODO_JSON = MediaType.parseMediaType("application/frodo+json");

	@Test // #1225
	void webTestClientConfigurerHandlesSingleHypermediaType() {

		withContext(HalConfig.class, context -> {

			WebTestClient webTestClient = WebTestClient.bindToServer().build();

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes)
					.doesNotContain(HAL_JSON, HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON);

			WebTestClientConfigurer webTestClientConfigurer = context.getBean(WebTestClientConfigurer.class);

			webTestClient = webTestClientConfigurer.registerHypermediaTypes(webTestClient);

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes) //
					.contains(HAL_JSON) //
					.doesNotContain(HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON);
		});
	}

	@Test // #1225
	void webTestClientConfigurerHandlesAllHypermediaTypes() {

		withContext(AllHypermediaConfig.class, context -> {

			WebTestClient webTestClient = WebTestClient.bindToServer().build();

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes)
					.doesNotContain(HAL_JSON, HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON);

			WebTestClientConfigurer webTestClientConfigurer = context.getBean(WebTestClientConfigurer.class);

			webTestClient = webTestClientConfigurer.registerHypermediaTypes(webTestClient);

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes) //
					.contains(HAL_JSON, HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON);
		});
	}

	@Test // #1225
	void webTestClientConfigurerHandlesCustomHypermediaTypes() {

		withContext(CustomHypermediaConfig.class, context -> {

			WebTestClient webTestClient = WebTestClient.bindToServer().build();

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes)
					.doesNotContain(HAL_JSON, FRODO_JSON, HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON);

			WebTestClientConfigurer webTestClientConfigurer = context.getBean(WebTestClientConfigurer.class);

			webTestClient = webTestClientConfigurer.registerHypermediaTypes(webTestClient);

			assertThat(exchangeStrategies(webTestClient).messageReaders())
					.flatExtracting(HttpMessageReader::getReadableMediaTypes) //
					.contains(HAL_JSON, FRODO_JSON) //
					.doesNotContain(HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON);
		});
	}

	// tag::web-test-client[]
	@Test // #1225
	void webTestClientShouldSupportHypermediaDeserialization() {

		// Configure an application context programmatically.
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(HalConfig.class); // <1>
		context.refresh();

		// Extract the WebTestClientConfigurer from the app context.
		WebTestClientConfigurer configurer = context.getBean(WebTestClientConfigurer.class);

		// Create an instance of a controller for testing
		WebFluxEmployeeController controller = new WebFluxEmployeeController();
		controller.reset();

		// Create a WebTestClient by binding to the controller.
		WebTestClient client = WebTestClient.bindToController(controller).build();

		// Apply hypermedia support.
		client = configurer.registerHypermediaTypes(client); // <2>

		// Exercise the controller.
		client.get().uri("http://localhost/employees") //
				.exchange() //
				.expectStatus().isOk() //
				.expectBody(new TypeReferences.CollectionModelType<EntityModel<Employee>>() {}) // <3>
				.consumeWith(result -> {
					CollectionModel<EntityModel<Employee>> model = result.getResponseBody(); // <4>

					// Assert against the hypermedia model.
					assertThat(model.getRequiredLink(IanaLinkRelations.SELF)).isEqualTo(Link.of("/employees"));
					assertThat(model.getContent()).hasSize(2);
				});
	}
	// end::web-test-client[]

	/**
	 * Extract the {@link ExchangeStrategies} from a {@link WebTestClient} to assert it has the proper message readers and
	 * writers.
	 *
	 * @param webTestClient
	 * @return
	 */
	private static ExchangeStrategies exchangeStrategies(WebTestClient webTestClient) {

		WebClient webClient = (WebClient) ReflectionTestUtils.getField(webTestClient, "webClient");

		return (ExchangeStrategies) ReflectionTestUtils
				.getField(ReflectionTestUtils.getField(webClient, "exchangeFunction"), "strategies");
	}

	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class HalConfig {

	}

	@EnableHypermediaSupport(
			type = { HypermediaType.HAL, HypermediaType.HAL_FORMS, HypermediaType.COLLECTION_JSON, HypermediaType.UBER })
	static class AllHypermediaConfig {

	}

	static class CustomHypermediaConfig extends HalConfig {

		@Bean
		HypermediaMappingInformation hypermediaMappingInformation() {
			return () -> Collections.singletonList(FRODO_JSON);
		}
	}
}