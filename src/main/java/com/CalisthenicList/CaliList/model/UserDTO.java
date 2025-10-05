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
	private LocalDate birthDate;
}
