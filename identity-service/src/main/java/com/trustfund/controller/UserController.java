package com.trustfund.controller;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.model.dto.response.ApiResponse;
import com.trustfund.model.dto.response.ImportResult;
import com.trustfund.model.request.CreateUserRequest;
import com.trustfund.model.request.UpdateUserRequest;
import com.trustfund.model.response.CheckEmailResponse;
import com.trustfund.model.response.UserInfo;
import com.trustfund.service.interfaceServices.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all users (paginated)", description = "Retrieve a paginated list of all users (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<UserInfo>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/staff")
    @Operation(summary = "Get all staff members", description = "Retrieve all active staff members (authenticated users)")
    public ResponseEntity<List<UserInfo>> getAllStaffs() {
        return ResponseEntity.ok(userService.getAllStaffs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve user information by user ID")
    public ResponseEntity<UserInfo> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Create a new user", description = "Create a new user account manually (Admin/Staff only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserInfo>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Create user request - email: {}, fullName: {}, role: {}", request.getEmail(), request.getFullName(), request.getRole());
        UserInfo user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user, "Tạo người dùng thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER', 'FUND_OWNER', 'FUND_DONOR')")
    @Operation(summary = "Update user", description = "Update user information by user ID (User can only update their own profile)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserInfo> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Delete user", description = "Permanently delete a user by user ID (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ban")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Ban user", description = "Ban/deactivate a user account (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserInfo> banUser(@PathVariable("id") Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(userService.banUser(id, reason));
    }

    @PutMapping("/{id}/unban")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Unban user", description = "Unban/activate a user account (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserInfo> unbanUser(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.unbanUser(id));
    }

    @PutMapping("/{id}/upgrade-to-fund-donor")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Upgrade user to FUND_DONOR", description = "Upgrade user role to FUND_DONOR after KYC verification (Admin/Staff only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> upgradeToFundDonor(@PathVariable("id") Long id) {
        userService.upgradeToFundDonor(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check email existence", description = "Check if email already exists in database (public endpoint for sign-in vs sign-up flow)")
    public ResponseEntity<CheckEmailResponse> checkEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.checkEmail(email));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export users to Excel", description = "Download an Excel file containing all users (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InputStreamResource> exportUsers() {
        String filename = "quanlynguoidung_" + java.time.LocalDate.now() + ".xlsx";
        InputStreamResource file = new InputStreamResource(userService.exportUsersToExcel());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Import users from Excel", description = "Upload an Excel file to import multiple users (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ImportResult>> importUsers(@RequestParam("file") MultipartFile file) {
        log.info("Import request - filename: {}, contentType: {}, size: {}",
                file.getOriginalFilename(), file.getContentType(), file.getSize());
        try {
            ImportResult result = userService.importUsersFromExcel(file);
            String msg = buildImportMessage(result);
            return ResponseEntity.ok(ApiResponse.success(result, msg));
        } catch (BadRequestException e) {
            log.warn("Import validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Import failed", e);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(ApiResponse.error("Could not import the file: " + file.getOriginalFilename() + "!", 500));
        }
    }

    private String buildImportMessage(ImportResult r) {
        if (r.getImported() > 0 && r.getSkipped() > 0) {
            return String.format("Nhập thành công %d người dùng. Bỏ qua %d dòng trùng lặp.", r.getImported(), r.getSkipped());
        } else if (r.getImported() > 0) {
            return String.format("Nhập thành công %d người dùng.", r.getImported());
        } else if (r.getSkipped() > 0) {
            return String.format("Tất cả %d dòng bị bỏ qua (trùng lặp hoặc không hợp lệ).", r.getSkipped());
        }
        return "Không có dòng nào để nhập.";
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Download users import template", description = "Download a blank Excel template for importing users (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InputStreamResource> downloadUsersTemplate() {
        String filename = "template_import_nguoidung.xlsx";
        InputStreamResource file = new InputStreamResource(userService.downloadUsersTemplate());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}
