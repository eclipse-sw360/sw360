/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.attachments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility service for computing various types of checksums on attachment files.
 * Supports MD5, SHA-1, and SHA-256 algorithms.
 */
public class ChecksumService {
    
    private static final Logger log = LogManager.getLogger(ChecksumService.class);
    
    private static final int BUFFER_SIZE = 8192;
    
    public enum ChecksumType {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256");
        
        private final String algorithmName;
        
        ChecksumType(String algorithmName) {
            this.algorithmName = algorithmName;
        }
        
        public String getAlgorithmName() {
            return algorithmName;
        }
    }
    
    /**
     * Compute all supported checksums for the given input stream.
     * 
     * @param inputStream The input stream to compute checksums for
     * @return Map containing all computed checksums
     * @throws SW360Exception if checksum computation fails
     */
    public Map<ChecksumType, String> computeAllChecksums(InputStream inputStream) throws SW360Exception {
        Map<ChecksumType, String> checksums = new HashMap<>();
        
        try {
            MessageDigest md5Digest = MessageDigest.getInstance(ChecksumType.MD5.getAlgorithmName());
            MessageDigest sha1Digest = MessageDigest.getInstance(ChecksumType.SHA1.getAlgorithmName());
            MessageDigest sha256Digest = MessageDigest.getInstance(ChecksumType.SHA256.getAlgorithmName());
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md5Digest.update(buffer, 0, bytesRead);
                sha1Digest.update(buffer, 0, bytesRead);
                sha256Digest.update(buffer, 0, bytesRead);
            }
            
            checksums.put(ChecksumType.MD5, bytesToHex(md5Digest.digest()));
            checksums.put(ChecksumType.SHA1, bytesToHex(sha1Digest.digest()));
            checksums.put(ChecksumType.SHA256, bytesToHex(sha256Digest.digest()));
            
            log.debug("Computed checksums - MD5: {}, SHA1: {}, SHA256: {}", 
                checksums.get(ChecksumType.MD5), 
                checksums.get(ChecksumType.SHA1), 
                checksums.get(ChecksumType.SHA256));
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Required digest algorithm not available", e);
            throw new SW360Exception("Checksum computation failed: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to read input stream for checksum computation", e);
            throw new SW360Exception("Failed to read file for checksum computation: " + e.getMessage());
        }
        
        return checksums;
    }
    
    /**
     * Compute a specific checksum for the given input stream.
     * 
     * @param inputStream The input stream to compute checksum for
     * @param checksumType The type of checksum to compute
     * @return The computed checksum as hex string
     * @throws SW360Exception if checksum computation fails
     */
    public String computeChecksum(InputStream inputStream, ChecksumType checksumType) throws SW360Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance(checksumType.getAlgorithmName());
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            String checksum = bytesToHex(digest.digest());
            log.debug("Computed {} checksum: {}", checksumType.name(), checksum);
            
            return checksum;
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Required digest algorithm not available: {}", checksumType.getAlgorithmName(), e);
            throw new SW360Exception("Checksum computation failed: " + e.getMessage()); 
        } catch (IOException e) {
            log.error("Failed to read input stream for checksum computation", e);
            throw new SW360Exception("Failed to read file for checksum computation: " + e.getMessage());
        }
    }
    
    /**
     * Validate a checksum against expected value.
     * 
     * @param actualChecksum The computed checksum
     * @param expectedChecksum The expected checksum
     * @param checksumType The type of checksum being validated
     * @return true if checksums match, false otherwise
     */
    public boolean validateChecksum(String actualChecksum, String expectedChecksum, ChecksumType checksumType) {
        if (actualChecksum == null || expectedChecksum == null) {
            log.warn("Cannot validate checksum - one of the values is null");
            return false;
        }
        
        boolean isValid = actualChecksum.equalsIgnoreCase(expectedChecksum);
        
        if (!isValid) {
            log.warn("Checksum validation failed for {} - Expected: {}, Actual: {}", 
                checksumType.name(), expectedChecksum, actualChecksum);
        } else {
            log.debug("Checksum validation successful for {}: {}", checksumType.name(), actualChecksum);
        }
        
        return isValid;
    }
    
    /**
     * Convert byte array to hexadecimal string.
     * 
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Check if a checksum string is valid (non-null, non-empty).
     * 
     * @param checksum The checksum string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidChecksum(String checksum) {
        return checksum != null && !checksum.trim().isEmpty() && checksum.matches("^[a-fA-F0-9]+$");
    }
}