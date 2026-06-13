package com.odin.registrationservice.service.impl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.odin.registrationservice.constants.ApplicationConstants;
import com.odin.registrationservice.constants.LanguageConstants;
import com.odin.registrationservice.constants.ResponseCodes;
import com.odin.registrationservice.dto.JwtDTO;
import com.odin.registrationservice.dto.NotificationDTO;
import com.odin.registrationservice.dto.ProfileDTO;
import com.odin.registrationservice.dto.ResponseDTO;
import com.odin.registrationservice.entity.Auth;
import com.odin.registrationservice.entity.Profile;
import com.odin.registrationservice.entity.RefreshToken;
import com.odin.registrationservice.enums.NotificationChannel;
import com.odin.registrationservice.enums.OTPType;
import com.odin.registrationservice.repo.ProfileRepository;
import com.odin.registrationservice.repo.RefreshTokenRepository;
import com.odin.registrationservice.service.SignUpService;
import com.odin.registrationservice.utility.JwtTokenUtil;
import com.odin.registrationservice.utility.NotificationUtility;
import com.odin.registrationservice.utility.OtpService;
import com.odin.registrationservice.utility.ResponseObject;
import com.odin.registrationservice.utility.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomerSignUpServiceImpl implements SignUpService {

	@Value("${is.regex.enabled}")
	private boolean isRegexEnabled;

	@Value("${is.static.otp}")
	private boolean isStaticOtp;

	@Value("${static.otp}")
	private String staticOtp;
	
	@Value("${otp.expiry.duration.seconds}")
	private int otpExpiryDuration;
	
	@Value("${jwt.refreshExpiration}")
	private long refreshTokenExpiryMs;
	
	@Autowired
	private RefreshTokenRepository refreshTokenRepo;

	@Autowired
	private ResponseObject responseObj;

	@Autowired
	private Utility utility;
	
	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private ProfileRepository profileRepo;

	@Autowired
	private OtpService otpService;

	@Autowired
	private NotificationUtility notification;

	@Override
	public ResponseDTO signUp(HttpServletRequest req, ProfileDTO signUpDTO) {
		log.info("Inside customer signup class");
		String flowAuthType = utility.getAuthType(ApplicationConstants.AUTH_FLOW_SIGNUP);
		log.info("Signup flow auth type from Redis: {}", flowAuthType);

		if (ApplicationConstants.OTP.equalsIgnoreCase(flowAuthType)) {
			return signUpViaOtp(req, signUpDTO);
		}
		String language = req.getHeader(ApplicationConstants.APP_LANG);
		if (isRegexEnabled) {
			log.info("Validating password with regex");
			boolean isValid = utility.validatePasswordRegex(signUpDTO.getAuth().getPassword());
			if (!isValid) {
				log.error("Regex validation failed");
				return responseObj.buildResponse(language, ResponseCodes.INVALID_PASSWORD_FORMAT);
			}
		}
		log.info("encrypting password");
		BCryptPasswordEncoder encrypt = new BCryptPasswordEncoder();
		String hashedPassword = encrypt.encode(signUpDTO.getAuth().getPassword());
		signUpDTO.getAuth().setPassword(hashedPassword);
		Profile checkProfile = profileRepo.findByMobileOrEmail(signUpDTO.getMobile(), signUpDTO.getIdNum());

		if (!ObjectUtils.isEmpty(checkProfile) && !Boolean.TRUE.equals(checkProfile.getIsDeleted())) {
			log.error("Customer already exists with customer id: {}", checkProfile.getCustomerId());
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.USER_EXISTS);
		}

		if (!ObjectUtils.isEmpty(checkProfile) && Boolean.TRUE.equals(checkProfile.getIsDeleted())) {
			log.info("Re-registration: soft-deleted account found for mobile={}, allowing re-signup", signUpDTO.getMobile());
		}

		if (ObjectUtils.isEmpty(checkProfile) || Boolean.TRUE.equals(checkProfile.getIsDeleted())) {
			log.info("Creating new customer");
			Auth newAuth = Auth.builder().isActive(true).isDeleted(false).isFirstTimeLogin(true).isPermLock(false)
					.isTempLock(false).isTempPassword(true).incorrectPasswordCount(0).passwordChangeCount(0)
					.permLockCount(0).tempLockCount(0).password(hashedPassword).isOtpLogin(false)
					.publicKey(signUpDTO.getAuth().getPublicKey())
					.keyVersion(!ObjectUtils.isEmpty(signUpDTO.getAuth().getPublicKey()) ? "1" : null)
					.build();
			Profile newProfile = Profile.builder().auth(newAuth).address(signUpDTO.getAddress())
					.customerType(signUpDTO.getCustomerType()).email(signUpDTO.getEmail()).isActive(true)
					.isDeleted(false).isNotificationEnabled(true).isTransactionEnabled(false)
					.firstName(signUpDTO.getFirstName()).lastName(signUpDTO.getLastName()).mobile(signUpDTO.getMobile())
					.build();
			newProfile = profileRepo.save(newProfile);
			newProfile.setAuth(null);
			return responseObj.buildResponse(ResponseCodes.USER_CREATED, newProfile);
		}
		// Should never reach here — active profile check already returned USER_EXISTS above
		return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.USER_EXISTS);
	}

	@Override
	public ResponseDTO signUpViaOtp(HttpServletRequest req, ProfileDTO signUpDTO) {

		Profile checkProfile = profileRepo.findByMobileOrEmail(signUpDTO.getMobile(), signUpDTO.getIdNum());

		if (!ObjectUtils.isEmpty(checkProfile) && !Boolean.TRUE.equals(checkProfile.getIsDeleted())) {
			log.error("Customer already exists with customer id: {}", checkProfile.getCustomerId());
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.USER_EXISTS);
		}

		if (!ObjectUtils.isEmpty(checkProfile) && Boolean.TRUE.equals(checkProfile.getIsDeleted())) {
			log.info("Re-registration: soft-deleted account found for mobile={}, allowing re-signup via OTP", signUpDTO.getMobile());
		}

		if (ObjectUtils.isEmpty(checkProfile) || Boolean.TRUE.equals(checkProfile.getIsDeleted())) {
			if (signUpDTO.getMobile().isEmpty() && signUpDTO.getEmail().isEmpty()) {
				return responseObj.buildResponse(ResponseCodes.INVALID_REQUEST);
			}
			String mobileOtp = (signUpDTO.getMobile() != null)
					? otpService.getOtp(signUpDTO.getMobile(), OTPType.REGISTRATION)
					: null;

			if (mobileOtp != null && !mobileOtp.isEmpty()) {
				otpService.clearOtp(signUpDTO.getMobile(), OTPType.REGISTRATION);
			}

			String emailOtp = (signUpDTO.getEmail() != null)
					? otpService.getOtp(signUpDTO.getEmail(), OTPType.REGISTRATION)
					: null;

			if (emailOtp != null && !emailOtp.isEmpty()) {
				otpService.clearOtp(signUpDTO.getEmail(), OTPType.REGISTRATION);
			}
			if (signUpDTO.getMobile() != null && !signUpDTO.getMobile().isEmpty()) {
				String otp = isStaticOtp || ! signUpDTO.getMobile().startsWith("+91") ? staticOtp : String.valueOf((int) (Math.random() * 900000) + 100000);

				otpService.saveOtp(signUpDTO.getMobile(), otp, OTPType.REGISTRATION, otpExpiryDuration);
				Map<String, String> map = new HashMap<>();
				map.put("otp", otp);
				NotificationDTO notify = NotificationDTO.builder().mobile(signUpDTO.getMobile()).notificationId(2020l)
						.channel(NotificationChannel.SMS).map(map).build();
				notification.sendOtpMessage(notify);
			}

			if (signUpDTO.getEmail() != null && !signUpDTO.getEmail().isEmpty()) {
				String otp = isStaticOtp ? staticOtp : String.valueOf((int) (Math.random() * 900000) + 100000);

				otpService.saveOtp(signUpDTO.getEmail(), otp, OTPType.REGISTRATION, otpExpiryDuration);
				Map<String, String> map = new HashMap<>();
				map.put("otp", otp);
				NotificationDTO notify = NotificationDTO.builder().email(signUpDTO.getEmail()).notificationId(2020l)
						.channel(NotificationChannel.EMAIL).map(map).build();
				notification.sendOtpMessage(notify);
			}
			log.info("Creating new customer");
			Auth newAuth = Auth.builder().isActive(false).isDeleted(false).isFirstTimeLogin(true).isPermLock(false)
					.isTempLock(false).isTempPassword(true).incorrectPasswordCount(0).passwordChangeCount(0)
					.permLockCount(0).tempLockCount(0).isOtpLogin(true)
					.publicKey(signUpDTO.getAuth().getPublicKey())
					.keyVersion(!ObjectUtils.isEmpty(signUpDTO.getAuth().getPublicKey()) ? "1" : null)
					.build();
			Profile newProfile = Profile.builder().auth(newAuth).address(signUpDTO.getAddress())
					.customerType(signUpDTO.getCustomerType()).email(signUpDTO.getEmail()).isActive(false)
					.isDeleted(false).isNotificationEnabled(true).isTransactionEnabled(false)
					.firstName(signUpDTO.getFirstName()).lastName(signUpDTO.getLastName()).mobile(signUpDTO.getMobile())
					.build();
			newProfile = profileRepo.save(newProfile);
			newProfile.setAuth(null);
			return responseObj.buildResponse(ResponseCodes.OTP_SENT_SUCCESSFUL);
		}
		// Should never reach here — active profile check already returned USER_EXISTS above
		return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.USER_EXISTS);
	}

	@Override
	public ResponseDTO completeSignUp(HttpServletRequest req, ProfileDTO signUpDTO) {
		log.info("Inside customer signup class");
		String deviceSignature = utility.getDeviceSignature(req);
		String flowAuthType = utility.getAuthType(ApplicationConstants.AUTH_FLOW_SIGNUP);
		log.info("Signup flow auth type from Redis: {}", flowAuthType);
		Profile checkProfile = profileRepo.findByMobileOrEmailAndIsActive(signUpDTO.getMobile(), signUpDTO.getEmail(), false);

		if (!ApplicationConstants.OTP.equalsIgnoreCase(flowAuthType) || !checkProfile.getAuth().isOtpLogin()) {
			return responseObj.buildResponse(ResponseCodes.INVALID_REQUEST);
		}
		String sentOtp = otpService.getOtp(signUpDTO.getMobile(), OTPType.REGISTRATION);

		if (checkProfile.getAuth().getIsFirstTimeLogin() && null == sentOtp) {
			return responseObj.buildResponse(ResponseCodes.OTP_EXPIRED);
		}
		else if(!sentOtp.equals(signUpDTO.getAuth().getPassword())){
			return responseObj.buildResponse(ResponseCodes.OTP_INVALID);
		}
		signUpDTO.getAuth().setPassword(ApplicationConstants.OTP_BASED_AUTH);

		if (!ObjectUtils.isEmpty(checkProfile) && !checkProfile.getAuth().getIsFirstTimeLogin()) {
			log.error("Customer already exists with customer id: {}", checkProfile.getCustomerId());
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.USER_EXISTS);
		} else {
			checkProfile.setIsActive(true);
			checkProfile.getAuth().setIsActive(true);
			profileRepo.save(checkProfile);
			String accessToken = jwtTokenUtil.generateAccessToken(String.valueOf(checkProfile.getCustomerId()),
					deviceSignature);
			String refreshToken = jwtTokenUtil.generateRefreshToken();

			// Persist refresh token (per device)
			RefreshToken rt = new RefreshToken();
			rt.setCustomerId(Long.valueOf(checkProfile.getCustomerId()));
			rt.setRefreshToken(refreshToken);
			Timestamp now = new Timestamp(System.currentTimeMillis());
			rt.setCreatedAt(now);
			rt.setExpiryDate(new Timestamp(now.getTime() + refreshTokenExpiryMs));
			rt.setIsActive(true);
			rt.setDeviceSignature(deviceSignature);
			refreshTokenRepo.save(rt);

			JwtDTO jwtResponse = JwtDTO.builder().accessToken(accessToken).refreshToken(refreshToken)
					.deviceSignature(deviceSignature).build();
			checkProfile.setAuth(null);
			return responseObj.buildResponse(ResponseCodes.USER_CREATED, jwtResponse);
		}
	}

}
