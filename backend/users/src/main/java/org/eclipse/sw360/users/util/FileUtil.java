/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.users.UserHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {
    private static final String EXTENSION = ".log";
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long dayMTimeMillis = 24 * 60 * 60 * 1000;
    private static final Logger log = LogManager.getLogger(FileUtil.class);
    private FileUtil() {
    }

    public static void writeLogToFile(String title, String message, String status, String folder) {
        BufferedWriter writer = null;
        FileWriter fileWriter = null;
        try {
            String contentLog = LocalDateTime.now().format(format) + " " + title + " " + message + " " + status;
            String path = folder + LocalDate.now() + EXTENSION;
            File file = new File(path);
            if (file.exists()) {
                fileWriter = new FileWriter(file, true);
                writer = new BufferedWriter(fileWriter);
                writer.append(contentLog);
            } else {
                File files = new File(path);
                if (files.getParentFile() != null) file.getParentFile().mkdirs();
                fileWriter = new FileWriter(files);
                writer = new BufferedWriter(fileWriter);
                writer.write(contentLog);
            }
            writer.newLine();
        } catch (IOException e) {
            log.error("Write log to file failed!", e.getMessage());
        } finally {
            try {
                if (writer != null) writer.close();
                if (fileWriter != null) fileWriter.close();
            } catch (IOException e) {
                log.error("Close file failed!" ,e.getMessage());
            }
        }
    }

    public static File getFileLastModified(String directoryFilePath) {
        File directory = new File(directoryFilePath);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }
        return chosenFile;
    }

    public static List<String> readFileLog(String filePath) throws SW360Exception {
        List<String> contentLog = new ArrayList<>();
        Path path = Paths.get(filePath);
        try {
            contentLog = Files.readAllLines(path);
        } catch (IOException e) {
            log.error("Read file log failed!" ,e.getMessage());
            throw new SW360Exception("Read file log failed!");
        }
        return contentLog;
    }

    public static Set<String> listFileNames(String dir) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), 1)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static Set<String> listPathFiles(String dir) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), 1)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static Set<String> getListFilesOlderThanNDays(int days, String dirPath) throws IOException {
        long cutOff;
        if (days != 0) cutOff = System.currentTimeMillis() - (days * dayMTimeMillis);
        else cutOff = days;
        try (Stream<Path> stream = Files.list(Paths.get(dirPath))) {
            return stream.filter(path -> {
                        try {
                            return Files.isRegularFile(path) && Files.getLastModifiedTime(path).to(TimeUnit.MILLISECONDS) >= cutOff;
                        } catch (IOException ex) {
                            return false;
                        }
                    }).map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }

    }

}
