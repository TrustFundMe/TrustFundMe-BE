# Business Rules - TrustFundMe Platform

## 1. Authentication & Authorization

| ID | Rule Definition |
|----|-----------------|
| BR-01 | Email must be unique when registering a new account. |
| BR-02 | Password must be encrypted using BCrypt before storing in database. |
| BR-03 | New registered user has default role as USER. |
| BR-04 | Login requires valid email and password, returns access token and refresh token. |
| BR-05 | Refresh token is used to obtain new access token when expired. |
| BR-06 | Google login authenticates via Google ID Token, creates new user if not exists. |
| BR-07 | User logging in via Google is automatically marked as verified = true. |
| BR-08 | OTP for password reset is 6 digits, expires after 10 minutes (configurable). |
| BR-09 | Each OTP can only be used once. |
| BR-10 | Email verification uses JWT token with type password_reset. |
| BR-11 | System roles: USER, FUND_OWNER, FUND_DONOR, STAFF, ADMIN. |
| BR-12 | User is automatically upgraded to FUND_OWNER role when creating a campaign. |
| BR-13 | User is upgraded to FUND_DONOR after KYC is verified. |

## 2. User Management

| ID | Rule Definition |
|----|-----------------|
| BR-14 | User can update their own profile, ADMIN can update any user. |
| BR-15 | Only ADMIN can delete a user account. |
| BR-16 | ADMIN and STAFF can ban or unban users. |
| BR-17 | Public API to check if email already exists in system. |
| BR-18 | Only ADMIN and STAFF can view all users list. |

## 3. KYC (Identity Verification)

| ID | Rule Definition |
|----|-----------------|
| BR-19 | Only STAFF or ADMIN can submit KYC on behalf of a user. |
| BR-20 | KYC status must be one of: PENDING, VERIFIED, REJECTED. |
| BR-21 | Campaign can only be APPROVED when fund owner has verified KYC. |
| BR-22 | Authenticated user can view their own KYC status. |
| BR-23 | STAFF and ADMIN can view all KYC requests. |
| BR-24 | System can filter KYC requests with PENDING status. |

## 4. Bank Account

| ID | Rule Definition |
|----|-----------------|
| BR-25 | User can add multiple bank accounts. |
| BR-26 | Bank account status must be one of: PENDING, APPROVED, REJECTED. |
| BR-27 | Only STAFF and ADMIN can approve bank accounts. |
| BR-28 | Only account owner can delete their own bank account. |
| BR-29 | User cannot add duplicate account numbers. |

## 5. Campaign

| ID | Rule Definition |
|----|-----------------|
| BR-30 | Campaign status must be one of: PENDING_APPROVAL, APPROVED, REJECTED, DELETED, DISABLED. |
| BR-31 | User creating a campaign is automatically upgraded to FUND_OWNER role. |
| BR-32 | Newly created campaign defaults to PENDING_APPROVAL status. |
| BR-33 | Only STAFF and ADMIN can approve campaigns, KYC must be verified. |
| BR-34 | Rejecting a campaign requires a rejection reason. |
| BR-35 | Only FUND_OWNER (owner), STAFF, or ADMIN can update campaign. |
| BR-36 | Deleted campaigns are soft-deleted (status = DELETED), not physically removed. |
| BR-37 | STAFF and ADMIN can filter campaigns by status. |
| BR-38 | Public can filter campaigns by category. |
| BR-39 | Users can view all campaigns created by a specific fund owner. |
| BR-40 | Disabled campaign cannot be edited. |

## 6. Campaign Category

| ID | Rule Definition |
|----|-----------------|
| BR-41 | Only STAFF and ADMIN can create, update, or delete campaign categories. |
| BR-42 | Campaign categories are publicly visible to all users. |

## 7. Fundraising Goal

| ID | Rule Definition |
|----|-----------------|
| BR-43 | Fundraising goal must be associated with a campaign and has target amount. |
| BR-44 | FUND_OWNER, STAFF, and ADMIN can update fundraising goal. |
| BR-45 | Only STAFF and ADMIN can delete fundraising goal. |
| BR-46 | Active goal is used to calculate campaign progress percentage. |

## 8. Expenditure

| ID | Rule Definition |
|----|-----------------|
| BR-47 | Expenditure must be associated with a campaign and contains list of items. |
| BR-48 | Expenditure items have quantity and quantityLeft for limited quantity funding. |
| BR-49 | Expenditure status must be one of: PENDING, APPROVED, REJECTED. |
| BR-50 | Goal-based expenditures are automatically approved. |
| BR-51 | Fund owner can request withdrawal from expenditure. |
| BR-52 | Fund owner can upload disbursement proof for expenditure. |
| BR-53 | Fund owner can update actual amount spent for expenditure. |

