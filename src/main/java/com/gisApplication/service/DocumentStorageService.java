package com.gisApplication.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.gisApplication.exception.DocumentStorageException;
import com.gisApplication.model.DocumentStorageProperties;
import com.gisApplication.repository.DocumentStoragePropertiesRepo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
@Service
public class DocumentStorageService {
    private final Path fileStorageLocation;
    @Autowired
    DocumentStoragePropertiesRepo docStorageRepo;
    @Autowired
    public DocumentStorageService(DocumentStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new DocumentStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    public String storeFile(MultipartFile file, Integer userId, String docType) {
        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = "";
        try {
            // Check if the file's name contains invalid characters
            if(originalFileName.contains("..")) {
                throw new DocumentStorageException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }
            String fileExtension = "";
            try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            } catch(Exception e) {
                fileExtension = "";
            }
            fileName = userId + "_" + docType + fileExtension;
         // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            DocumentStorageProperties doc = docStorageRepo.checkDocumentByUserId(userId, docType);
            if(doc != null) {
                doc.setDocumentFormat(file.getContentType());
                doc.setFileName(fileName);
                docStorageRepo.save(doc);
            } else {
                DocumentStorageProperties newDoc = new DocumentStorageProperties();
                newDoc.setUserId(userId);
                newDoc.setDocumentFormat(file.getContentType());
                newDoc.setFileName(fileName);
                newDoc.setDocumentType(docType);
                docStorageRepo.save(newDoc);
            }
            return fileName;
        } catch (IOException ex) {
            throw new DocumentStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }
    public Resource loadFileAsResource(String fileName) throws Exception {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("File not found " + fileName);
        }
    }
    public String getDocumentName(Integer userId, String docType) {
        return docStorageRepo.getUploadDocumnetPath(userId, docType);
    }
}