package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
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

    @Size(min = 1, max = 20, message = Messages.USERNAME_LENGTH_ERROR)
    @NotBlank(message = Messages.USERNAME_NOT_BLANK_ERROR)
    private String username;

    @Email(message = Messages.EMAIL_INVALID_ERROR)
    @NotBlank(message = Messages.EMAIL_NOT_BLANK_ERROR)
    private String email;

    @Size(min = 8, message = Messages.PASSWORD_LENGTH_ERROR)
    @NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
    private String password;

    @Past(message = Messages.BIRTHDATE_PAST_ERROR)
    private Date birthDate = null;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;
    private Roles role = Roles.ROLE_USER;
    private String gender;
}

