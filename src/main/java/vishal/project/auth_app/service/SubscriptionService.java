package vishal.project.auth_app.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import vishal.project.auth_app.dto.SubscriptionRequestDto;
import vishal.project.auth_app.dto.SubscriptionResponseDto;
import vishal.project.auth_app.entity.Subscription;
import vishal.project.auth_app.entity.User;
import vishal.project.auth_app.entity.enums.PlanType;
import vishal.project.auth_app.exception.BadRequestException;
import vishal.project.auth_app.repository.SubscriptionRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static vishal.project.auth_app.utils.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ModelMapper modelMapper;

    public PlanType getActivePlan(User user) {
        if (user == null) {
            return PlanType.FREE;
        }

        Instant now = Instant.now();

        // Active means: isActive=true AND (endAt is null OR endAt > now)
        return subscriptionRepository.findByUserAndIsActiveTrueAndEndAtIsNull(user)
                .or(() -> subscriptionRepository.findByUserAndIsActiveTrueAndEndAtAfter(user, now))
                .map(Subscription::getPlanType)
                .orElse(PlanType.FREE);
    }

    public SubscriptionResponseDto subscribe(SubscriptionRequestDto subscriptionRequestDto) {
        if (subscriptionRequestDto == null || subscriptionRequestDto.getPlan() == null) {
            throw new BadRequestException("Plan type is required");
        }

        User currentUser = getCurrentUser();
        Instant now = Instant.now();

        boolean hasActive = subscriptionRepository.findByUserAndIsActiveTrueAndEndAtIsNull(currentUser).isPresent()
                || subscriptionRepository.findByUserAndIsActiveTrueAndEndAtAfter(currentUser, now).isPresent();
        if (hasActive) {
            throw new BadRequestException("User already has an active subscription");
        }

        Subscription subscription = new Subscription();
        subscription.setUser(currentUser);
        subscription.setPlanType(subscriptionRequestDto.getPlan());
        subscription.setActive(true);
        subscription.setStartAt(now);
        subscription.setEndAt(calculateEndAt(now, subscriptionRequestDto.getPlan()));

        subscription = subscriptionRepository.save(subscription);

        SubscriptionResponseDto responseDto = modelMapper.map(subscription, SubscriptionResponseDto.class);
        responseDto.setUser(currentUser.getUsername());
        return responseDto;
    }

    private Instant calculateEndAt(Instant start, PlanType planType) {
        return switch (planType) {
            case FREE -> null;
            case BASIC, PREMIUM -> start.plus(30, ChronoUnit.DAYS);
        };
    }
}
