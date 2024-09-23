package com.odin.registrationservice.service;

import javax.servlet.http.HttpServletRequest;

import com.odin.registrationservice.dto.ProfileDTO;
import com.odin.registrationservice.dto.ResponseDTO;

public interface SignUpService {

	public ResponseDTO signUp(HttpServletRequest req, ProfileDTO signUpDTO);
}
