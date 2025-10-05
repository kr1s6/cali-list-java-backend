package com.CalisthenicList.CaliList.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class ApiResponse<T> {
	private boolean success;
	private String message;
	private T data;
	private String accessToken;
}
