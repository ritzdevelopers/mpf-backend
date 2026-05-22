package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.OTP;
import com.mypropertyfact.estate.entities.OtpPurpose;
import com.mypropertyfact.estate.repositories.OTPRepository;
import com.mypropertyfact.estate.validation.ConsumerEmailNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class OTPService {

    private final OTPRepository otpRepository;

    public String generateOTP(String email) {
        return generateOTP(email, OtpPurpose.MAGIC_LINK);
    }

    public String generateOTP(String email, OtpPurpose purpose) {
        String normalizedEmail = ConsumerEmailNormalizer.normalize(email);
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        OTP otp = new OTP();
        otp.setEmail(normalizedEmail);
        otp.setOtpCode(otpCode);
        otp.setIsVerified(false);
        otp.setPurpose(purpose != null ? purpose : OtpPurpose.MAGIC_LINK);
        otpRepository.save(otp);
        log.info("OTP generated for email: {} purpose: {}", normalizedEmail, otp.getPurpose());
        return otpCode;
    }

    public boolean verifyOTP(String email, String otpCode) {
        return verifyOTP(email, otpCode, OtpPurpose.MAGIC_LINK);
    }

    public boolean verifyOTP(String email, String otpCode, OtpPurpose purpose) {
        String normalizedEmail = ConsumerEmailNormalizer.normalize(email);
        OtpPurpose p = purpose != null ? purpose : OtpPurpose.MAGIC_LINK;

        List<OTP> matches = otpRepository.findMatchingUnverified(
                normalizedEmail, otpCode, p, PageRequest.of(0, 1));

        if (matches.isEmpty()) {
            return false;
        }

        OTP otpEntity = matches.get(0);

        if (otpEntity.getExpiresAt().before(new Date())) {
            log.warn("OTP expired for email: {}", email);
            return false;
        }

        otpEntity.setIsVerified(true);
        otpRepository.save(otpEntity);

        log.info("OTP verified for email: {} purpose: {}", email, p);
        return true;
    }

    public boolean isValidOTP(String email, String otpCode) {
        return isValidOTP(email, otpCode, OtpPurpose.MAGIC_LINK);
    }

    public boolean isValidOTP(String email, String otpCode, OtpPurpose purpose) {
        String normalizedEmail = ConsumerEmailNormalizer.normalize(email);
        OtpPurpose p = purpose != null ? purpose : OtpPurpose.MAGIC_LINK;

        List<OTP> matches = otpRepository.findMatchingUnverified(
                normalizedEmail, otpCode, p, PageRequest.of(0, 1));

        Optional<OTP> otp = matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));

        return otp.map(value -> value.getExpiresAt().after(new Date())).orElse(false);
    }
}
