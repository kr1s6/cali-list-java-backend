package com.CalisthenicList.CaliList.service.tokens;

import com.CalisthenicList.CaliList.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
//INFO - used to access resources
public class AccessTokenService {
	@Value("${accessToken.expiration.minutes}")
	private int accessTokenDuration;
	private final JwtUtils jwtUtils;

	public String generateAccessToken(String email) {
		Duration duration = Duration.ofMinutes(accessTokenDuration);
		return jwtUtils.generateJwt(email, duration);
	}
}
