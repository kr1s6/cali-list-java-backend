package com.CalisthenicList.CaliList.model.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtResponseDTO {
    private String message;
    private Status status;
    private String exceptionType;
    private String jwt;
    private Jws<Claims> jws;

    public enum Status {
        SUCCESS, ERROR
    }

    public JwtResponseDTO() {
    }

    public JwtResponseDTO(String jwt) {
        this.jwt = jwt;
        this.status = Status.SUCCESS;
    }

    public JwtResponseDTO(Jws<Claims> jws) {
        this.jws = jws;
        this.status = Status.SUCCESS;
    }

}
