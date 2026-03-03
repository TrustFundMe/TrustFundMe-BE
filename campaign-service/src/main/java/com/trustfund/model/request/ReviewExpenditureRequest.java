package com.trustfund.model.request;
 
 import jakarta.validation.constraints.NotBlank;
 import lombok.AllArgsConstructor;
 import lombok.Builder;
 import lombok.Data;
 import lombok.NoArgsConstructor;
 
 @Data
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 public class ReviewExpenditureRequest {
 
     @NotBlank(message = "Trạng thái không được để trống")
     private String status;
 
     private Long staffId;
 
     private String reasonReject;
 }
