package com.theneuron.pricer.resources;

import com.theneuron.pricer.model.health.HealthView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api
@RequestMapping(value = "/health")
@Slf4j
public class HealthController {

    @ApiOperation(
        nickname = "health",
        value = "Performs the health check"
    )
    @ApiResponses(
        value = {
            @ApiResponse(code = 200, message = "Health is OK"),
        }
    )

    @GetMapping(value = "")
    public HealthView getHealth() {
        log.debug("checking health");
        return HealthView.builder().state("OK").build();
    }

}
