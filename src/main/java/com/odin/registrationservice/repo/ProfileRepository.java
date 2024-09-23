package com.odin.registrationservice.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.odin.registrationservice.dto.ProfileDTO;
import com.odin.registrationservice.dto.ResponseDTO;
import com.odin.registrationservice.utility.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProfileRepository {

	@Value("${core.data.url}")
	private String coreUrl;

	@Autowired
	private Utility utility;

	public ResponseDTO save(ProfileDTO profile) {
		log.info("Saving customer profile");
		return utility.makeRestCall(coreUrl + CoreAPIConstants.CREATE + CoreAPIConstants.CUSTOMER,
				profile, HttpMethod.POST, ResponseDTO.class);
	}

}
