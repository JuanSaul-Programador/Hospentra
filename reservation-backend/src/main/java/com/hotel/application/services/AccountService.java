package com.hotel.application.services;

import com.hotel.web.dto.*;

public interface AccountService {

    UserProfileResponse getProfile(String username);

    UserProfileResponse updateProfile(String username, UpdateProfileRequest request);

    void requestPasswordChangeOtp(String username);

    void changePassword(String username, ChangePasswordRequest request);

    AccountDeletionStatusResponse requestAccountDeletion(String username, RequestAccountDeletionRequest request);

    AccountDeletionStatusResponse cancelAccountDeletion(String username);
}
