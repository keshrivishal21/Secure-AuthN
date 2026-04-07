package vishal.project.auth_app.security;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vishal.project.auth_app.dto.LoginRequestDto;
import vishal.project.auth_app.dto.LoginResponseDto;
import vishal.project.auth_app.dto.SignUpRequestDto;
import vishal.project.auth_app.dto.SignUpResponseDto;
import vishal.project.auth_app.entity.User;
import vishal.project.auth_app.entity.enums.Role;
import vishal.project.auth_app.exception.BadRequestException;
import vishal.project.auth_app.repository.UserRepository;
import vishal.project.auth_app.service.SessionService;
import vishal.project.auth_app.service.UserService;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SessionService sessionService;

    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto) {

        Optional<User>user = userRepository.findByEmail(signUpRequestDto.getEmail());

        if (user.isPresent()) {
            throw new BadRequestException("User with email already exists");
        }

        User newUser = modelMapper.map(signUpRequestDto, User.class);
        newUser.setRole(Role.USER);
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newUser.setActive(true);
        User savedUser = userRepository.save(newUser);
        return modelMapper.map(savedUser, SignUpResponseDto.class);
    }

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        sessionService.generateNewSession(user,refreshToken);
        return new LoginResponseDto(user.getId(), accessToken,refreshToken);
    }

    public LoginResponseDto refreshToken(String refreshToken){
        Long userId = jwtService.getUserIdFromToken(refreshToken);
        sessionService.validateSession(refreshToken);
        User user = userService.getUserById(userId);
        String accessToken = jwtService.generateAccessToken(user);
        return new LoginResponseDto(user.getId(), accessToken,refreshToken);
    }

    @Transactional
    public void logout(String refreshToken){
        sessionService.deleteSession(refreshToken);
    }
}
