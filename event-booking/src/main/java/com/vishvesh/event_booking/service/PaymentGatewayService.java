package com.vishvesh.event_booking.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

    private final RazorpayClient razorpayClient;

    @Value("${RAZORPAY_TEST_API_SECRET}")
    private String keySecret;

    public String createOrder(BigDecimal amount, String receiptId) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receiptId);

            Order order = razorpayClient.orders.create(orderRequest);
            log.info("Razorpay order created: {} for receipt: {}", order.get("id"), receiptId);
            return order.get("id");
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for receipt {}: {}", receiptId, e.getMessage());
            throw new IllegalStateException("Payment gateway is currently unavailable. Please try again.");
        }
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hashBytes = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            boolean isValid = hexString.toString().equals(signature);
            if (!isValid) {
                log.warn("Signature mismatch for orderId: {}. Possible tampering attempt.", orderId);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Signature verification failed for orderId: {}", orderId, e);
            return false;
        }
    }
}
