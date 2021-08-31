package com.theneuron.pricer.services;

import com.theneuron.pricer.model.Directive;

public interface DirectivePublisher {
    void publish(Directive directive) throws Exception;
}
