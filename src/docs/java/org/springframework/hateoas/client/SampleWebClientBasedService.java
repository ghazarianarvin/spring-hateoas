package org.springframework.hateoas.client;

import org.springframework.hateoas.config.WebClientConfigurer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

// tag::code[]
@Service
public class SampleWebClientBasedService {

    private WebClientConfigurer webClientConfigurer;

    public SampleWebClientBasedService(WebClientConfigurer webClientConfigurer) { // <1>
        this.webClientConfigurer = webClientConfigurer;
    }

    void doSomething() {
        WebClient client = WebClient.create();

        client = webClientConfigurer.registerHypermediaTypes(client); // <3>

        // Your client is now ready to speak hypermedia!
    }
}
// end::code[]
