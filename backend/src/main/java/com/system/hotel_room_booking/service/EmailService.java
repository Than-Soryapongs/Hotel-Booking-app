package com.system.hotel_room_booking.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;
    
    @Value("${sendgrid.from-email}")
    private String fromEmail;
    
    @Value("${sendgrid.from-name}")
    private String fromName;
    
    @Value("${app.email.verification.base-url}")
    private String baseUrl;
    
    public void sendVerificationEmail(String toEmail, String username, String verificationToken) {
        try {
            String verificationLink = baseUrl + "/verify-email?token=" + verificationToken;
            
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            String subject = "Verify Your Email Address";
            
            String htmlContent = buildVerificationEmailHtml(username, verificationLink);
            Content content = new Content("text/html", htmlContent);
            
            Mail mail = new Mail(from, subject, to, content);
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Verification email sent successfully to: {}", toEmail);
            } else {
                log.error("Failed to send verification email. Status code: {}", response.getStatusCode());
                throw new RuntimeException("Failed to send verification email");
            }
            
        } catch (IOException e) {
            log.error("Error sending verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        try {
            String resetLink = baseUrl + "/reset-password?token=" + resetToken;
            
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            String subject = "Reset Your Password";
            
            String htmlContent = buildPasswordResetEmailHtml(username, resetLink);
            Content content = new Content("text/html", htmlContent);
            
            Mail mail = new Mail(from, subject, to, content);
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Password reset email sent successfully to: {}", toEmail);
            } else {
                log.error("Failed to send password reset email. Status code: {}", response.getStatusCode());
                throw new RuntimeException("Failed to send password reset email");
            }
            
        } catch (IOException e) {
            log.error("Error sending password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            String subject = "Welcome to " + fromName;
            
            String htmlContent = buildWelcomeEmailHtml(username);
            Content content = new Content("text/html", htmlContent);
            
            Mail mail = new Mail(from, subject, to, content);
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Welcome email sent successfully to: {}", toEmail);
            } else {
                log.error("Failed to send welcome email. Status code: {}", response.getStatusCode());
            }
            
        } catch (IOException e) {
            log.error("Error sending welcome email to: {}", toEmail, e);
        }
    }
    
    private String buildVerificationEmailHtml(String username, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #4CAF50; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Email Verification</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>Thank you for registering with us. Please verify your email address to activate your account.</p>
                        <p>Click the button below to verify your email:</p>
                        <a href="%s" class="button">Verify Email Address</a>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #4CAF50;">%s</p>
                        <p><strong>This link will expire in 24 hours.</strong></p>
                        <p>If you didn't create an account, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, verificationLink, verificationLink, fromName);
    }
    
    private String buildPasswordResetEmailHtml(String username, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF5722; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #FF5722; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>We received a request to reset your password. Click the button below to reset it:</p>
                        <a href="%s" class="button">Reset Password</a>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #FF5722;">%s</p>
                        <p><strong>This link will expire in 1 hour.</strong></p>
                        <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, resetLink, resetLink, fromName);
    }
    
    private String buildWelcomeEmailHtml(String username) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to %s!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>Your email has been successfully verified and your account is now active.</p>
                        <p>You can now access all features of your account.</p>
                        <p>If you have any questions, feel free to contact our support team.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fromName, username, fromName);
    }
    
    public void sendEmailChangeVerificationEmail(String newEmail, String username, String oldEmail, String verificationToken) {
        try {
            String verificationLink = baseUrl + "/verify-email-change?token=" + verificationToken;
            
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(newEmail);
            String subject = "Verify Your New Email Address";
            
            String htmlContent = buildEmailChangeVerificationHtml(username, oldEmail, newEmail, verificationLink);
            Content content = new Content("text/html", htmlContent);
            
            Mail mail = new Mail(from, subject, to, content);
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email change verification sent successfully to: {}", newEmail);
            } else {
                log.error("Failed to send email change verification. Status code: {}", response.getStatusCode());
                throw new RuntimeException("Failed to send email change verification");
            }
            
        } catch (IOException e) {
            log.error("Error sending email change verification to: {}", newEmail, e);
            throw new RuntimeException("Failed to send email change verification", e);
        }
    }
    
    public void sendEmailChangeConfirmation(String oldEmail, String username, String newEmail) {
        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(oldEmail);
            String subject = "Your Email Address Has Been Changed";
            
            String htmlContent = buildEmailChangeConfirmationHtml(username, oldEmail, newEmail);
            Content content = new Content("text/html", htmlContent);
            
            Mail mail = new Mail(from, subject, to, content);
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email change confirmation sent successfully to: {}", oldEmail);
            } else {
                log.error("Failed to send email change confirmation. Status code: {}", response.getStatusCode());
            }
            
        } catch (IOException e) {
            log.error("Error sending email change confirmation to: {}", oldEmail, e);
        }
    }
    
    private String buildEmailChangeVerificationHtml(String username, String oldEmail, String newEmail, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #FF9800; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .info-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Email Change Verification</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>You have requested to change your email address.</p>
                        <div class="info-box">
                            <strong>Current Email:</strong> %s<br>
                            <strong>New Email:</strong> %s
                        </div>
                        <p>To complete the email change, please verify your new email address by clicking the button below:</p>
                        <a href="%s" class="button">Verify New Email</a>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #FF9800;">%s</p>
                        <p><strong>This link will expire in 24 hours.</strong></p>
                        <p>If you didn't request this change, please ignore this email and your email address will remain unchanged.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, oldEmail, newEmail, verificationLink, verificationLink, fromName);
    }
    
    private String buildEmailChangeConfirmationHtml(String username, String oldEmail, String newEmail) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .alert-box { background-color: #d4edda; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Email Address Changed</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>This is to confirm that your email address has been successfully changed.</p>
                        <div class="alert-box">
                            <strong>Previous Email:</strong> %s<br>
                            <strong>New Email:</strong> %s
                        </div>
                        <p>All future communications will be sent to your new email address.</p>
                        <p>If you didn't make this change, please contact our support team immediately.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, oldEmail, newEmail, fromName);
    }
}

