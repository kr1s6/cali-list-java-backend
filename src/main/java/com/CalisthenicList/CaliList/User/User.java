package com.CalisthenicList.CaliList.User;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "users")
@Data //annotation that bundles features of @ToString, @EqualsAndHashCode, @Getter/@Setter, @RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    private ObjectId id;

    @Size(min = 1, max = 30, message = "The name must be between 1 and 30 characters long.")
    @NotEmpty(message = "The name must not be empty.")
    private String name;

    @Email(message = "Invalid email address.")
    @NotEmpty(message = "The email address must not be empty.")
    private String email;

    @Size(min = 8, message = "The password must be at least 8 characters long.")
    @Pattern(regexp = ".*\\d.*", message = "The password must contain at least one number.")
    @Pattern(regexp = ".*[a-z].*", message = "The password must contain at least one lowercase letter.")
    @Pattern(regexp = ".*[A-Z].*", message = "The password must contain at least one uppercase letter.")
    @NotEmpty(message = "The password must not be empty.")
    private String password;

    @Past(message = "The date of birth must be in the past.")
    private Date birthDate = null;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;
    private Roles role = Roles.ROLE_USER;
    private String gender;
}

//PBKDF2