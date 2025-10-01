package com.CalisthenicList.CaliList.controller;


import com.CalisthenicList.CaliList.model.UserDeleteByIdDTO;
import com.CalisthenicList.CaliList.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class UserController {
	public static final String deleteUserByIdUrl = "/deleteUser";
	private final UserService userService;

	@DeleteMapping(deleteUserByIdUrl)
	public ResponseEntity<String> deleteUserById(@Valid @RequestBody UserDeleteByIdDTO userDeleteByIdDto) {
		return userService.deleteUserById(userDeleteByIdDto);
//      TODO
//       - need to be secured for admin, tests and for user to delete himself
	}
}
