/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.util.List;
import java.util.function.Consumer;

/**
 * Assembles {@link ExchangeStrategies} needed to wire a {@link WebTestClient} with hypermedia support.
 *
 * @author Greg Turnquist
 * @since 1.1
 */
@Configuration
public class WebTestClientConfigurer {

	private Consumer<ClientCodecConfigurer> configurer;

	/**
	 * Creates a new {@link WebTestClientConfigurer} for the given {@link ObjectMapper} and
	 * {@link HypermediaMappingInformation}s.
	 *
	 * @param mapper must not be {@literal null}.
	 * @param hypermediaTypes must not be {@literal null}.
	 */
	public WebTestClientConfigurer(ObjectMapper mapper, List<HypermediaMappingInformation> hypermediaTypes) {

		Assert.notNull(mapper, "mapper must not be null!");
		Assert.notNull(hypermediaTypes, "hypermediaTypes must not be null!");

		this.configurer = clientCodecConfigurer -> hypermediaTypes.forEach(hypermedia -> {

			ObjectMapper objectMapper = hypermedia.configureObjectMapper(mapper.copy());
			MimeType[] mimeTypes = hypermedia.getMediaTypes().toArray(new MimeType[0]);

			clientCodecConfigurer.customCodecs().registerWithDefaultConfig(new Jackson2JsonEncoder(objectMapper, mimeTypes));
			clientCodecConfigurer.customCodecs().registerWithDefaultConfig(new Jackson2JsonDecoder(objectMapper, mimeTypes));
		});
	}

	/**
	 * Register the proper {@link ExchangeStrategies} for a given {@link WebTestClient}.
	 *
	 * @param webTestClient
	 * @return mutated webTestClient with hypermedia support.
	 */
	public WebTestClient registerHypermediaTypes(WebTestClient webTestClient) {

		return webTestClient.mutate() //
				.codecs(this.configurer) //
				.build();
	}
}
