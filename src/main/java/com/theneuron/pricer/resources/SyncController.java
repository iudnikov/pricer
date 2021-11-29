package com.theneuron.pricer.resources;

import com.theneuron.pricer.model.Guideline;
import com.theneuron.pricer.model.health.HealthView;
import com.theneuron.pricer.repo.GuidelineReader;
import com.theneuron.pricer.services.DirectivePublisher;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Api
@RequestMapping(value = "/sync")
@Slf4j
public class SyncController {

    final GuidelineReader guidelineReader;
    final DirectivePublisher directivePublisher;

    public SyncController(GuidelineReader guidelineReader, DirectivePublisher directivePublisher) {
        this.guidelineReader = guidelineReader;
        this.directivePublisher = directivePublisher;
    }

    @ApiOperation(
        nickname = "sync",
        value = "Performs the sync when bidder redeployed"
    )
    @ApiResponses(
        value = {
            @ApiResponse(code = 200, message = "OK"),
        }
    )

    @GetMapping(value = "")
    public ResponseEntity<String> sync() throws Exception {
        List<Guideline> guidelines = guidelineReader.readAll(g -> g.isActive() || g.isCompleted());
        guidelines.forEach(g -> log.debug("following guideline would be synchronised: {}", g));
        directivePublisher.publish(guidelines);
        return ResponseEntity.ok("OK");
    }

}
