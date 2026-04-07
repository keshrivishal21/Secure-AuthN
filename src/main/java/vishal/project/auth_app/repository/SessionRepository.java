package vishal.project.auth_app.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import vishal.project.auth_app.entity.Session;
import vishal.project.auth_app.entity.User;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByUser(User user);

    Optional<Session> findByRefreshToken(String refreshToken);
}
