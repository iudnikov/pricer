package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.Guideline;

public interface GuidelineWriter {
    void write(Guideline guideline) throws Exception;
}
