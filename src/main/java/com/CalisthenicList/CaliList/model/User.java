package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.enums.Roles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
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

    @Size(min = 1, max = 20, message = "The username must be between 1 and 30 characters long.")
    @NotBlank(message = "The username must not be blank.")
    private String username;

    @Email(message = "Invalid email address.")
    @NotBlank(message = "The email address must not be blank.")
    private String email;

    @Size(min = 8, message = "The password must be at least 8 characters long.")
    @NotBlank(message = "The password must not be blank.")
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

