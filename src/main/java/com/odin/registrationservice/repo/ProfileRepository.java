package com.odin.registrationservice.repo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.odin.registrationservice.constants.ApplicationConstants;
import com.odin.registrationservice.dto.ProfileDTO;
import com.odin.registrationservice.dto.ResponseDTO;
import com.odin.registrationservice.entity.Profile;
import com.odin.registrationservice.utility.SearchCriteria;
import com.odin.registrationservice.utility.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProfileRepository {

	@Value("${core.data.url}")
	private String coreUrl;
	
	@Value("${core.update.url}")
	private String coreUpdateUrl;

	@Autowired
	private Utility utility;

	public Profile findByMobileOrEmail(String mobile, String email) {
		List<SearchCriteria> searchCriteriaList = new ArrayList<>();
	    searchCriteriaList.add(new SearchCriteria("mobile", ":", mobile,"OR"));
	    searchCriteriaList.add(new SearchCriteria("email", ":", email,"OR"));
	    
	    // Make the REST call using your utility method
	    ResponseDTO response = utility.makeRestCall(
	    		coreUpdateUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.DETAILS, 
	            searchCriteriaList, 
	            HttpMethod.POST, 
	            ResponseDTO.class
	    );
	    
	    return utility.getAnInstance(response.getData(), Profile.class);
	}
	
	
	public Profile save(Profile profile) {
		ResponseDTO response = utility.makeRestCall(
				coreUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.SAVE, profile, HttpMethod.POST,
				ResponseDTO.class);
		return utility.getAnInstance(response.getData(), Profile.class);
	}

}
