package vishal.project.auth_app.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vishal.project.auth_app.dto.UpdateRoleDto;
import vishal.project.auth_app.dto.SignUpResponseDto;
import vishal.project.auth_app.entity.User;
import vishal.project.auth_app.entity.enums.Role;
import vishal.project.auth_app.exception.ResourceNotFoundException;
import vishal.project.auth_app.exception.UnauthorizedException;
import vishal.project.auth_app.repository.UserRepository;

import java.util.List;

import static vishal.project.auth_app.utils.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public List<SignUpResponseDto> getAllUsers() {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You are not allowed to view all users");
        }

        List<User> users = userRepository.findAll();
        return users.stream().map(user -> modelMapper.map(user, SignUpResponseDto.class)).toList();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    public SignUpResponseDto updateUserRole(UpdateRoleDto updateRoleDto) {
        User user = getUserById(updateRoleDto.getId());
        user.setRole(updateRoleDto.getRole());
        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, SignUpResponseDto.class);
    }

    public void deleteUser(Long id) {
        User currentUser = getCurrentUser();

        boolean isSelf = currentUser.getId() != null && currentUser.getId().equals(id);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isSelf && !isAdmin) {
            throw new UnauthorizedException("You are not allowed to delete this user");
        }

        User user = getUserById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    public SignUpResponseDto getUser(Long id) {
        User currentUser = getCurrentUser();

        boolean isSelf = currentUser.getId() != null && currentUser.getId().equals(id);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isSelf && !isAdmin) {
            throw new UnauthorizedException("You are not allowed to view this user");
        }

        User user = getUserById(id);
        return modelMapper.map(user, SignUpResponseDto.class);
    }
}
