package com.CalisthenicList.CaliList.configurations;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
		try {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, Messages.UNAUTHORIZED);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
