package com.odin.registrationservice.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.odin.registrationservice.constants.ApplicationConstants;
import com.odin.registrationservice.dto.ProfileDTO;
import com.odin.registrationservice.dto.ResponseDTO;
import com.odin.registrationservice.factory.SignUpFactory;
import com.odin.registrationservice.service.SignUpService;

@RestController
@RequestMapping(ApplicationConstants.API_VERSION)
public class SignUpController {

	@Autowired
	private SignUpFactory signUpFactory;
	
	@PostMapping(ApplicationConstants.SIGN_UP)
	public ResponseEntity<Object> signUp(HttpServletRequest req,@Valid @RequestBody ProfileDTO signUp){
		SignUpService signUpObject = signUpFactory.getSignUpType(signUp.getCustomerType());
		ResponseDTO response = signUpObject.signUp(req, signUp);
		return new ResponseEntity<>(response,HttpStatus.OK);
		
	}
}
