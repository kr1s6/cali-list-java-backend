package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.*;
import com.CalisthenicList.CaliList.repositories.RefreshTokenRepository;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.tokens.AccessTokenService;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class UserService {
	private final Logger logger = Logger.getLogger(UserService.class.getName());
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtUtils jwtUtils;
	private final AccessTokenService accessTokenService;

	public ResponseEntity<ApiResponse<Object>> deleteUserById(UserDeleteByIdDTO userDeleteByIdDto) {
		UUID id = userDeleteByIdDto.getUserId();
		//Validate if user exists
		User user = userRepository.findById(id)
				.orElseThrow(() -> {
					logger.warning("Attempt to delete NON-existing user.");
					return new UsernameNotFoundException(Messages.SERVICE_ERROR);
				});

		//Validate if the password is valid for user id
		String password = userDeleteByIdDto.getPassword();
		if(!encoder.matches(password, user.getPassword())) {
			logger.warning("Invalid password for user deletion attempt.");
			throw new BadCredentialsException(Messages.SERVICE_ERROR);
		}
		//Delete user and refresh token
		refreshTokenRepository.findByUserEmail(user.getEmail()).ifPresent(refreshTokenRepository::delete);
		userRepository.delete(user);
		logger.info(Messages.USER_DELETED);
		return ResponseEntity.ok(
				ApiResponse.builder()
						.success(true)
						.message(Messages.USER_DELETED)
						.build()
		);
	}

	public ResponseEntity<ApiResponse<Object>> setUserBirthdate(@Valid BirthdateDTO dto, String refreshToken) {
		String userEmail = jwtUtils.extractSubject(refreshToken);
		//Validate if user exists
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> {
					logger.warning(Messages.USER_NOT_FOUND);
					return new UsernameNotFoundException(Messages.USER_NOT_FOUND);
				});

		user.setBirthdate(dto.getBirthdate());
		userRepository.save(user);

		//Create an access token
		String accessToken = accessTokenService.generateAccessToken(userEmail);

		//Return apiResponse
		UserDTO userDTO = new UserDTO(user);
		return ResponseEntity.ok(
				ApiResponse.builder()
						.success(true)
						.message("Birthday set.")
						.data(userDTO)
						.accessToken(accessToken)
						.build()
		);
	}

	public ResponseEntity<ApiResponse<Object>> setUserCaliStartDate(@Valid CaliStartDateDTO dto, String refreshToken) {
		String userEmail = jwtUtils.extractSubject(refreshToken);
		//Validate if user exists
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> {
					logger.warning(Messages.USER_NOT_FOUND);
					return new UsernameNotFoundException(Messages.USER_NOT_FOUND);
				});

		//Set caliStartDate
		LocalDate start = dto.getCaliStartDate();
		user.setCaliStartDate(start);

		//Set trainingDuration
		user.setTrainingDuration(calculateTrainingDuration(start));
		userRepository.save(user);

		//Create an access token
		String accessToken = accessTokenService.generateAccessToken(userEmail);

		//Return apiResponse
		UserDTO userDTO = new UserDTO(user);
		return ResponseEntity.ok(
				ApiResponse.builder()
						.success(true)
						.message("Cali start date set.")
						.data(userDTO)
						.accessToken(accessToken)
						.build()
		);
	}

	public static String calculateTrainingDuration(LocalDate caliStartDate) {
		if(caliStartDate == null) {
			return "0 Days";
		}
		LocalDate today = LocalDate.now();
		Period trainingDuration = Period.between(caliStartDate, today);
		String years  = formatPart(trainingDuration.getYears(), "Year");
		String months = formatPart(trainingDuration.getMonths(), "Month");
		String days   = formatPart(trainingDuration.getDays(), "Day");
		return String.join(" ", years, months, days).trim();
	}

	private static String formatPart(int value, String unit) {
		if (value == 0) return "";
		return value + " " + (value == 1 ? unit : unit + "s");
	}

	public ResponseEntity<ApiResponse<Object>> setUserSettings(@Valid UserSettingsDTO dto, String refreshToken) {
		String userEmail = jwtUtils.extractSubject(refreshToken);
		//Validate if user exists
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> {
					logger.warning(Messages.USER_NOT_FOUND);
					return new UsernameNotFoundException(Messages.USER_NOT_FOUND);
				});

		user.setUserAvatarPath(dto.getUserAvatarPath());

		//Create an access token
		String accessToken = accessTokenService.generateAccessToken(userEmail);

		//Return apiResponse
		UserDTO userDTO = new UserDTO(user);
		return ResponseEntity.ok(
				ApiResponse.builder()
						.success(true)
						.message("User settings updated.")
						.data(userDTO)
						.accessToken(accessToken)
						.build()
		);
	}
}
