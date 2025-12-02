package com.CalisthenicList.CaliList.service;
import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.*;
import com.CalisthenicList.CaliList.repositories.RefreshTokenRepository;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.tokens.AccessTokenService;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RequiredArgsConstructor
class UserServiceTest {
	@Mock
	private UserRepository userRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private PasswordEncoder encoder;
	@Mock
	private JwtUtils jwtUtils;
	@Mock
	private AccessTokenService accessTokenService;
	@InjectMocks
	private UserService userService;
	private UserDeleteByIdDTO userDeleteByIdDto;
	private final UUID userId = UUID.randomUUID();
	private User user;
	private String email;

	@BeforeEach
	void setUp() {
		userDeleteByIdDto = new UserDeleteByIdDTO();
		userDeleteByIdDto.setUserId(userId);
		userDeleteByIdDto.setPassword("rawPassword");
		email = "test@email.com";
		user = new User();
		user.setId(userId);
		user.setPassword("encodedPassword");
		user.setEmail(email);
	}

	@Nested
	@DisplayName("deleteUserById")
	class DeleteUserByIdTest {

		private ResponseEntity<ApiResponse<Object>> deleteUserById(UserDeleteByIdDTO userDeleteByIdDto) {
			return userService.deleteUserById(userDeleteByIdDto);
		}

		@Test
		@DisplayName("✅ Happy Case: Delete user successfully")
		void givenValidUserAndPassword_whenDeleteUser_thenReturnOk() {
			// Given
			when(userRepository.findById(userId)).thenReturn(Optional.of(user));
			when(refreshTokenRepository.findByUserEmail(email)).thenReturn(Optional.of(new RefreshToken()));
			when(encoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
			// When
			ResponseEntity<ApiResponse<Object>> response = deleteUserById(userDeleteByIdDto);
			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(Messages.USER_DELETED, response.getBody().getMessage());
			verify(userRepository).delete(user);
		}

		@Test
		@DisplayName("❌ Negative Case: User not found")
		void givenNonExistingUser_whenDeleteUser_thenThrowUsernameNotFoundException() {
			// Given
			when(userRepository.findById(userId)).thenReturn(Optional.empty());
			// When Then
			UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
					() -> deleteUserById(userDeleteByIdDto));
			assertEquals(Messages.SERVICE_ERROR, exception.getMessage());
			verify(userRepository, never()).delete(any());
		}

		@Test
		@DisplayName("❌ Negative Case: Invalid password")
		void givenWrongPassword_whenDeleteUser_thenThrowBadCredentialsException() {
			// Given
			when(userRepository.findById(userId)).thenReturn(Optional.of(user));
			when(encoder.matches("rawPassword", "encodedPassword")).thenReturn(false);
			// When Then
			BadCredentialsException exception = assertThrows(BadCredentialsException.class,
					() -> deleteUserById(userDeleteByIdDto));
			assertEquals(Messages.SERVICE_ERROR, exception.getMessage());
			verify(userRepository, never()).delete(any());
		}
	}

	@Nested
	@DisplayName("setUserBirthdate")
	class SetUserBirthdateTest {
		private final String refreshToken = "dummyToken";
		private final String userEmail = "test@email.com";
		private BirthdateDTO birthdateDTO;

		@BeforeEach
		void setUpBirthdate() {
			birthdateDTO = new BirthdateDTO();
			birthdateDTO.setBirthdate(LocalDate.of(2000, 1, 1));
			user.setEmail(userEmail);
		}

		@Test
		@DisplayName("✅ Happy Case: Set user's birthdate successfully")
		void givenValidUserAndBirthdate_whenSetUserBirthdate_thenReturnOk() {
			// Given
			when(jwtUtils.extractSubject(refreshToken)).thenReturn(userEmail);
			when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
			when(accessTokenService.generateAccessToken(userEmail)).thenReturn("newAccessToken");
			when(userRepository.save(user)).thenReturn(user);
			// When
			ResponseEntity<ApiResponse<Object>> response = userService.setUserBirthdate(birthdateDTO, refreshToken);
			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertTrue(response.getBody().isSuccess());
			assertEquals("Birthday set.", response.getBody().getMessage());
			assertEquals("newAccessToken", response.getBody().getAccessToken());
			UserDTO responseUserDTO = (UserDTO) response.getBody().getData();
			assertEquals(user.getBirthdate(), responseUserDTO.getBirthdate());
			verify(userRepository).save(user);
		}

		@Test
		@DisplayName("❌ Negative Case: User not found")
		void givenNonExistingUser_whenSetUserBirthdate_thenThrowUsernameNotFoundException() {
			// Given
			when(jwtUtils.extractSubject(refreshToken)).thenReturn(userEmail);
			when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());
			// When / Then
			UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
					() -> userService.setUserBirthdate(birthdateDTO, refreshToken));
			assertEquals(Messages.USER_NOT_FOUND, exception.getMessage());
			verify(userRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("setUserCaliStartDate")
	class SetUserCaliStartDateTest {
		private final String refreshToken = "dummyToken";
		private final String userEmail = "test@email.com";
		private CaliStartDateDTO caliStartDateDTO;

		@BeforeEach
		void setUpCaliStartDate() {
			caliStartDateDTO = new CaliStartDateDTO();
			caliStartDateDTO.setCaliStartDate(LocalDate.of(2022, 5, 1));
			user.setEmail(userEmail);
		}

		@Test
		@DisplayName("✅ Happy Case: Set user's cali start date successfully")
		void givenValidUserAndCaliStartDate_whenSetUserCaliStartDate_thenReturnOk() {
			// Given
			when(jwtUtils.extractSubject(refreshToken)).thenReturn(userEmail);
			when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
			when(accessTokenService.generateAccessToken(userEmail)).thenReturn("newAccessToken");
			when(userRepository.save(user)).thenReturn(user);
			// When
			ResponseEntity<ApiResponse<Object>> response = userService.setUserCaliStartDate(caliStartDateDTO, refreshToken);
			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertTrue(response.getBody().isSuccess());
			assertEquals("Cali start date set.", response.getBody().getMessage());
			assertEquals("newAccessToken", response.getBody().getAccessToken());
			UserDTO responseUserDTO = (UserDTO) response.getBody().getData();
			assertEquals(user.getCaliStartDate(), responseUserDTO.getCaliStartDate());
			verify(userRepository).save(user);
		}

		@Test
		@DisplayName("❌ Negative Case: User not found")
		void givenNonExistingUser_whenSetUserCaliStartDate_thenThrowUsernameNotFoundException() {
			// Given
			when(jwtUtils.extractSubject(refreshToken)).thenReturn(userEmail);
			when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());
			// When / Then
			UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
					() -> userService.setUserCaliStartDate(caliStartDateDTO, refreshToken));
			assertEquals(Messages.USER_NOT_FOUND, exception.getMessage());
			verify(userRepository, never()).save(any());
		}
	}
}