package com.odin.registrationservice.service.impl;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.odin.registrationservice.constants.ApplicationConstants;
import com.odin.registrationservice.constants.ResponseCodes;
import com.odin.registrationservice.dto.ProfileDTO;
import com.odin.registrationservice.dto.ResponseDTO;
import com.odin.registrationservice.repo.ProfileRepository;
import com.odin.registrationservice.service.SignUpService;
import com.odin.registrationservice.utility.ResponseObject;
import com.odin.registrationservice.utility.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomerSignUpServiceImpl implements SignUpService {

	@Value("${is.regex.enabled}")
	boolean isRegexEnabled;

	@Autowired
	private ResponseObject responseObj;
	
	@Autowired
	private Utility utility;
	
	@Autowired
	private ProfileRepository profileRepo;

	@Override
	public ResponseDTO signUp(HttpServletRequest req, ProfileDTO signUpDTO) {
		log.info("Inside customer signup class");
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
		ResponseDTO profile = profileRepo.save(signUpDTO);
		
		log.info("Customer signup successful for mobile : {}", signUpDTO.getMobile());
		return profile;
	}

}
