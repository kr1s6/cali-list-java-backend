package com.CalisthenicList.CaliList.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAuthResponseDTO {
	private Map<String, String> message;
	private String jwt;
	private UUID userId;
	private String username;
	private String email;
	private String role;
	private boolean emailVerified;
}
