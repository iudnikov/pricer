package com.theneuron.pricer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfigLocal;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class BidEvidenceSerializationTest {

    ObjectMapper objectMapper = AppConfigLocal.objectMapper();

    @Test
    public void should_not_deserialize_methods() throws JsonProcessingException {
        BidEvidence bidEvidence = BidEvidence.builder()
                .minPrice(BigDecimal.valueOf(10))
                .maxPrice(BigDecimal.valueOf(20))
                .actualPrice(BigDecimal.valueOf(15))
                .currencyCode("USD")
                .build();

        String result = objectMapper.writeValueAsString(bidEvidence);
        assertFalse(result.contains("priceIncreaseCapacity"));
        assertFalse(result.contains("priceReduceCapacity"));
        assertFalse(result.contains("actualPriceMoney"));
        System.out.println(result);
    }

}