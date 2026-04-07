package vishal.project.auth_app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Service;
import vishal.project.auth_app.entity.Session;
import vishal.project.auth_app.entity.User;
import vishal.project.auth_app.entity.enums.PlanType;
import vishal.project.auth_app.repository.SessionRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SubscriptionService subscriptionService;

    @Value("${app.session.limit:2}")
    private int defaultSessionLimit;

    @Value("${app.session.limit.free:1}")
    private int freeSessionLimit;

    @Value("${app.session.limit.basic:3}")
    private int basicSessionLimit;

    @Value("${app.session.limit.premium:5}")
    private int premiumSessionLimit;

    public void generateNewSession(User user, String refreshToken) {
        int sessionLimit = resolveSessionLimit(user);
        if (sessionLimit < 1) {
            sessionLimit = 1;
        }

        List<Session> userSessions = sessionRepository.findByUser(user);

        // If somehow we already have >= limit, remove least recently used until there's room for the new one.
        if (userSessions.size() >= sessionLimit) {
            userSessions.sort(Comparator.comparing(
                    Session::getLastUsedAt,
                    Comparator.nullsFirst(Comparator.naturalOrder())
            ));
            int toDelete = (userSessions.size() - sessionLimit) + 1;
            for (int i = 0; i < toDelete; i++) {
                sessionRepository.delete(userSessions.get(i));
            }
        }

        Session newSession = Session.builder()
                .user(user)
                .refreshToken(refreshToken)
                .build();
        sessionRepository.save(newSession);
    }

    public void validateSession(String refreshToken) {
        Session session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new SessionAuthenticationException("Session not found for refreshToken: " + refreshToken));
        session.setLastUsedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    public void deleteSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        sessionRepository.findByRefreshToken(refreshToken).ifPresent(sessionRepository::delete);
    }

    private int resolveSessionLimit(User user) {
        PlanType planType = subscriptionService.getActivePlan(user); // returns FREE if expired/no plan
        int limit = switch (planType) {
            case FREE -> freeSessionLimit;
            case BASIC -> basicSessionLimit;
            case PREMIUM -> premiumSessionLimit;
        };

        // final safety: never return < 1
        if (limit < 1) {
            limit = defaultSessionLimit;
        }
        if (limit < 1) {
            limit = 1;
        }
        return limit;
    }
}
