package com.trustfund.model.response;

import com.trustfund.model.enums.KYCStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class KYCResponse {
    private Long id;
    private Long userId;
    // ── OCR fields (từ CCCD/hộ chiếu) ──
    private String fullName;          // họ tên trên CCCD
    private String address;           // địa chỉ thường trú
    private String workplace;         // nơi làm việc
    private String taxId;             // mã số thuế cá nhân
    // ── Tài khoản user ──
    private String email;
    private String phoneNumber;
    // ── ID document ──
    private String idType;
    private String idNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuePlace;
    private String idImageFront;
    private String idImageBack;
    private String selfieImage;
    private KYCStatus status;
    private String rejectionReason;
    // ── Face biometric data ──
    private String faceDescriptor;
    private String livenessMetadata;
    private String faceMeshSample;
    /** Whether this KYC has passed face liveness check */
    private boolean livenessVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KYCResponse() {
    }

    public KYCResponse(
            Long id,
            Long userId,
            String fullName,
            String address,
            String workplace,
            String taxId,
            String email,
            String phoneNumber,
            String idType,
            String idNumber,
            LocalDate issueDate,
            LocalDate expiryDate,
            String issuePlace,
            String idImageFront,
            String idImageBack,
            String selfieImage,
            KYCStatus status,
            String rejectionReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.address = address;
        this.workplace = workplace;
        this.taxId = taxId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.idType = idType;
        this.idNumber = idNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.issuePlace = issuePlace;
        this.idImageFront = idImageFront;
        this.idImageBack = idImageBack;
        this.selfieImage = selfieImage;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWorkplace() {
        return workplace;
    }

    public void setWorkplace(String workplace) {
        this.workplace = workplace;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getIssuePlace() {
        return issuePlace;
    }

    public void setIssuePlace(String issuePlace) {
        this.issuePlace = issuePlace;
    }

    public String getIdImageFront() {
        return idImageFront;
    }

    public void setIdImageFront(String idImageFront) {
        this.idImageFront = idImageFront;
    }

    public String getIdImageBack() {
        return idImageBack;
    }

    public void setIdImageBack(String idImageBack) {
        this.idImageBack = idImageBack;
    }

    public String getSelfieImage() {
        return selfieImage;
    }

    public void setSelfieImage(String selfieImage) {
        this.selfieImage = selfieImage;
    }

    public KYCStatus getStatus() {
        return status;
    }

    public void setStatus(KYCStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
