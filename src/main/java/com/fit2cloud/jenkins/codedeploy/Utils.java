package com.fit2cloud.jenkins.codedeploy;

import com.fit2cloud.sdk.model.ApplicationDeployPolicyType;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhangbohan on 15/9/25.
 */
public class Utils {
    public static final String FWD_SLASH = "/";

    public static boolean isNullOrEmpty(final String name) {
        boolean isValid = false;
        if (name == null || name.matches("\\s*")) {
            isValid = true;
        }
        return isValid;
    }

    public static boolean isNumber(final String name) {
        boolean isNumber = false;
        if (name == null || name.matches("[0-9]+")) {
            isNumber = true;
        }
        return isNumber;
    }

    public static String replaceTokens(AbstractBuild<?, ?> build,
                                       BuildListener listener, String text) throws IOException,
            InterruptedException {
        String newText = null;
        if (!isNullOrEmpty(text)) {
            Map<String, String> envVars = build.getEnvironment(listener);
            newText = Util.replaceMacro(text, envVars);
        }
        return newText;
    }

    public static List<Map<String,String>> getRepoList(){
        List<Map<String,String>> repoList = new ArrayList<Map<String,String>>();
        Map repo = new HashMap();
        repo.put("label","OSS仓库");
        repo.put("value","oss");
        Map repo1 = new HashMap();
        repo1.put("label","NEXUS仓库");
        repo1.put("value","nexus");
        Map repo2 = new HashMap();
        repo2.put("label","无仓库");
        repo2.put("value","null");
        repoList.add(repo);
        repoList.add(repo1);
        repoList.add(repo2);
        return repoList;
    }

    public static List<Map<String,String>> getStrategyList(){
        List<Map<String,String>> repoList = new ArrayList<Map<String,String>>();
        Map repo = new HashMap();
        repo.put("label","全部同时部署");
        repo.put("value", ApplicationDeployPolicyType.ALL_AT_ONCE);
        Map repo1 = new HashMap();
        repo1.put("label","半数分批部署");
        repo1.put("value",ApplicationDeployPolicyType.HALF_AT_A_TIME);
        Map repo2 = new HashMap();
        repo2.put("label","单台依次部署");
        repo2.put("value",ApplicationDeployPolicyType.ONE_AT_A_TIME);
        repoList.add(repo);
        repoList.add(repo1);
        repoList.add(repo2);
        return repoList;
    }

}
