package com.CalisthenicList.CaliList.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAuthResponseDTO {
	private Map<String, String> message;
	private String accessToken;
}
