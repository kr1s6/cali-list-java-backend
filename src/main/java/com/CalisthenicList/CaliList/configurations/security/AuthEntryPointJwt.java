package com.CalisthenicList.CaliList.configurations.security;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * A class to handle authorized access attempts in a Spring Security application using JWT authentication.
 * It acts as a gatekeeper, ensuring only users with valid access can access protected resources.
 */
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
