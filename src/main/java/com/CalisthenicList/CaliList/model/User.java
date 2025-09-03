package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.enums.Roles;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Data //annotation that bundles features of @ToString, @EqualsAndHashCode, @Getter/@Setter, @RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "password")
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Size(min = 1, max = 20, message = Messages.USERNAME_LENGTH_ERROR)
	@NotBlank(message = Messages.USERNAME_NOT_BLANK_ERROR)
	@Column(nullable = false, unique = true, length = 20)
	private String username;

	@Email(message = Messages.EMAIL_INVALID_ERROR)
	@NotBlank(message = Messages.EMAIL_INVALID_ERROR)
	@Column(nullable = false, unique = true)
	private String email;

	@Size(min = 8, message = Messages.PASSWORD_LENGTH_ERROR)
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	@Column(nullable = false)
	private String password;

	@Past(message = Messages.BIRTHDATE_PAST_ERROR)
	private LocalDate birthDate = null;

	@CreatedDate
	private Instant createdDate;

	@LastModifiedDate
	private Instant updatedDate;

	@Enumerated(EnumType.STRING)
	private Roles role = Roles.ROLE_USER;
}

