package com.CalisthenicList.CaliList.service;
import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.ApiResponse;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserDeleteByIdDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
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
	private PasswordEncoder encoder;
	@InjectMocks
	private UserService userService;
	private UserDeleteByIdDTO userDeleteByIdDto;
	private final UUID userId = UUID.randomUUID();
	private User user;

	@BeforeEach
	void setUp() {
		userDeleteByIdDto = new UserDeleteByIdDTO();
		userDeleteByIdDto.setUserId(userId);
		userDeleteByIdDto.setPassword("rawPassword");
		user = new User();
		user.setId(userId);
		user.setPassword("encodedPassword");
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
}