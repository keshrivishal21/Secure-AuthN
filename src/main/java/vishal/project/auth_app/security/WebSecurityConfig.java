package vishal.project.auth_app.security;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JWTAuthFilter jwtAuthFilter;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public WebSecurityConfig(
            JWTAuthFilter jwtAuthFilter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/error",

            // OpenAPI spec
            "/v3/api-docs/**",

            // Swagger UI (SpringDoc)
            "/swagger-ui/**",
            "/swagger-ui.html",

            // Swagger UI static resources
            "/webjars/**",
            "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exHandlingConfig -> exHandlingConfig
                        .accessDeniedHandler(accessDeniedHandler())
                        .authenticationEntryPoint((request, response, authException) ->
                                handlerExceptionResolver.resolveException(request, response, null, authException)
                        )
                );

        return httpSecurity.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
