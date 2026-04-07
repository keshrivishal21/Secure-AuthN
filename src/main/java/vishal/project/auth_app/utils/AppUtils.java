package vishal.project.auth_app.utils;

import org.springframework.security.core.context.SecurityContextHolder;
import vishal.project.auth_app.entity.User;

public class AppUtils {

    public static User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
