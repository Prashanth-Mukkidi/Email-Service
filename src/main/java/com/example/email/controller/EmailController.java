package com.example.email.controller;

import com.example.email.dto.ApiResponse;
import com.example.email.dto.EmailRequest;
import com.example.email.dto.SendEmailRequest;
import com.example.email.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;

    /**
     * Send email with one or more file attachments using multipart/form-data.
     * URL: POST /api/v1/email/send-with-attachment
     */
    @PostMapping(value = "/send-with-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendWithAttachment(
            @Valid @ModelAttribute EmailRequest request,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
            log.info("Request received to send email with attachment to: {}", request.getTo());

            // Merge single file and multi-file inputs seamlessly
            List<MultipartFile> fileList = new ArrayList<>();
            if (files != null) {
                for (MultipartFile f : files) {
                    if (f != null && !f.isEmpty()) {
                        fileList.add(f);
                    }
                }
            }
            if (file != null && !file.isEmpty()) {
                fileList.add(file);
            }

            MultipartFile[] combinedFiles = fileList.toArray(new MultipartFile[0]);
            List<String> attachedFiles = emailService.sendEmailWithAttachments(request, combinedFiles);

            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("recipient", request.getTo());
            responseData.put("subject", request.getSubject());
            responseData.put("attachmentCount", attachedFiles.size());
            responseData.put("attachments", attachedFiles);

            return ResponseEntity.ok(
                    ApiResponse.success("Email sent successfully with attachments", responseData));

        } catch (Exception e) {
            log.error("Failed to send email with attachment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }

    /**
     * Alias endpoint: POST /api/v1/email/send (Supports multipart form data with attachments)
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendEmail(
            @Valid @ModelAttribute EmailRequest request,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return sendWithAttachment(request, files, file);
    }

    /**
     * Send simple JSON email without attachments.
     * URL: POST /api/v1/email/send-simple
     */
    @PostMapping(value = "/send-simple", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> sendSimpleEmail(
            @Valid @RequestBody SendEmailRequest request) {
        try {
            log.info("Request received to send simple email to: {}", request.getTo());
            emailService.sendSimpleEmail(request);
            return ResponseEntity.ok(
                    ApiResponse.success("Email sent successfully", "Delivered to " + request.getTo()));
        } catch (Exception e) {
            log.error("Failed to send simple email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint.
     * URL: GET /api/v1/email/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", "email-service");
        status.put("port", "8086");
        return ResponseEntity.ok(ApiResponse.success("Email service is running", status));
    }
}
