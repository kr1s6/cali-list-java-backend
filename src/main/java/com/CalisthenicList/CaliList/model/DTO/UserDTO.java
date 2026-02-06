package com.CalisthenicList.CaliList.model.DTO;

import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
	private Long id;
	private String username;
	private String email;
	private Roles role;
	private boolean emailVerified;
	private LocalDate birthdate;
	private String trainingDuration;
	private String avatarKey;

	public UserDTO(User user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.email = user.getEmail();
		this.role = user.getRole();
		this.emailVerified = user.isEmailVerified();
		this.birthdate = user.getBirthdate();
		this.trainingDuration = user.getTrainingDuration();
		this.avatarKey = user.getAvatarKey();
	}
}
