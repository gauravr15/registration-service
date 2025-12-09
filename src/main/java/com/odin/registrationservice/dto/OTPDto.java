package com.odin.registrationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OTPDto {
	
	private String route;
	
	private String message;
	
	private String numbers;
	
	private int flash;
}