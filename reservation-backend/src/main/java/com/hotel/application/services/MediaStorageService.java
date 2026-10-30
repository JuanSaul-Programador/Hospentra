package com.hotel.application.services;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    String uploadFile(MultipartFile file, String subDirectory);

    void deleteFile(String fileUrl);
}
