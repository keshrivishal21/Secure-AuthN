package vishal.project.auth_app.dto;

import lombok.Data;
import vishal.project.auth_app.entity.enums.PlanType;

import java.time.Instant;

@Data
public class SubscriptionResponseDto {
    private Long id;
    private PlanType planType;
    private boolean isActive;
    private Instant startAt;
    private Instant endAt;
    private String user;
}
