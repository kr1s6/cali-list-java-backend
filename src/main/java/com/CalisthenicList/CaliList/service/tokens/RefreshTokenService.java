package com.CalisthenicList.CaliList.service.tokens;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.ApiResponse;
import com.CalisthenicList.CaliList.model.RefreshToken;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserDTO;
import com.CalisthenicList.CaliList.repositories.RefreshTokenRepository;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.UserService;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
//INFO - used to get new access tokens when the old ones expire
public class RefreshTokenService {
	@Value("${refreshToken.expiration.days}")
	private int refreshTokenDuration;
	private final AccessTokenService accessTokenService;
	private final JwtUtils jwtUtils;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private Duration tokenDuration;

	@PostConstruct
	public void init() {
		tokenDuration = Duration.ofDays(refreshTokenDuration);
	}


	public ResponseCookie createCookieWithRefreshToken(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException(Messages.USER_NOT_FOUND));
		return createCookieWithRefreshToken(email, user);
	}

	public ResponseCookie createCookieWithRefreshToken(String email, User user) {
		RefreshToken refreshToken = createRefreshToken(email, user);
		return ResponseCookie.from("refreshToken", refreshToken.getToken())
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(tokenDuration)
				.build();
	}

	public RefreshToken createRefreshToken(String email, User user) {
		String jwt = jwtUtils.generateJwt(email, tokenDuration);
		//Update or create a refresh token
		RefreshToken token = refreshTokenRepository.findByUserEmail(email)
				.map(exists -> {
					exists.setToken(jwt);
					exists.setExpiryDate(Instant.now().plus(tokenDuration));
					return exists;
				}).orElseGet(() -> {
					RefreshToken newToken = new RefreshToken();
					newToken.setUser(user);
					newToken.setExpiryDate(Instant.now().plus(tokenDuration));
					newToken.setToken(jwt);
					return newToken;
				});
		return refreshTokenRepository.save(token);
	}

	public ResponseEntity<ApiResponse<Object>> refreshAccessToken(String refToken, HttpServletResponse response) {
		if(refToken == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.builder()
							.success(false)
							.message("Unauthorized - no refresh token")
							.build());
		}
		//Check if the refresh token exists
		RefreshToken refreshToken = refreshTokenRepository.findByToken(refToken)
				.orElseThrow(() -> new UsernameNotFoundException(Messages.UNAUTHORIZED));

		//Validate refresh token
		String jwtEmail = jwtUtils.extractSubject(refToken);
		String userEmail = refreshToken.getUser().getEmail();
		if(jwtEmail == null || !jwtUtils.validateIfJwtSubjectMatchTheUser(jwtEmail, userEmail)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.builder()
							.success(false)
							.message("Unauthorized - invalid token")
							.build());
		}
		if(isRefreshTokenExpired(refreshToken)) {
			refreshTokenRepository.delete(refreshToken);
			return ResponseEntity.badRequest().body(ApiResponse.builder()
							.success(false)
							.message("Refresh token expired. Please login again.")
							.build());
		}

		//Create a new refresh token
		ResponseCookie cookieWithRefreshToken = createCookieWithRefreshToken(jwtEmail);
		response.addHeader(HttpHeaders.SET_COOKIE, cookieWithRefreshToken.toString());

		// Create a new access token
		String accessToken = accessTokenService.generateAccessToken(jwtEmail);

		//Validate if user exists
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> new UsernameNotFoundException(Messages.UNAUTHORIZED));
		//Update user
		user.setTrainingDuration(UserService.calculateTrainingDuration(user.getCaliStartDate()));

		//Return userDTO with an access token
		UserDTO userDTO = new UserDTO(user);
		return ResponseEntity.ok(ApiResponse.builder()
						.success(true)
						.message(Messages.REFRESH_TOKEN_SUCCESS)
						.data(userDTO)
						.accessToken(accessToken)
						.build());
	}

	public ResponseEntity<ApiResponse<Object>> deleteRefreshToken(String refToken, HttpServletResponse response) {
		if(refToken == null) {
			return ResponseEntity.badRequest().body(ApiResponse.builder()
							.success(false)
							.message("No refresh token found in cookies.")
							.build());
		}
		//Check if the refresh token exists
		RefreshToken refreshToken = refreshTokenRepository.findByToken(refToken)
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
		return ResponseEntity.ok(ApiResponse.builder()
						.success(true)
						.message("Logged out successfully.")
						.build());
	}

	public boolean isRefreshTokenExpired(RefreshToken token) {
		return token.getExpiryDate().isBefore(Instant.now());
	}
}