package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.ApiResponse;
import com.CalisthenicList.CaliList.model.DTO.JwtResponseDTO;
import com.CalisthenicList.CaliList.service.authorization.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;

@Profile("dev")
@Controller
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class JwtController {
	private final JwtService jwtService;

	@Value("${accessToken.expiration.minutes}")
	private int accessTokenDuration;

	@GetMapping("/buildJwt")
	public ResponseEntity<ApiResponse<JwtResponseDTO>> buildJwt() {
		Duration duration = Duration.ofMinutes(accessTokenDuration);
		String jwt = jwtService.buildJwt("email@interia.pl", duration);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.<JwtResponseDTO>builder()
						.success(true)
						.message("JWT created.")
						.data(new JwtResponseDTO(jwt))
						.build());
	}

	@GetMapping("/parseJwt")
	public ResponseEntity<ApiResponse<JwtResponseDTO>> parseJwt(@RequestParam String jwt) {
		Jws<Claims> jws = jwtService.parseJwt(jwt);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.<JwtResponseDTO>builder()
						.success(true)
						.message("JWT parsed.")
						.data(new JwtResponseDTO(jws))
						.build());
	}
}
