package kr.shkworld.shktown.core.logging.service.impl;

import com.google.gson.Gson;
import kr.shkworld.shktown.core.logging.model.LogType;
import kr.shkworld.shktown.core.logging.repository.LogRepository;
import kr.shkworld.shktown.core.logging.service.LogService;
import kr.shkworld.shktown.util.PluginLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogServiceImpl implements LogService {
    private final LogRepository logRepository;
    private final PluginLogger pluginLogger;
    private final Gson gson = new Gson();

    private final ExecutorService fileExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "shktown-file-log-thread");
        thread.setDaemon(true);
        return thread;
    });

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final File logFolder;

    public LogServiceImpl(File dataFolder, LogRepository logRepository, PluginLogger pluginLogger) {
        this.logRepository = logRepository;
        this.pluginLogger = pluginLogger;
        this.logFolder = new File(dataFolder, "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
    }

    @Override
    public CompletableFuture<Void> logSystem(LogType logType, String message, Map<String, Object> data) {
        String dataJson = (data != null) ? gson.toJson(data) : "{}";

        CompletableFuture<Void> fileFuture = CompletableFuture.runAsync(() -> {
            String timePrefix = LocalDateTime.now().format(formatter);
            String logLine = String.format("[%s] [%s] %s | Data: %s",
                timePrefix, logType.getDescription(), message, dataJson);
            writeLogToFile("system.log", logLine);

            if (logType == LogType.ERROR) {
                pluginLogger.severe(message + " | Data: " + dataJson);
            } else {
                pluginLogger.info(message);
            }
        }, fileExecutor);

        CompletableFuture<Void> dbFuture = logRepository.saveSystemLog(
            logType, message, dataJson
        );

        return CompletableFuture.allOf(fileFuture, dbFuture)
                .exceptionally(throwable -> {
                    pluginLogger.severe("시스템 로그 연산 중 예외 발생: " + throwable.getMessage());
                    return null;
                });
    }

    private void writeLogToFile(String baseFileName, String logLine) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String rotatedFileName = baseFileName.replace(".log", "-" + currentDate + ".log");
        File file = new File(logFolder, rotatedFileName);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            writer.println(logLine);
        } catch (IOException e) {
            pluginLogger.severe("로컬 로그 파일 기록 실패 (" + rotatedFileName + "): " + e.getMessage());
        }
    }

    public void shutdownFileExecutor() {
        fileExecutor.shutdown();
    }
}
