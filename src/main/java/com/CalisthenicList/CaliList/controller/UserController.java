package com.CalisthenicList.CaliList.controller;


import com.CalisthenicList.CaliList.model.ApiResponse;
import com.CalisthenicList.CaliList.model.BirthdateDTO;
import com.CalisthenicList.CaliList.model.CaliStartDateDTO;
import com.CalisthenicList.CaliList.model.UserDeleteByIdDTO;
import com.CalisthenicList.CaliList.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class UserController {
	public static final String deleteUserByIdUrl = "/api/deleteUser";
	public static final String patchUserBirthdateUrl = "/api/set-user-birthday";
	public static final String patchUserCaliStartDateUrl = "/api/set-user-cali-start-day";
	private final UserService userService;

	@DeleteMapping(deleteUserByIdUrl)
	public ResponseEntity<ApiResponse<Object>> deleteUserById(@Valid @RequestBody UserDeleteByIdDTO dto) {
		return userService.deleteUserById(dto);
//      TODO
//       - need to be secured for admin, tests and for user to delete himself
	}

	@PatchMapping(patchUserBirthdateUrl)
	public ResponseEntity<ApiResponse<Object>> setUserBirthdate(@Valid @RequestBody BirthdateDTO dto,
																@CookieValue(name = "refreshToken") String refreshToken) {
		return userService.setUserBirthdate(dto, refreshToken);
	}

	@PatchMapping(patchUserCaliStartDateUrl)
	public ResponseEntity<ApiResponse<Object>> setUserCaliStartDate(@Valid @RequestBody CaliStartDateDTO dto,
																	@CookieValue(name = "refreshToken") String refreshToken){
		return userService.setUserCaliStartDate(dto, refreshToken);
	}
}
