package com.codesync.payment.controller;

import com.codesync.payment.dto.OrderRequest;
import com.codesync.payment.dto.OrderResponse;
import com.codesync.payment.dto.VerifyRequest;
import com.codesync.payment.entity.Subscription;
import com.codesync.payment.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) throws RazorpayException {
        return ResponseEntity.ok(paymentService.createOrder(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(@RequestBody VerifyRequest request) {
        try {
            paymentService.verifyPayment(request);
            return ResponseEntity.ok("Payment verified successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<Subscription.SubscriptionStatus> getStatus(@PathVariable UUID userId) {
        return ResponseEntity.ok(paymentService.getSubscriptionStatus(userId));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription(@RequestParam UUID userId) {
        paymentService.cancelSubscription(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────
    @GetMapping("/admin/subscriptions")
    public ResponseEntity<java.util.List<Subscription>> getAllSubscriptions() {
        return ResponseEntity.ok(paymentService.getAllSubscriptions());
    }

    @GetMapping("/admin/transactions")
    public ResponseEntity<java.util.List<com.codesync.payment.entity.Transaction>> getAllTransactions() {
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }
}
