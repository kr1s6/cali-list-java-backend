package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.enums.Roles;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

import static com.CalisthenicList.CaliList.constants.UserConstants.PASSWORD_MIN_LENGTH;
import static com.CalisthenicList.CaliList.constants.UserConstants.USERNAME_MAX_LENGTH;

@Getter
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Size(min = 1, max = USERNAME_MAX_LENGTH, message = Messages.USERNAME_LENGTH_ERROR)
	@NotBlank(message = Messages.USERNAME_NOT_BLANK_ERROR)
	@Column(nullable = false, unique = true, length = USERNAME_MAX_LENGTH)
	private String username;

	@Email(message = Messages.EMAIL_INVALID_ERROR)
	@NotBlank(message = Messages.EMAIL_INVALID_ERROR)
	@Column(nullable = false, unique = true)
	private String email;

	@Size(min = PASSWORD_MIN_LENGTH, message = Messages.PASSWORD_LENGTH_ERROR)
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Roles role;

	@Past(message = Messages.BIRTHDATE_PAST_ERROR)
	private LocalDate birthDate;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdDate;

	@LastModifiedDate
	private Instant updatedDate;

	public User(String username, String email, String password) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.role = Roles.ROLE_USER;
	}
}

