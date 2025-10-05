package com.CalisthenicList.CaliList.filter;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessTokenAuthFilterTest {
	@Mock
	private JwtUtils jwtUtils;
	@Mock
	private UserDetailsService userDetailsService;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private FilterChain filterChain;
	@InjectMocks
	private AccessTokenAuthFilter filter;
	private final String jwt = "fake-jwt-token";
	private final String email = "test@example.com";

	@BeforeEach
	void setup() {
		SecurityContextHolder.clearContext();
		filter = new AccessTokenAuthFilter(jwtUtils, userDetailsService);
	}

	@Test
	@DisplayName("✅ Happy Case: No Authorization header → request passes through without authentication")
	void noAuthorizationHeader_shouldPassThrough() throws ServletException, IOException {
		//Given
		when(request.getHeader("Authorization")).thenReturn(null);
		//When
		filter.doFilterInternal(request, response, filterChain);
		//Given
		verify(filterChain, times(1)).doFilter(request, response);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	@DisplayName("✅ Happy Case: Valid JWT → sets authentication in SecurityContext")
	void validJwt_setsAuthentication() throws ServletException, IOException {
		// Given
		when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
		when(jwtUtils.extractSubject(anyString())).thenReturn(email);
		UserDetails userDetails = new User(email, "password", Collections.emptyList());
		when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
		when(jwtUtils.validateIfJwtSubjectMatchTheUser(email, userDetails)).thenReturn(true);
		// When
		filter.doFilterInternal(request, response, filterChain);
		// Then
		verify(filterChain, times(1)).doFilter(request, response);
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(authentication);
		assertInstanceOf(UsernamePasswordAuthenticationToken.class, authentication);
		assertEquals(email, ((UserDetails) authentication.getPrincipal()).getUsername());
	}

	@Test
	@DisplayName("❌ Negative Case: Invalid JWT (validateIfJwtSubjectMatchTheUser = false) → no authentication")
	void invalidJwt_doesNotAuthenticate() throws ServletException, IOException {
		//Given
		when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
		when(jwtUtils.extractSubject(jwt)).thenReturn(email);
		UserDetails userDetails = new User(email, "password", Collections.emptyList());
		when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
		when(jwtUtils.validateIfJwtSubjectMatchTheUser(email, userDetails)).thenReturn(false);
		//When
		filter.doFilterInternal(request, response, filterChain);
		//Then
		verify(filterChain, times(1)).doFilter(request, response);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	@DisplayName("❌ Negative Case: JwtUtils throws exception → no authentication, request still passes through")
	void exceptionInJwtUtils_doesNotBreakFilterChain() throws ServletException, IOException {
		//Given
		when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
		when(jwtUtils.extractSubject(jwt)).thenThrow(new RuntimeException("invalid token"));
		//When
		filter.doFilterInternal(request, response, filterChain);
		//Then
		verify(filterChain, times(1)).doFilter(request, response);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
