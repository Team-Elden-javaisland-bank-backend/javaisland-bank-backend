package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.common.SecurityUtil;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile-picture")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('C')")
public class ProfilePictureController {

    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads/profile-pictures}")
    private String uploadDir;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Map<String, String> TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        if (file.isEmpty()) {
            throw new ApiBankException("File vuoto.", "EMPTY_FILE");
        }

        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ApiBankException("Tipo non supportato. Usa JPG, PNG, GIF o WebP.", "INVALID_FILE_TYPE");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new ApiBankException("File troppo grande. Massimo 5 MB.", "FILE_TOO_LARGE");
        }

        User user = securityUtil.getUser(jwt);

        Path dirPath = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(dirPath);

        String ext = TYPE_TO_EXTENSION.get(contentType);
        String filename = user.getId() + "_" + UUID.randomUUID() + ext;
        Path filePath = dirPath.resolve(filename).normalize();
        if (!filePath.startsWith(dirPath)) {
            throw new ApiBankException("Percorso non valido.", "INVALID_UPLOAD_PATH");
        }

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        if (user.getProfilePictureUrl() != null) {
            deleteOldPicture(user.getProfilePictureUrl());
        }

        String pictureUrl = "/uploads/profile-pictures/" + filename;
        user.setProfilePictureUrl(pictureUrl);
        userRepository.save(user);

        log.info("Profile picture uploaded for user {}: {}", user.getId(), filename);
        return ResponseEntity.ok(pictureUrl);
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@AuthenticationPrincipal Jwt jwt) {
        User user = securityUtil.getUser(jwt);

        if (user.getProfilePictureUrl() != null) {
            deleteOldPicture(user.getProfilePictureUrl());
            user.setProfilePictureUrl(null);
            userRepository.save(user);
            log.info("Profile picture deleted for user {}", user.getId());
        }

        return ResponseEntity.ok().build();
    }

    private void deleteOldPicture(String pictureUrl) {
        try {
            String filename = pictureUrl.substring(pictureUrl.lastIndexOf('/') + 1);
            Path path = Paths.get(uploadDir).toAbsolutePath().resolve(filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete old profile picture: {}", pictureUrl);
        }
    }
}
