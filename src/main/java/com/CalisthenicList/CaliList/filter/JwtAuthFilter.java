package com.CalisthenicList.CaliList.filter;


import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
//INFO - Intercepts incoming requests, validates JWT tokens, and authenticates users if a valid token is present
public class JwtAuthFilter extends OncePerRequestFilter {
	private final JwtUtils jwtUtils;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
									@NonNull FilterChain filterChain) throws ServletException, IOException {
		try {
			String jwtToken = parseJwt(request);
			if(jwtToken != null && jwtUtils.validateJwtToken(jwtToken)) {
				String email = jwtUtils.extractEmailFromToken(jwtToken);
				Optional<User> userOptional = userRepository.findByEmail(email);
				if(userOptional.isPresent()) {
					User user = userOptional.get();
					UserDetails userDetails = new org.springframework.security.core.userdetails.User(
							user.getEmail(),
							user.getPassword(),
							List.of(new SimpleGrantedAuthority(user.getRole().name()))
					);
					UsernamePasswordAuthenticationToken authToken =
							new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch(Exception e) {
			System.out.println("Cannot set user authentication: " + e);
		}
		filterChain.doFilter(request, response);
	}

	private String parseJwt(HttpServletRequest request) {
		String authorizationHeader = request.getHeader("Authorization");
		if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			return authorizationHeader.substring(7);
		}
		return null;
	}

}