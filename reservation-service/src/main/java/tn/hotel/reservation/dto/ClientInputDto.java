package tn.hotel.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientInputDto {

    @NotBlank(message = "firstName is required")
    @Size(max = 80, message = "firstName must not exceed 80 characters")
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(max = 80, message = "lastName must not exceed 80 characters")
    private String lastName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    private String phone;
}
