package Jutjubic.RA56.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.util.StringUtils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

@Service
public class FileStorageService {

    private final Path videoStorageLocation;
    private final Path thumbnailStorageLocation;

    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024; // 200MB

    public FileStorageService() {
        this.videoStorageLocation = Paths.get("storage/videos").toAbsolutePath().normalize();
        this.thumbnailStorageLocation = Paths.get("storage/thumbnails").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.videoStorageLocation);
            Files.createDirectories(this.thumbnailStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeVideo(MultipartFile file) {
        if (!"video/mp4".equals(file.getContentType())) {
            throw new RuntimeException("Failed to store file: Invalid video format. Only MP4 is allowed.");
        }

        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new RuntimeException("Failed to store file: Video size exceeds the limit of 200MB.");
        }
        
        return storeFile(file, this.videoStorageLocation);
    }

    public String storeThumbnail(MultipartFile file) {
         if (file.getContentType() == null || !file.getContentType().startsWith("image")) {
            throw new RuntimeException("Failed to store file: Invalid thumbnail format. Only image files are allowed.");
        }

        return storeFile(file, this.thumbnailStorageLocation);
    }

    private String storeFile(MultipartFile file, Path location) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch (Exception e) {
            // ignore
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = location.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public void deleteFile(String fileName, boolean isVideo) {
        try {
            Path fileLocation = isVideo ? this.videoStorageLocation : this.thumbnailStorageLocation;
            Path filePath = fileLocation.resolve(fileName).normalize();
            if(Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException ex) {
            System.err.println("Could not delete file: " + fileName);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.thumbnailStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    public Resource loadVideoAsResource(String fileName) {
        try {
            Path filePath = this.videoStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    public Path resolveVideoPath(String fileName) {
        Path filePath = this.videoStorageLocation.resolve(fileName).normalize();
        if (!filePath.startsWith(this.videoStorageLocation)) {
            throw new IllegalArgumentException("Invalid video path");
        }
        return filePath;
    }
}
