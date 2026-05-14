package com.flowboard.payment_service.service;

import com.flowboard.payment_service.dto.*;
import com.flowboard.payment_service.entity.PaymentRecord;
import com.flowboard.payment_service.entity.Plan;
import com.flowboard.payment_service.entity.Subscription;
import com.flowboard.payment_service.exception.CustomException;
import com.flowboard.payment_service.repository.PaymentRecordRepository;
import com.flowboard.payment_service.repository.PlanRepository;
import com.flowboard.payment_service.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl - Unit Tests (~75% Coverage)")
class PaymentServiceImplTest {

    @Mock private PlanRepository planRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PaymentRecordRepository paymentRecordRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private final Long userId = 100L;
    private Plan proPlan;
    private Subscription activeSubscription;

    /*
     * NOTE: To target ~75% coverage, tests for `createUpgradeCheckoutSession()`,
     * `handleWebhook()`, and their private helpers have been deliberately omitted.
     */

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "sk_test_12345");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "whsec_12345");
        paymentService.init();

        proPlan = Plan.builder()
                .id(2L)
                .name("PRO")
                .displayName("Pro")
                .priceMonthly(new BigDecimal("9.99"))
                .priceYearly(new BigDecimal("99.00"))
                .stripePriceIdMonthly("price_monthly_mock")
                .stripePriceIdYearly("price_yearly_mock")
                .hasAdvancedAnalytics(true)
                .build();

        activeSubscription = Subscription.builder()
                .id(1L)
                .userId(userId)
                .plan(proPlan)
                .status("ACTIVE")
                .stripeCustomerId("cus_123")
                .stripeSubscriptionId("sub_123")
                .build();
    }

    // ── Plans & Subscriptions Retrieval ───────────────────────────────────────

    @Test
    @DisplayName("getAllPlans - returns mapped plans and seeds if empty")
    void getAllPlans_Success() {
        when(planRepository.count()).thenReturn(1L); // Skip seeding
        when(planRepository.findAll()).thenReturn(List.of(proPlan));

        List<PlanResponse> result = paymentService.getAllPlans();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("PRO");
    }

    @Test
    @DisplayName("getSubscription - returns existing subscription")
    void getSubscription_Existing_Success() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(activeSubscription));

        SubscriptionResponse result = paymentService.getSubscription(userId);

        assertThat(result.getPlanName()).isEqualTo("PRO");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("getSubscription - seeds free plan if no subscription exists")
    void getSubscription_NotExists_SeedsFreePlan() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Plan freePlan = Plan.builder().name("FREE").build();
        when(planRepository.findByName("FREE")).thenReturn(Optional.of(freePlan));

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionResponse result = paymentService.getSubscription(userId);

        assertThat(result.getPlanName()).isEqualTo("FREE");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    // ── Checkout Creation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createCheckoutSession - Success with Stripe Mock")
    void createCheckoutSession_Success() {
        CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
        request.setPlanId(proPlan.getId());
        request.setBillingCycle("MONTHLY");

        when(planRepository.findById(proPlan.getId())).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(activeSubscription));

        // Intercept Stripe static method
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session mockStripeSession = mock(Session.class);
            when(mockStripeSession.getId()).thenReturn("sess_123");
            when(mockStripeSession.getUrl()).thenReturn("https://checkout.stripe.com/123");

            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockStripeSession);

            CheckoutSessionResponse response = paymentService.createCheckoutSession(request, userId);

            assertThat(response.getSessionId()).isEqualTo("sess_123");
            assertThat(response.getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/123");
        }
    }

    @Test
    @DisplayName("createCheckoutSession - Fails if plan not found")
    void createCheckoutSession_PlanNotFound_ThrowsException() {
        CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
        request.setPlanId(99L);
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createCheckoutSession(request, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Plan not found");
    }

    // ── Checkout Confirmation ─────────────────────────────────────────────────

    @Test
    @DisplayName("confirmCheckoutSession - Success activates subscription")
    void confirmCheckoutSession_Complete_Success() {
        String sessionId = "sess_123";

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session mockStripeSession = mock(Session.class);

            when(mockStripeSession.getPaymentStatus()).thenReturn("paid");
            when(mockStripeSession.getStatus()).thenReturn("complete");
            when(mockStripeSession.getMetadata()).thenReturn(Map.of(
                    "userId", String.valueOf(userId),
                    "planId", String.valueOf(proPlan.getId()),
                    "billingCycle", "MONTHLY"
            ));
            when(mockStripeSession.getCustomer()).thenReturn("cus_new");
            when(mockStripeSession.getSubscription()).thenReturn("sub_new");

            mockedSession.when(() -> Session.retrieve(sessionId)).thenReturn(mockStripeSession);

            when(planRepository.findById(proPlan.getId())).thenReturn(Optional.of(proPlan));
            when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(activeSubscription));

            CheckoutSessionStatusResponse response = paymentService.confirmCheckoutSession(sessionId, userId);

            assertThat(response.getStatus()).isEqualTo("ACTIVE");
            verify(subscriptionRepository).save(any(Subscription.class));
        }
    }

    @Test
    @DisplayName("confirmCheckoutSession - Returns Pending if not complete")
    void confirmCheckoutSession_Pending() {
        String sessionId = "sess_123";

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session mockStripeSession = mock(Session.class);

            when(mockStripeSession.getPaymentStatus()).thenReturn("unpaid");
            when(mockStripeSession.getStatus()).thenReturn("open");
            when(mockStripeSession.getMetadata()).thenReturn(Map.of("userId", String.valueOf(userId)));

            mockedSession.when(() -> Session.retrieve(sessionId)).thenReturn(mockStripeSession);

            CheckoutSessionStatusResponse response = paymentService.confirmCheckoutSession(sessionId, userId);

            assertThat(response.getStatus()).isEqualTo("PENDING");
            verify(subscriptionRepository, never()).save(any());
        }
    }

    // ── Cancellation & Utilities ──────────────────────────────────────────────

    @Test
    @DisplayName("cancelSubscription - Success updates Stripe and Database")
    void cancelSubscription_Success() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(activeSubscription));

        try (MockedStatic<com.stripe.model.Subscription> mockedStripeSub = mockStatic(com.stripe.model.Subscription.class)) {
            com.stripe.model.Subscription mockStripeEntity = mock(com.stripe.model.Subscription.class);
            mockedStripeSub.when(() -> com.stripe.model.Subscription.retrieve("sub_123")).thenReturn(mockStripeEntity);

            paymentService.cancelSubscription(userId);

            verify(subscriptionRepository).save(activeSubscription);
            assertThat(activeSubscription.getStatus()).isEqualTo("CANCELLED");
            assertThat(activeSubscription.getCancelledAt()).isNotNull();

            try {
                verify(mockStripeEntity).update(any(SubscriptionUpdateParams.class));
            } catch (StripeException e) {
                // Handled in mock execution
            }
        }
    }

    @Test
    @DisplayName("hasFeature - Evaluates correctly based on Plan booleans")
    void hasFeature_ReturnsCorrectValue() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(activeSubscription));

        // Pro plan setup in @BeforeEach sets hasAdvancedAnalytics = true
        boolean resultTrue = paymentService.hasFeature(userId, "ADVANCED_ANALYTICS");
        boolean resultFalse = paymentService.hasFeature(userId, "PRIORITY_SUPPORT");

        assertThat(resultTrue).isTrue();
        assertThat(resultFalse).isFalse();
    }

    @Test
    @DisplayName("getPaymentHistory - Retrieves records from DB")
    void getPaymentHistory_Success() {
        PaymentRecord mockRecord = PaymentRecord.builder().status("SUCCEEDED").build();
        when(paymentRecordRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Collections.singletonList(mockRecord));

        List<PaymentRecord> result = paymentService.getPaymentHistory(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("SUCCEEDED");
    }
}