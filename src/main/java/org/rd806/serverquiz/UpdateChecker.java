package org.rd806.serverquiz;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class UpdateChecker {

    private static final String PROJECT_ID = "serverquiz";

    // 检查版本更新
    public static void checkUpdate() {
        boolean enable = ServerQuiz.config.getBoolean("updateChecker", true);
        String source = ServerQuiz.config.getString("updateChecker.source", "modrinth").toLowerCase();

        if (!enable) return;

        ServerQuiz.main.getServer().getScheduler().runTaskAsynchronously(ServerQuiz.main, () -> {
            String latestVersion = null;
            switch (source) {
                case "modrinth" -> {
                    ServerQuiz.logger.info("\u001B[33mFetching latest version from Modrinth ...\u001B[0m");
                    latestVersion = getVersionFromModrinth();
                }
                case "github" -> {
                    ServerQuiz.logger.info("\u001B[33mFetching latest version from Github ...\u001B[0m");
                    latestVersion = getVersionFromGitHub();
                }
            }

            String currentVersion = ServerQuiz.main.getDescription().getVersion();

            if (latestVersion == null) {
                ServerQuiz.logger.warning("No latest version found, please check your Internet connection!");
                return;
            }

            if (!latestVersion.equals(currentVersion)) {
                ServerQuiz.logger.info("\u001B[33m*** Newer version available! ***\u001B[0m");
                ServerQuiz.logger.info("\u001B[33m*** Latest version: " + latestVersion + " ***\u001B[0m");
                ServerQuiz.logger.info("\u001B[33m*** Current version: " + currentVersion + " ***\u001B[0m");
            } else {
                ServerQuiz.logger.info("\u001B[33m*** ServerQuiz is already up-to-date! ***\u001B[0m");
            }
        });
    }

    // 从Modrinth获取版本
    private static String getVersionFromModrinth() {
        String url = "https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version";

        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            InputStream inputStream = connection.getInputStream();
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String json = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            return extractVersion(json);

        } catch (Exception e) {
            ServerQuiz.logger.warning(e.getMessage());
            return null;
        }
    }

    // 从Github获取版本
    private static String getVersionFromGitHub() {
        String url = "https://api.github.com/repos/rd806/ServerQuiz/releases/latest";

        try {
            InputStream inputStream = new URL(url).openStream();
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String json = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            return json.split("\"tag_name\":\"")[1].split("\"")[0];
        } catch (IOException e) {
            ServerQuiz.logger.warning(e.getMessage());
            return null;
        }
    }

    // 提取版本信息
    private static String extractVersion(String json) {
        String key = "\"version_number\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
