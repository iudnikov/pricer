package com.theneuron.pricer.resources;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api
@RequestMapping(value = "/guidelines")
@Slf4j
public class GuidelineController {

    private final GuidelineControllerDelegate delegate;

    public GuidelineController(GuidelineControllerDelegate delegate) {
        this.delegate = delegate;
    }

    @ApiOperation(
            nickname = "guidelines",
            value = "returns list of current guidelines with nested directives"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Health is OK"),
            }
    )

    @GetMapping(value = "")
    public ResponseEntity<String> getGuidelines() {
        try {
            return delegate.getGuidelines();
        } catch (Exception e) {
            log.error("can't read guidelines", e);
            return new ResponseEntity<>("something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
