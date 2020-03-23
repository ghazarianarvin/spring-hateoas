package org.springframework.hateoas.client;

import org.springframework.hateoas.config.WebConverters;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// tag::code[]
@Service
public class SampleService {

    private WebConverters webConverters;

    public SampleService(WebConverters webConverters) { // <1>
        this.webConverters = webConverters;
    }

    void doSomething() {
        RestTemplate template = new RestTemplate(); // <2>

        template.setMessageConverters(webConverters.and(template.getMessageConverters())); // <3>

        // Your template is now set up to do hypermedia!
    }
}
// end::code[]
