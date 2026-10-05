package com.example.email.service;

import com.example.email.dto.EmailRequest;
import com.example.email.dto.SendEmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name:Email Service}")
    private String fromName;

    /**
     * Send email with one or more file attachments.
     *
     * @param request Recipient, subject, body, CC, BCC details
     * @param files   Array of MultipartFile attachments
     * @return List of attached file names
     */
    public List<String> sendEmailWithAttachments(EmailRequest request, MultipartFile[] files)
            throws MessagingException, IOException {

        log.info("Preparing to send email with attachment(s) to: {}", request.getTo());

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        // Pass 'true' to indicate a multipart message (supports attachments & inline elements)
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

        // Configure sender
        try {
            if (fromName != null && !fromName.isBlank()) {
                helper.setFrom(fromAddress, fromName);
            } else {
                helper.setFrom(fromAddress);
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not set sender personal name, defaulting to raw address: {}", e.getMessage());
            helper.setFrom(fromAddress);
        }

        // Configure recipient
        helper.setTo(request.getTo());

        // Configure CC recipients if provided
        if (request.getCc() != null && !request.getCc().isBlank()) {
            String[] ccs = Arrays.stream(request.getCc().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (ccs.length > 0) {
                helper.setCc(ccs);
            }
        }

        // Configure BCC recipients if provided
        if (request.getBcc() != null && !request.getBcc().isBlank()) {
            String[] bccs = Arrays.stream(request.getBcc().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (bccs.length > 0) {
                helper.setBcc(bccs);
            }
        }

        // Configure subject and content
        helper.setSubject(request.getSubject());
        helper.setText(request.getBody(), request.isHtml());

        // Attach files
        List<String> attachedFileNames = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String filename = file.getOriginalFilename();
                    if (filename == null || filename.isBlank()) {
                        filename = "attachment_" + (attachedFileNames.size() + 1);
                    }

                    // Wrap bytes in ByteArrayResource to guarantee safe file reading
                    byte[] fileBytes = file.getBytes();
                    String finalFilename = filename;
                    ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                        @Override
                        public String getFilename() {
                            return finalFilename;
                        }
                    };

                    String contentType = file.getContentType();
                    if (contentType != null && !contentType.isBlank()) {
                        helper.addAttachment(finalFilename, resource, contentType);
                    } else {
                        helper.addAttachment(finalFilename, resource);
                    }

                    attachedFileNames.add(filename);
                    log.info("Attached file: {} ({} bytes)", filename, file.getSize());
                }
            }
        }

        // Transmit email
        mailSender.send(mimeMessage);
        log.info("Email successfully sent to: {} with {} attachment(s)", request.getTo(), attachedFileNames.size());

        return attachedFileNames;
    }

    /**
     * Send simple email without attachments.
     *
     * @param request Recipient, subject, and body details
     */
    public void sendSimpleEmail(SendEmailRequest request) throws MessagingException {
        EmailRequest attachmentRequest = EmailRequest.builder()
                .to(request.getTo())
                .subject(request.getSubject())
                .body(request.getBody())
                .html(request.isHtml())
                .build();

        try {
            sendEmailWithAttachments(attachmentRequest, null);
        } catch (IOException e) {
            throw new MessagingException("Failed to send simple email", e);
        }
    }
}
