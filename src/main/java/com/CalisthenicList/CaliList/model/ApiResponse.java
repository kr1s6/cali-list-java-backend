package com.CalisthenicList.CaliList.model;

import lombok.Builder;

@Builder
public record ApiResponse<T>(Boolean success, String message, T data, String accessToken) {}
