package com.theneuron.pricer.services;

import com.theneuron.pricer.model.Directive;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-local.properties")
class DirectivePublisherSNSImplTest {

    @Autowired
    DirectivePublisherSNSImpl directivePublisherSNS;

    @Test
    void publish() throws Exception {
        directivePublisherSNS.publish(Directive.builder().directiveId(UUID.randomUUID()).build());
    }
}