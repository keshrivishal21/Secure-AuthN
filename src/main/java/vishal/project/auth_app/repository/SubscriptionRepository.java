package vishal.project.auth_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vishal.project.auth_app.entity.Subscription;
import vishal.project.auth_app.entity.User;

import java.time.Instant;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserAndIsActiveTrue(User user);

    Optional<Subscription> findByUserAndIsActiveTrueAndEndAtAfter(User user, Instant now);

    Optional<Subscription> findByUserAndIsActiveTrueAndEndAtIsNull(User user);
}