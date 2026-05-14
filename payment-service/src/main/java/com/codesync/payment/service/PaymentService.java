package com.codesync.payment.service;

import com.codesync.payment.client.AuthClient;
import com.codesync.payment.dto.OrderRequest;
import com.codesync.payment.dto.OrderResponse;
import com.codesync.payment.dto.VerifyRequest;
import com.codesync.payment.entity.Subscription;
import com.codesync.payment.entity.Transaction;
import com.codesync.payment.repository.SubscriptionRepository;
import com.codesync.payment.repository.TransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final AuthClient authClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (razorpayKeyId == null || razorpayKeyId.isEmpty() || razorpayKeyId.equals("${razorpay.key.id}")) {
            log.error("RAZORPAY KEY ID IS MISSING! Check application.properties");
        } else {
            log.info("Razorpay initialized with Key ID: {}...", razorpayKeyId.substring(0, 8));
        }
    }

    public OrderResponse createOrder(OrderRequest request) throws RazorpayException {
        log.info("Creating order for user {} | amount: {}", request.getUserId(), request.getAmount());
        
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) (request.getAmount() * 100)); // amount in paise
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", "txn_" + UUID.randomUUID().toString().substring(0, 8));

        Order order = client.orders.create(orderRequest);
        String orderId = order.get("id");

        // Save transaction as pending
        Transaction transaction = Transaction.builder()
                .userId(request.getUserId())
                .orderId(orderId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(Transaction.TransactionStatus.PENDING)
                .build();
        transactionRepository.save(transaction);

        return OrderResponse.builder()
                .orderId(orderId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .keyId(razorpayKeyId)
                .build();
    }

    @Transactional
    public boolean verifyPayment(VerifyRequest request) {
        log.info("Verifying payment for order: {}, payment: {}, userId: {}", 
            request.getOrderId(), request.getPaymentId(), request.getUserId());
        
        // 1. Check if transaction exists
        Optional<Transaction> transactionOpt = transactionRepository.findByOrderId(request.getOrderId());
        if (transactionOpt.isEmpty()) {
            log.error("CRITICAL: Transaction NOT FOUND in database for orderId: {}", request.getOrderId());
            throw new RuntimeException("Transaction not found for orderId: " + request.getOrderId());
        }

        try {
            // 2. Verify Razorpay Signature
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getOrderId());
            attributes.put("razorpay_payment_id", request.getPaymentId());
            attributes.put("razorpay_signature", request.getSignature());

            boolean isValid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
            
            if (isValid) {
                log.info("Signature verification SUCCESS for order {}", request.getOrderId());
                updateTransactionAndSubscription(request.getOrderId(), request.getPaymentId(), Transaction.TransactionStatus.SUCCESS);
                return true;
            } else {
                log.error("Signature verification FAILED for order {}. Expected valid signature for payload: {}|{}", 
                    request.getOrderId(), request.getOrderId(), request.getPaymentId());
                updateTransactionAndSubscription(request.getOrderId(), request.getPaymentId(), Transaction.TransactionStatus.FAILED);
                throw new RuntimeException("Signature verification failed. Potential tampering detected.");
            }
        } catch (Exception e) {
            log.error("EXCEPTION during payment verification for order {}: {}", request.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Verification process failed: " + e.getMessage());
        }
    }

    private void updateTransactionAndSubscription(String orderId, String paymentId, Transaction.TransactionStatus status) {
        Transaction transaction = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for orderId: " + orderId));
        
        transaction.setPaymentId(paymentId);
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        if (status == Transaction.TransactionStatus.SUCCESS) {
            UUID userId = transaction.getUserId();
            
            // Create or update subscription
            Subscription subscription = subscriptionRepository.findByUserId(userId)
                    .orElse(Subscription.builder().userId(userId).build());
            
            subscription.setPlanType(Subscription.PlanType.PRO);
            subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
            
            // Determine duration and type based on amount
            // Individual: 1249 (Monthly), 11988 (Yearly)
            // Team: 4999 (Monthly), 49999 (Yearly)
            long monthsToAdd = 1;
            if (transaction.getAmount() >= 49999) {
                monthsToAdd = 12;
                subscription.setBillingCycle(Subscription.BillingCycle.YEARLY);
                subscription.setEntityType(Subscription.EntityType.TEAM);
            } else if (transaction.getAmount() >= 11988) {
                monthsToAdd = 12;
                subscription.setBillingCycle(Subscription.BillingCycle.YEARLY);
                subscription.setEntityType(Subscription.EntityType.INDIVIDUAL);
            } else if (transaction.getAmount() >= 4999) {
                monthsToAdd = 1;
                subscription.setBillingCycle(Subscription.BillingCycle.MONTHLY);
                subscription.setEntityType(Subscription.EntityType.TEAM);
            } else {
                monthsToAdd = 1;
                subscription.setBillingCycle(Subscription.BillingCycle.MONTHLY);
                subscription.setEntityType(Subscription.EntityType.INDIVIDUAL);
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentExpiry = subscription.getExpiryDate();
            
            // If subscription is active, extend from current expiry, else start from now
            if (currentExpiry != null && currentExpiry.isAfter(now)) {
                subscription.setExpiryDate(currentExpiry.plusMonths(monthsToAdd));
                subscription.setStartDate(now); 
            } else {
                subscription.setStartDate(now);
                subscription.setExpiryDate(now.plusMonths(monthsToAdd));
            }
            
            subscriptionRepository.save(subscription);

            // Update auth-service
            authClient.updatePremiumStatus(AuthClient.PremiumUpdateRequest.builder()
                    .userId(userId)
                    .premium(true)
                    .planType("PRO")
                    .subscriptionStart(subscription.getStartDate())
                    .subscriptionExpiry(subscription.getExpiryDate())
                    .build());
            
            log.info("Subscription extended/created for user {} | New Expiry: {}", userId, subscription.getExpiryDate());
        }
    }

    public Subscription.SubscriptionStatus getSubscriptionStatus(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(Subscription::getStatus)
                .orElse(Subscription.SubscriptionStatus.EXPIRED);
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        // Implement webhook logic here
        // Verify signature and update status idempotently
        log.info("Received Razorpay Webhook");
        // For brevity, logic omitted but follows similar pattern to verifyPayment
    }

    public void cancelSubscription(UUID userId) {
        subscriptionRepository.findByUserId(userId).ifPresent(sub -> {
            sub.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(sub);
            
            authClient.updatePremiumStatus(AuthClient.PremiumUpdateRequest.builder()
                    .userId(userId)
                    .premium(false)
                    .planType("FREE")
                    .subscriptionExpiry(null)
                    .build());
        });
    }

    // ── Admin methods ────────────────────────────────────────────────────────
    public java.util.List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public java.util.List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
