package com.theneuron.pricer.model;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GuidelineTest {

    @Test
    public void should_return_latest_lor() {
        final Instant now = Instant.now();
        Guideline guideline = Guideline.builder()
                .directive(
                        Directive.builder()
                                .type(DirectiveType.EXPLORATION)
                                .timestamp(now.plus(1, ChronoUnit.SECONDS))
                                .directiveId(UUID.randomUUID())
                                .requestId("two")
                                .screenId("two")
                                .lineItemId("two")
                                .build())
                .directive(
                        Directive.builder()
                                .type(DirectiveType.EXPLORATION)
                                .timestamp(now)
                                .directiveId(UUID.randomUUID())
                                .requestId("one")
                                .screenId("one")
                                .lineItemId("one")
                                .build())

                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .build();

        assertEquals("two", guideline.getLatestLor().getRequestId());
        assertFalse(guideline.getLatestLoi().isPresent());

    }

    @Test
    public void should_return_latest_loi() {
        final Instant now = Instant.now();
        Guideline guideline = Guideline.builder()
                .directive(
                        Directive.builder()
                                .type(DirectiveType.EXPLOITATION)
                                .timestamp(now.plus(1, ChronoUnit.SECONDS))
                                .directiveId(UUID.randomUUID())
                                .requestId("two")
                                .screenId("two")
                                .lineItemId("two")
                                .build())
                .directive(
                        Directive.builder()
                                .type(DirectiveType.EXPLOITATION)
                                .timestamp(now)
                                .directiveId(UUID.randomUUID())
                                .requestId("one")
                                .screenId("one")
                                .lineItemId("one")
                                .build())
                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .build();

        assertEquals("two", guideline.getLatestLoi().get().getRequestId());
        assertThrows(Exception.class, guideline::getLatestLor);
    }

    @Test
    public void should_validate_first_directive_is_always_lor() {
        final Instant now = Instant.now();
        Guideline guideline = Guideline.builder()
                .directive(
                        Directive.builder()
                                .type(DirectiveType.EXPLOITATION)
                                .timestamp(now)
                                .directiveId(UUID.randomUUID())
                                .requestId("one")
                                .screenId("one")
                                .lineItemId("one")
                                .build())
                .directive(
                        Directive.builder()
                                .type(DirectiveType.EXPLORATION)
                                .timestamp(now.plus(1, ChronoUnit.SECONDS))
                                .directiveId(UUID.randomUUID())
                                .requestId("two")
                                .screenId("two")
                                .lineItemId("two")
                                .build())
                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .build();

        assertFalse(guideline.isValid());
    }

}