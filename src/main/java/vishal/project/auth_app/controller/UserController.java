package vishal.project.auth_app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vishal.project.auth_app.dto.SignUpResponseDto;
import vishal.project.auth_app.dto.UpdateRoleDto;
import vishal.project.auth_app.service.UserService;


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SignUpResponseDto>> getAllUsers() {
        List<SignUpResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SignUpResponseDto> getUserById(@PathVariable Long id) {
        SignUpResponseDto user = userService.getUser(id);
        return ResponseEntity.ok(user);
    }


    @PatchMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SignUpResponseDto> updateUserRole(@Valid @RequestBody UpdateRoleDto updateRoleDto) {
        SignUpResponseDto userDto = userService.updateUserRole(updateRoleDto);
        return ResponseEntity.ok().body(userDto);
    }
}
