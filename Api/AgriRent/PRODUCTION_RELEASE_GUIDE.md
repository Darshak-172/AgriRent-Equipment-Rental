# AgriRent Production Release Guide

**Date**: March 13, 2026  
**Project**: AgriRent Full Platform Release  
**Scope**: ASP.NET Core API, Admin Web, Android Mobile App  
**Release Status**: Conditionally ready for production after final release blockers are closed

---

## 1. Release Summary

AgriRent is a multi-role agriculture marketplace and rental platform with the following released capability set:

- Farmer authentication, profile, equipment booking, and product ordering
- Equipment owner listing management and booking approval workflow
- Seller product management and order handling
- Admin review, approval, plan management, and operational oversight
- Subscription purchase and activation with Razorpay integration
- Firebase notification support
- Multi-language response support
- Android mobile client aligned with the current backend API contract

As of this document revision:

- Backend build status is verified successful with 0 warnings and 0 errors
- Android app build has previously been verified successful after the latest API-alignment fixes
- Core backend release protections such as HTTPS redirection, authorization, and payment signature verification are present
- The codebase is feature-complete for most business flows

This project should be treated as **release-candidate quality**, not fully production-approved yet, until the release blockers in Section 5 are completed.

---

## 2. Release Scope

### Backend and Admin Web

- ASP.NET Core 10.0 application
- SQL Server database with EF Core migrations
- JWT-based authentication and refresh tokens
- Role-based access control
- Razorpay payment and subscription activation
- Firebase notification integration
- Admin web workflows for approval and plan management

### Android Mobile App

- Authentication and session management
- Equipment browsing and booking
- Product browsing and ordering
- Seller product creation and management
- Subscription views and slot usage display
- Notification routing and language/theme support

---

## 3. Verified Current State

### Verified Ready

- Backend compiles successfully
- Firebase initialization warnings removed
- Authentication response contract aligned between API and app
- Search API aligned with real product data
- Booking response parsing aligned between API and app
- Subscription slot fallback logic implemented in the Android app
- Product management and add-product flows added in the Android app

### Verified Present in Code

- HTTPS redirection enabled in application startup
- Razorpay payment signature verification implemented in subscription verification flow
- Rate limiter middleware is registered in the backend pipeline
- OrderItems are created during order placement

### Not Yet Verified for Production Sign-off

- End-to-end smoke testing in a production-like environment
- Real device Android validation against deployed backend
- Load and concurrency validation for ordering and booking hotspots
- Full test coverage for critical backend business flows

---

## 4. Production Release Requirements

The following conditions must be satisfied before approving the first production release.

### Infrastructure

- Production SQL Server instance provisioned and reachable
- Production hosting environment configured for ASP.NET Core 10.0
- HTTPS certificate configured correctly
- Domain and DNS configured
- Application logging enabled
- Health check endpoint monitored
- Backup and restore strategy documented and tested

### Secrets and Configuration

- JWT signing key stored securely outside source control
- Razorpay keys stored securely outside source control
- Firebase service-account credentials stored securely outside source control
- Translator and OTP provider keys stored securely outside source control
- Production `appsettings` values separated from development values

### Release Validation

- Database migrations applied successfully to production database
- Backend startup succeeds with production configuration
- Android app points to production API base URL
- Admin web login and approval flows validated
- Mobile authentication, booking, ordering, subscription, and notification flows validated

---

## 5. Release Blockers

These items should be treated as production blockers.

### Blocker 1: Product Stock Concurrency Protection

Current order placement reduces stock in normal application flow without explicit concurrency protection. Under simultaneous requests, overselling remains possible.

**Impact**:

- Inventory inconsistency
- Customer-facing failed fulfillment
- Incorrect order acceptance during high traffic

**Required fix**:

- Wrap order placement in a database transaction and enforce safe stock update semantics
- Prefer atomic update or row-version/concurrency-token based enforcement
- Validate behavior under concurrent requests before release approval

### Blocker 2: Remaining Android Placeholder Flows

Some Android navigation/actions still contain placeholders instead of real release functionality.

**Known examples**:

- Add listing placeholder in bookings navigation
- Products placeholder in bookings navigation
- Equipment edit action placeholder
- Equipment delete action placeholder

**Required fix**:

- Replace all user-visible placeholders with real navigation or remove the entry points from production UI

### Blocker 3: Missing Backend Automated Test Suite

There is no real backend C# test project currently present in the repository.

**Required minimum before release**:

- Integration tests for auth, subscription verification, booking creation, booking approval/rejection, and order placement
- Regression checks for the API contracts used by Android

### Blocker 4: Production Secrets Handling

