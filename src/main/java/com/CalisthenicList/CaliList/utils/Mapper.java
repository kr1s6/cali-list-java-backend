package com.CalisthenicList.CaliList.utils;

import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import lombok.experimental.UtilityClass;

@UtilityClass
//INFO - @UtilityClass is for static functions
public class Mapper {

	public static User newUser(UserRegistrationDTO userDTO) {
		return new User(userDTO.getUsername(), userDTO.getEmail(), userDTO.getPassword());
	}
}
