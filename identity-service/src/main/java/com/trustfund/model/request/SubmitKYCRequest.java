package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitKYCRequest {
    @NotBlank(message = "ID type is required")
    private String idType;

    @NotBlank(message = "ID number is required")
    private String idNumber;

    @NotNull(message = "Issue date is required")
    @Past(message = "Issue date must be in the past")
    private LocalDate issueDate;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    @NotBlank(message = "Issue place is required")
    private String issuePlace;

    @NotBlank(message = "Front ID image is required")
    private String idImageFront;

    @NotBlank(message = "Back ID image is required")
    private String idImageBack;

    @NotBlank(message = "Selfie image is required")
    private String selfieImage;
}
