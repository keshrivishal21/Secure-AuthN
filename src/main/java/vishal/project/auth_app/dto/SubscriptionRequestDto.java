package vishal.project.auth_app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vishal.project.auth_app.entity.enums.PlanType;

@Data
public class SubscriptionRequestDto {
    @NotNull(message = "Plan type is required")
    private PlanType plan;
}
