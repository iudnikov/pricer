package com.theneuron.pricer.resources;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.Guideline;
import com.theneuron.pricer.repo.GuidelineReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GuidelineControllerDelegate {

    private final GuidelineReader guidelineReader;
    private final ObjectMapper objectMapper;

    public GuidelineControllerDelegate(GuidelineReader guidelineReader, ObjectMapper objectMapper) {
        this.guidelineReader = guidelineReader;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<String> getGuidelines() throws Exception {
        List<Guideline> guidelines = guidelineReader.readAll(null);
        return new ResponseEntity<>(objectMapper.writeValueAsString(guidelines), HttpStatus.OK);
    }
}
