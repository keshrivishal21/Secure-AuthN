package vishal.project.auth_app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vishal.project.auth_app.dto.SubscriptionRequestDto;
import vishal.project.auth_app.dto.SubscriptionResponseDto;
import vishal.project.auth_app.service.SubscriptionService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponseDto> subscribe(@Valid @RequestBody SubscriptionRequestDto subscriptionRequestDto) {
        SubscriptionResponseDto subscriptionResponseDto = subscriptionService.subscribe(subscriptionRequestDto);
        return ResponseEntity.ok(subscriptionResponseDto);
    }
}
