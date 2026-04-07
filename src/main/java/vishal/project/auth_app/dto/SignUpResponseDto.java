package vishal.project.auth_app.dto;


import lombok.Data;
import vishal.project.auth_app.entity.enums.Role;

@Data
public class SignUpResponseDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean isActive;
}
