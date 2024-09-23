package com.odin.registrationservice.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.odin.registrationservice.enums.CustomerType;
import com.odin.registrationservice.service.SignUpService;
import com.odin.registrationservice.service.impl.CustomerSignUpServiceImpl;

@Component
public class SignUpFactory {

	@Autowired
	private CustomerSignUpServiceImpl customerSignUp;
	
	public SignUpService getSignUpType(CustomerType type) {
		switch(type) {
		case CUSTOMER:
			return customerSignUp;
		default:
			return null;
		}
	}
}
