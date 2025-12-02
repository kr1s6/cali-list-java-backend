package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.enums.Roles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
	private UUID id;
	private String username;
	private String email;
	private Roles role;
	private boolean emailVerified;
	private LocalDate birthdate;
	private LocalDate caliStartDate;

	public UserDTO(User user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.email = user.getEmail();
		this.role = user.getRole();
		this.emailVerified = user.isEmailVerified();
		this.birthdate = user.getBirthdate();
		this.caliStartDate = user.getCaliStartDate();
	}
}
