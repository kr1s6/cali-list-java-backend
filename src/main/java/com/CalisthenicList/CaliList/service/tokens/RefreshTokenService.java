package com.CalisthenicList.CaliList.service.tokens;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.RefreshToken;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.RefreshTokenRepository;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
//INFO - used to get new access tokens when the old ones expire
public class RefreshTokenService {
	@Value("${refreshToken.expiration.days}")
	private int refreshTokenDuration;
	private final AccessTokenService accessTokenService;
	private final JwtUtils jwtUtils;
	private final UserDetailsService userDetailsService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private Duration tokenDuration;

	@PostConstruct
	public void init() {
		tokenDuration = Duration.ofDays(refreshTokenDuration);
	}

	public ResponseCookie createCookieWithRefreshToken(String email) {
		RefreshToken refreshToken = createRefreshToken(email);
		return ResponseCookie.from("refreshToken", refreshToken.getToken())
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(tokenDuration)
				.build();
	}

	public RefreshToken createRefreshToken(String email) {
		String jwt = jwtUtils.generateJwt(email, tokenDuration);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException(Messages.USER_NOT_FOUND));
		RefreshToken token = new RefreshToken();
		token.setUser(user);
		token.setExpiryDate(Instant.now().plus(tokenDuration));
		token.setToken(jwt);
		return refreshTokenRepository.save(token);
	}

	public ResponseEntity<?> getNewAccessToken(HttpServletRequest request) {
		String token = Arrays.stream(request.getCookies())
				.filter(cookie -> "refreshToken".equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElse(null);

		if(token == null) {
			return new ResponseEntity<>("Unauthorized - no refresh token", HttpStatus.UNAUTHORIZED);
		}

		//Check if the refresh token exists
		RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
				.orElseThrow(() -> new UsernameNotFoundException(Messages.SERVICE_ERROR));

		//Validate refresh token
		String email = jwtUtils.extractEmail(token);
		UserDetails userDetails = userDetailsService.loadUserByUsername(email);
		if(email == null || !jwtUtils.validateJwt(token, userDetails)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized - invalid token");
		}

		if(isRefreshTokenExpired(refreshToken)) {
			refreshTokenRepository.delete(refreshToken);
			return ResponseEntity.badRequest().body("Refresh token expired. Please login again.");
		}

		// Create a new access token
		String accessToken = accessTokenService.generateAccessToken(email);
		return ResponseEntity.ok(Map.of("accessToken", accessToken));
	}

	//TODO generate new refresh token every refresh or just update refresh token

	public ResponseEntity<?> deleteRefreshToken(String token, HttpServletResponse response) {
		if(token == null) {
			return ResponseEntity.badRequest().body("No refresh token found in cookies.");
		}
		//Check if the refresh token exists
		RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
				.orElseThrow(() -> new UsernameNotFoundException(Messages.SERVICE_ERROR));
		refreshTokenRepository.delete(refreshToken);

		// Delete refresh token
		ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(0)
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
		return ResponseEntity.ok("Logged out successfully.");
	}

	public boolean isRefreshTokenExpired(RefreshToken token) {
		return token.getExpiryDate().isBefore(Instant.now());
	}
}