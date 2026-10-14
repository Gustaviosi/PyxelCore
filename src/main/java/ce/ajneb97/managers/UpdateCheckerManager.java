package ce.ajneb97.managers;

import ce.ajneb97.model.internal.UpdateCheckerResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateCheckerManager {

    private String version;
    private String latestVersion;
    private static final String API = "https://api.github.com/repos/Gustavo-bt/PyxelCore/releases/latest";

    public UpdateCheckerManager(String version){
        this.version = version;
    }

    public UpdateCheckerResult check(){
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(API).openConnection();
            con.setConnectTimeout(1500);
            con.setReadTimeout(1500);
            con.setRequestProperty("User-Agent","PyxelCore-UpdateChecker");
            con.setRequestProperty("Accept","application/vnd.github+json");
            StringBuilder body = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String l; while((l = br.readLine()) != null) body.append(l);
            }
            Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(body.toString());
            latestVersion = m.find() ? m.group(1).replaceAll("^v","") : null;
            if(latestVersion != null && !version.equals(latestVersion)){
                return UpdateCheckerResult.noErrors(latestVersion);
            }
            return UpdateCheckerResult.noErrors(null);
        } catch (Exception ex) {
            return UpdateCheckerResult.error();
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}