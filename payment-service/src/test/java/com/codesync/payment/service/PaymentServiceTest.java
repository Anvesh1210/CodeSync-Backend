package com.codesync.payment.service;

import com.codesync.payment.client.AuthClient;
import com.codesync.payment.dto.OrderRequest;
import com.codesync.payment.dto.VerifyRequest;
import com.codesync.payment.entity.Subscription;
import com.codesync.payment.entity.Transaction;
import com.codesync.payment.repository.SubscriptionRepository;
import com.codesync.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private PaymentService paymentService;

    private UUID userId;
    private String orderId;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = "order_123";
        
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "test_id");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret");

        mockTransaction = Transaction.builder()
                .userId(userId)
                .orderId(orderId)
                .amount(1249.0)
                .currency("INR")
                .status(Transaction.TransactionStatus.PENDING)
                .build();
    }

    @Test
    void getSubscriptionStatus_ShouldReturnExpired_WhenNotFound() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        Subscription.SubscriptionStatus status = paymentService.getSubscriptionStatus(userId);
        
        assertEquals(Subscription.SubscriptionStatus.EXPIRED, status);
    }

    @Test
    void getSubscriptionStatus_ShouldReturnStatus_WhenFound() {
        Subscription sub = Subscription.builder().status(Subscription.SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));
        
        Subscription.SubscriptionStatus status = paymentService.getSubscriptionStatus(userId);
        
        assertEquals(Subscription.SubscriptionStatus.ACTIVE, status);
    }

    @Test
    void cancelSubscription_ShouldUpdateStatus() {
        Subscription sub = Subscription.builder().userId(userId).status(Subscription.SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));
        
        paymentService.cancelSubscription(userId);
        
        assertEquals(Subscription.SubscriptionStatus.CANCELLED, sub.getStatus());
        verify(subscriptionRepository).save(sub);
        verify(authClient).updatePremiumStatus(any());
    }

    @Test
    void verifyPayment_ShouldThrow_WhenTransactionNotFound() {
        VerifyRequest request = new VerifyRequest();
        request.setOrderId(orderId);
        
        when(transactionRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> paymentService.verifyPayment(request));
    }

    @Test
    void verifyPayment_ShouldThrow_WhenSignatureInvalid() {
        VerifyRequest request = new VerifyRequest();
        request.setOrderId(orderId);
        request.setPaymentId("pay_123");
        request.setSignature("invalid_sig");
        
        when(transactionRepository.findByOrderId(orderId)).thenReturn(Optional.of(mockTransaction));
        
        // Note: Utils.verifyPaymentSignature is static, we can't easily mock it without additional dependencies.
        // But the catch block will handle the failure.
        assertThrows(RuntimeException.class, () -> paymentService.verifyPayment(request));
    }

    @Test
    void updateTransactionAndSubscription_ShouldExtendExpiry_WhenActive() {
        LocalDateTime now = LocalDateTime.now();
        Subscription sub = Subscription.builder()
                .userId(userId)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .expiryDate(now.plusDays(10))
                .build();
        
        mockTransaction.setStatus(Transaction.TransactionStatus.SUCCESS);
        mockTransaction.setAmount(1249.0);
        
        when(transactionRepository.findByOrderId(orderId)).thenReturn(Optional.of(mockTransaction));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));
        
        // Invoke private method using reflection to avoid UnnecessaryStubbingException
        ReflectionTestUtils.invokeMethod(paymentService, "updateTransactionAndSubscription", 
            orderId, "pay_123", Transaction.TransactionStatus.SUCCESS);
        
        assertEquals(Subscription.SubscriptionStatus.ACTIVE, sub.getStatus());
        // Initial 10 days + 1 month extension
        assertTrue(sub.getExpiryDate().isAfter(now.plusMonths(1)));
        verify(subscriptionRepository).save(sub);
        verify(authClient).updatePremiumStatus(any());
    }
}