## 9. Campaign Follow

| ID | Rule Definition |
|----|-----------------|
| BR-54 | Users can follow campaigns to receive update notifications. |
| BR-55 | Users can unfollow campaigns they previously followed. |

## 10. Payment & Donation

| ID | Rule Definition |
|----|-----------------|
| BR-56 | PayOS payment generates unique orderCode from timestamp and donation ID. |
| BR-57 | Total donation amount equals donationAmount plus tipAmount. |
| BR-58 | Donor can choose to donate anonymously (isAnonymous flag). |
| BR-59 | PayOS webhook callback updates payment status. |
| BR-60 | System calls PayOS API to verify actual payment status. |
| BR-61 | Upon successful payment, quantityLeft is deducted for funded items. |
| BR-62 | System checks quantityLeft before allowing payment. |
| BR-63 | Campaign progress percentage equals raised divided by goal, capped at 100%. |
| BR-64 | Recent donors list shows last N donors with anonymous handling. |
| BR-65 | Payment description is truncated to 50 characters (PayOS limit). |

## 11. Chat & Messaging

| ID | Rule Definition |
|----|-----------------|
| BR-66 | Conversation is created between Fund Owner/Donor and Staff. |
| BR-67 | STAFF can view all conversations in the system. |
| BR-68 | Regular users can only view their own conversations. |
| BR-69 | System automatically sends welcome message when conversation is created. |
| BR-70 | First staff member to reply is assigned as conversation handler. |
| BR-71 | Only conversation participants can send messages. |

## 12. Appointment Schedule

| ID | Rule Definition |
|----|-----------------|
| BR-72 | Users can create appointment schedules between donor and staff. |
| BR-73 | Appointment status must be one of: PENDING, CONFIRMED, COMPLETED, CANCELLED. |
| BR-74 | Users can update appointment information and status. |
| BR-75 | System can filter appointments by donor ID. |
| BR-76 | System can filter appointments by assigned staff ID. |

## 13. Feed Post

| ID | Rule Definition |
|----|-----------------|
| BR-77 | Authenticated user can create new feed post, default status is DRAFT. |
| BR-78 | Feed post status must be one of: DRAFT, ACTIVE. |
| BR-79 | Feed post visibility must be one of: PUBLIC, PRIVATE, FOLLOWERS. |
| BR-80 | Only post author can update post content. |
| BR-81 | Only post author can delete their own post, ADMIN can force delete. |
| BR-82 | STAFF and ADMIN can pin posts to display at top. |
| BR-83 | STAFF and ADMIN can lock posts to disable comments. |
| BR-84 | View count increments once per user or IP address, cached for 1 hour. |
| BR-85 | Users can like or unlike posts, toggle functionality. |
| BR-86 | PUBLIC visibility posts are visible to all users. |
| BR-87 | PRIVATE visibility posts are only visible to the author. |
| BR-88 | FOLLOWERS visibility posts are only visible to followers. |

## 14. Feed Comment

| ID | Rule Definition |
|----|-----------------|
| BR-89 | Authenticated users can comment on posts. |
| BR-90 | Only comment author can edit their own comment. |
| BR-91 | Comment author or ADMIN can delete comments. |
| BR-92 | Users can toggle like on comments. |

## 15. Forum Attachment

| ID | Rule Definition |
|----|-----------------|
| BR-93 | Attachment type must be one of: IMAGE, VIDEO, DOCUMENT. |
| BR-94 | Attachments have displayOrder for sorting. |

## 16. Security & Validation

| ID | Rule Definition |
|----|-----------------|
| BR-95 | All authenticated APIs require valid JWT token. |
| BR-96 | Role-based access control is enforced before allowing operations. |
| BR-97 | Soft delete is used instead of physical deletion. |
| BR-98 | Only STAFF and ADMIN can view deleted campaigns. |

## 17. Data Consistency

| ID | Rule Definition |
|----|-----------------|
| BR-99 | ADMIN can run sync job to correct comment counts. |
| BR-100 | Like count cannot be negative. |
| BR-101 | View count cannot be negative. |
| BR-102 | Quantity left cannot be negative. |

## 18. API Gateway & Microservices

| ID | Rule Definition |
|----|-----------------|
| BR-103 | Microservices communicate via internal API endpoints. |
| BR-104 | Campaign service validates user existence via Identity service. |
