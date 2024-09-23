package com.odin.registrationservice.controller;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.odin.registrationservice.constants.ApplicationConstants;
import com.odin.registrationservice.utility.EncryptionDecryption;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = ApplicationConstants.API_VERSION)
public class Test {
	
	@Value("${default-iv-value}")
    private String iv;
	
	@Value("${encryption-key}")
    private String encryptionKey;
	
	@PostMapping("/test")
	public String apiMetadataController(HttpServletRequest request, @RequestBody Object obj){
		 EncryptionDecryption encryptionDecryption = null;
		try {
			encryptionDecryption = new EncryptionDecryption(iv, encryptionKey);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
         String decryptedRequestBody = null;
		try {
			decryptedRequestBody = encryptionDecryption.encrypt("{\"name\":\"gaurav\"}");
		} catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
				| InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
			e.printStackTrace();
		}
         return decryptedRequestBody;
	}

}
