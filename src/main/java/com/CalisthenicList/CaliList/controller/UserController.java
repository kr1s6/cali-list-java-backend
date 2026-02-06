package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.*;
import com.CalisthenicList.CaliList.model.DTO.UserBirthdateDTO;
import com.CalisthenicList.CaliList.model.DTO.CaliStartDateDTO;
import com.CalisthenicList.CaliList.model.DTO.UserDeleteByIdDTO;
import com.CalisthenicList.CaliList.model.DTO.UserSettingsDTO;
import com.CalisthenicList.CaliList.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("api/v1")
public class UserController {
	public static final String deleteUserUrl = "/deleteUser";
	public static final String updateUserBirthdateUrl = "/set-user-birthdate";
	public static final String updateUserCaliStartDateUrl = "/set-user-cali-start-date";
	public static final String updateUserSettingsUrl = "/set-user-settings";
	private final UserService userService;

	@DeleteMapping(deleteUserUrl)
	public ResponseEntity<ApiResponse<Object>> deleteUserById(@Valid @RequestBody UserDeleteByIdDTO dto) {
		return userService.deleteUserById(dto);
//      TODO
//       - need to be secured for admin, tests and for user to delete himself
	}

	@PatchMapping(updateUserBirthdateUrl)
	public ResponseEntity<ApiResponse<Object>> setUserBirthdate(@CookieValue(name = "refreshToken", required = false) String refreshToken,
																@Valid @RequestBody UserBirthdateDTO dto) {
		return userService.setUserBirthdate(dto, refreshToken);
	}

	@PatchMapping(updateUserCaliStartDateUrl)
	public ResponseEntity<ApiResponse<Object>> setUserCaliStartDate(@CookieValue(name = "refreshToken", required = false) String refreshToken,
																	@Valid @RequestBody CaliStartDateDTO dto) {
		return userService.setUserCaliStartDate(dto, refreshToken);
	}

	@PatchMapping(updateUserSettingsUrl)
	public ResponseEntity<ApiResponse<Object>> setUserSettings(@CookieValue(name = "refreshToken", required = false) String refreshToken,
																@Valid @RequestBody UserSettingsDTO dto) {
		return userService.setUserSettings(dto, refreshToken);
	}
}
