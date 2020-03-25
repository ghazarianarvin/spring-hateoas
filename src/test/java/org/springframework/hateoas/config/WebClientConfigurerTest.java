package org.springframework.hateoas.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.MediaTypes.*;
import static org.springframework.hateoas.support.ContextTester.*;

public class WebClientConfigurerTest {

	private static MediaType FRODO_JSON = MediaType.parseMediaType("application/frodo+json");

	@Test // #1224
	void webClientConfigurerHandlesSingleHypermediaType() {

		withContext(HalConfig.class, context -> {

			WebClient webClient = WebClient.create();

			Verifier.of(webClient) //
					.doesNotContain(HAL_JSON, HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON) //
					.verify();

			WebClientConfigurer configurer = context.getBean(WebClientConfigurer.class);

			webClient = configurer.registerHypermediaTypes(webClient);

			Verifier.of(webClient) //
					.contains(HAL_JSON) //
					.doesNotContain(HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON) //
					.verify();
		});
	}

	@Test // #1224
	void webClientConfigurerHandlesMultipleHypermediaTypes() {

		withContext(AllHypermediaConfig.class, context -> {

			WebClient webClient = WebClient.create();

			Verifier.of(webClient) //
					.doesNotContain(HAL_JSON, HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON) //
					.verify();

			WebClientConfigurer configurer = context.getBean(WebClientConfigurer.class);

			webClient = configurer.registerHypermediaTypes(webClient);

			Verifier.of(webClient) //
					.contains(HAL_JSON, HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON) //
					.verify();
		});
	}

	@Test // #1224
	void webClientConfigurerHandlesCustomHypermediaTypes() {

		withContext(CustomHypermediaConfig.class, context -> {

			WebClient webClient = WebClient.create();

			Verifier.of(webClient) //
					.doesNotContain(HAL_JSON, FRODO_JSON, HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON) //
					.verify();

			WebClientConfigurer configurer = context.getBean(WebClientConfigurer.class);

			webClient = configurer.registerHypermediaTypes(webClient);

			Verifier.of(webClient) //
					.contains(HAL_JSON, FRODO_JSON).doesNotContain(HAL_FORMS_JSON, COLLECTION_JSON, UBER_JSON) //
					.verify();
		});
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

	/**
	 * This classes uses reflection to verify {@link WebClient} has registered {@link MediaType}s.
	 *
	 * @author Greg Turnquist
	 * @since 1.1
	 */
	private static class Verifier {

		private WebClient webClient;
		private MediaType[] contains = new MediaType[0];
		private MediaType[] doesNotContain = new MediaType[0];

		private Verifier(WebClient webClient) {
			this.webClient = webClient;
		}

		static Verifier of(WebClient webClient) {
			return new Verifier(webClient);
		}

		Verifier contains(MediaType... contains) {
			this.contains = contains;
			return this;
		}

		Verifier doesNotContain(MediaType... doesNotContain) {
			this.doesNotContain = doesNotContain;
			return this;
		}

		void verify() {

			ExchangeStrategies strategies = (ExchangeStrategies) ReflectionTestUtils
					.getField(ReflectionTestUtils.getField(this.webClient, "exchangeFunction"), "strategies");

			if (this.contains.length > 0) {

				assertThat(strategies.messageReaders()).flatExtracting(HttpMessageReader::getReadableMediaTypes)
						.contains(this.contains);
				assertThat(strategies.messageWriters()).flatExtracting(HttpMessageWriter::getWritableMediaTypes)
						.contains(this.contains);
			}

			if (this.doesNotContain.length > 0) {
				assertThat(strategies.messageReaders()).flatExtracting(HttpMessageReader::getReadableMediaTypes)
						.doesNotContain(this.doesNotContain);
				assertThat(strategies.messageWriters()).flatExtracting(HttpMessageWriter::getWritableMediaTypes)
						.doesNotContain(this.doesNotContain);
			}
		}
	}
}
