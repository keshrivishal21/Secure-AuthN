package vishal.project.auth_app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vishal.project.auth_app.entity.enums.Role;

@Data
public class UpdateRoleDto {

    @NotNull(message = "User id is required")
    private Long id;

    @NotNull(message = "Role is required")
    private Role role;
}
