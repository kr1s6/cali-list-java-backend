package com.CalisthenicList.CaliList.filter;


import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.JwtService;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
//INFO - Intercepts incoming requests, validates JWT tokens, and authenticates users if a valid token is present
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
									@NonNull FilterChain filterChain) throws ServletException, IOException {
		String authorizationHeader = request.getHeader("Authorization");
		if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String jwtToken = authorizationHeader.substring(7);
		String email = jwtService.extractEmail(jwtToken);

		if(email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			Optional<User> userOptional = userRepository.findByEmail(email);
			if(userOptional.isPresent() && jwtService.validateJwtToken(jwtToken)) {
				User user = userOptional.get();
				Collection<GrantedAuthority> authorities = List.of(
						new SimpleGrantedAuthority(user.getRole().name()) // np. ROLE_USER
				);
				UsernamePasswordAuthenticationToken authToken =
						new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		}
		filterChain.doFilter(request, response);
	}


}