The project currently depends on local configuration and credential files for several integrations.

**Required fix**:

- Move production secrets to a secure secret-management approach such as environment variables, deployment secrets, or managed secret storage
- Confirm no secret-bearing files are shipped in the release artifact

---

## 6. High-Priority Hardening Items

These items are not necessarily release blockers in every deployment model, but they should be completed as part of production hardening.

### Password Hashing Upgrade

Current password hashing should be upgraded to BCrypt or Argon2 for long-term production security.

### Endpoint Abuse Protection

Rate limiting middleware is present, but release approval should confirm policy coverage for:

- OTP endpoints
- Login endpoints
- Subscription/payment endpoints
- Search endpoints if public traffic is expected

### Observability

Production should include:

- Structured logs for auth, payments, bookings, and orders
- Error monitoring
- Startup diagnostics
- Request correlation where possible

### Backup and Rollback Readiness

Production approval should confirm:

- Database backup before schema deployment
- Rollback procedure for API deployment
- Rollback procedure for Android release if a bad client build ships

---

## 7. Release Acceptance Checklist

Use this checklist as the final sign-off gate.

### Backend

- [ ] Production configuration reviewed
- [ ] Secrets removed from deployable files
- [ ] Database migrations tested on staging
- [ ] Concurrency-safe product ordering implemented
- [ ] Health checks return success after deployment
- [ ] Logs verified after startup
- [ ] Payment verification validated end to end

### Admin Web

- [ ] Admin login works in production
- [ ] Equipment approval flow works
- [ ] Product approval flow works
- [ ] Subscription plan management works
- [ ] Dashboard statistics load correctly

### Android App

- [ ] Login and refresh token flow verified
- [ ] Language/theme/session persistence verified
- [ ] Search opens correct detail screens
- [ ] Equipment booking flow verified
- [ ] Booking list and detail display verified
- [ ] Seller add-product flow verified
- [ ] Product management screen verified
- [ ] Subscription screens and slot counters verified
- [ ] All placeholders removed from release UI
- [ ] Push notifications verified on real device

### Security and Operations

- [ ] HTTPS verified with production certificate
- [ ] Rate-limit policy coverage verified
- [ ] Backup taken before deployment
- [ ] Rollback steps documented
- [ ] Monitoring and alerting enabled

---

## 8. Recommended Release Process

### Stage 1: Staging Validation

1. Deploy backend to staging
2. Apply latest migrations to staging database
3. Configure staging secrets and Firebase credentials
4. Test admin web workflows
5. Test Android app against staging backend
6. Run smoke tests for booking, ordering, and subscription flows

### Stage 2: Production Readiness Review

1. Confirm all blockers in Section 5 are closed
2. Confirm all acceptance checklist items required for launch are complete
3. Freeze API contract for the release build
4. Tag release candidate in source control

### Stage 3: Production Deployment

1. Take production database backup
2. Deploy backend release artifact
3. Run migration if required
4. Validate health endpoint and startup logs
5. Validate payment, booking, and order smoke flows
6. Publish Android production build

### Stage 4: Post-Release Monitoring

1. Monitor authentication failures
2. Monitor order placement and stock behavior
3. Monitor payment verification failures
4. Monitor booking approval/rejection flows
5. Monitor notification delivery and app crashes

---

## 9. Minimum Smoke Test Pack

The following tests should be executed immediately after production deployment.

### Authentication

- Register or log in with OTP
- Refresh token path works
- Role-based UI and API access behaves correctly

### Subscription

- Create Razorpay order
- Complete payment
- Verify subscription activation
- Confirm updated access/roles if applicable

### Equipment Flow

- Owner adds equipment
- Admin approves equipment
- Farmer browses and books equipment
- Owner approves or rejects booking

### Product Flow

- Seller adds product
- Admin approves product
- Farmer places order
- Seller sees order and status updates
- Stock changes correctly after order and cancellation

### Notifications

- Booking notification received
- Subscription-related notification routing works

---

## 10. Release Decision

### Current Decision

**Do not mark AgriRent as fully production-released yet.**

### Current Best Classification

**Production candidate / release candidate**

### Conditions to Approve Production Release

Approve production release only after:

- product stock concurrency protection is implemented
- Android placeholder flows are removed or completed
- backend automated tests are added for critical flows
- production secret handling is finalized
- staging and production smoke tests pass

Once those items are complete, this document can be revised to:

**Release Status: Production Approved**

---

## 11. Document Ownership

This document should be updated whenever one of the following changes:

- payment flow logic
- auth contract or token flow
- Android release-critical user journeys
- database migration strategy
- deployment process or hosting model
- release blockers or sign-off criteria
