package com.theneuron.pricer.services;

import com.theneuron.pricer.model.Directive;
import com.theneuron.pricer.model.Guideline;

import java.util.List;

public interface DirectivePublisher {
    void publish(Directive directive) throws Exception;
    void publish(List<Guideline> directives) throws Exception;
}
