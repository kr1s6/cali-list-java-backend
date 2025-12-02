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
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.CalisthenicList.CaliList.constants.UserConstants.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User implements UserDetails {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH, message = Messages.USERNAME_LENGTH_ERROR)
	@NotBlank(message = Messages.USERNAME_LENGTH_ERROR)
	@Column(nullable = false, unique = true, length = USERNAME_MAX_LENGTH)
	private String username;

	@Email(message = Messages.EMAIL_INVALID_ERROR)
	@NotBlank(message = Messages.EMAIL_INVALID_ERROR)
	@Column(nullable = false, unique = true)
	private String email;

	@Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH, message = Messages.PASSWORD_LENGTH_ERROR)
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	@Column(nullable = false, length = PASSWORD_MAX_LENGTH)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Roles role;

	@Column(nullable = false)
	private boolean emailVerified;

	@Past(message = Messages.DATE_SHOULD_BE_PAST)
	private LocalDate birthdate = null;

	@Past(message = Messages.DATE_SHOULD_BE_PAST)
	private LocalDate caliStartDate = null;

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
		this.emailVerified = false;
	}

	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.name()));
	}

	public boolean isEnabled() {
		return true;
	}

	public boolean isCredentialsNonExpired() {
		return true;
	}

	public boolean isAccountNonLocked() {
		return true;
	}

	public boolean isAccountNonExpired() {
		return true;
	}
}

