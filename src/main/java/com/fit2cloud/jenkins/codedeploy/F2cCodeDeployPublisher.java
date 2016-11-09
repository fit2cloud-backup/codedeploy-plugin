package com.fit2cloud.jenkins.codedeploy;

import com.fit2cloud.sdk.Fit2CloudClient;
import com.fit2cloud.sdk.Fit2CloudException;
import com.fit2cloud.sdk.model.*;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangbohan on 15/9/22.
 */
public class F2cCodeDeployPublisher extends Publisher {
    private final String f2cApiKey;
    private final String f2cApiSecret;
    private final String f2cRestApiEndpoint;
    private final String repoName;
    private final String downloadAddress;
    private final String appName;
    private final String appVersionName;
    private final String description;
    private final boolean autoDeploy;
    private final String targetCluster;
    private final String targetClusterRole;
    private final String targetVm;
    private final String deployStrategy;
    private final String noticeGroup;

    private PrintStream logger;

    @DataBoundConstructor
    public F2cCodeDeployPublisher(String repoName,
                                  String downloadAddress,
                                  String appName,
                                  String description,
                                  String appVersionName,
                                  Boolean autoDeploy,
                                  String targetCluster,
                                  String targetClusterRole,
                                  String targetVm,
                                  String deployStrategy,
                                  String f2cApiKey,
                                  String f2cApiSecret,
                                  String f2cRestApiEndpoint,
                                  String noticeGroup) {
        this.f2cApiKey = f2cApiKey;
        this.f2cApiSecret = f2cApiSecret;
        this.f2cRestApiEndpoint = f2cRestApiEndpoint;
        this.repoName = repoName;
        this.downloadAddress = downloadAddress;
        this.appName = appName;
        this.description = description;
        this.appVersionName = appVersionName;
        if(autoDeploy !=null && autoDeploy){
            this.autoDeploy = autoDeploy;
            this.targetCluster = targetCluster;
            if(Utils.isNullOrEmpty(targetClusterRole)){
                this.targetClusterRole = null;
            }else{
                this.targetClusterRole = targetClusterRole;
            }
            if(Utils.isNullOrEmpty(targetVm)){
                this.targetVm = null;
            }else{
                this.targetVm = targetVm;
            }
            this.deployStrategy = deployStrategy;
            this.noticeGroup = noticeGroup;
        }else{
            this.autoDeploy = false;
            this.targetCluster = null;
            this.targetClusterRole = null;
            this.targetVm = null;
            this.deployStrategy = null;
            this.noticeGroup = null;
        }

    }


    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        this.logger = listener.getLogger();
        final boolean buildFailed = build.getResult() == Result.FAILURE;
        if (buildFailed) {
            logger.println("Job构建失败,无需注册到FIT2CLOUD.");
            return true;
        }
        String newAddress = Utils.replaceTokens(build, listener, this.downloadAddress);

        String newAppVersionName = Utils.replaceTokens(build, listener, this.appVersionName);
        System.out.println(newAppVersionName);
        if (newAddress != null) {
            newAddress = newAddress.trim();
        }
        logger.println("应用版本地址:"+newAddress);


        if(appName==null){
            logger.println("应用名无效,无法注册到FIT2CLOUD.");
            return false;
        }

        boolean success = false;
        ApplicationRevision applicationRevision = null;

        try {
            logger.println("开始注册新版本到FIT2CLOUD...");
            Fit2CloudClient fit2CloudClient = new Fit2CloudClient(this.f2cApiKey,
                                                                this.f2cApiSecret,
                                                                this.f2cRestApiEndpoint);

            if(repoName==null){
                logger.println("仓库名无效,无法注册到FIT2CLOUD.");
                return false;
            }
            applicationRevision = fit2CloudClient.addApplicationRevision(newAppVersionName,description,appName,repoName,newAddress,null);
            success = true;
            logger.println("注册应用版本成功: 新版本Id是"+applicationRevision.getId());
        }catch (Exception e){
            this.logger.println("注册FIT2CLOUD应用版本失败，错误消息如下:");
            this.logger.println(e.getMessage());
            e.printStackTrace(this.logger);
            success = false;
        }
        if(autoDeploy){
            try {
                logger.println("开始自动部署新注册的应用版本...");
                if(applicationRevision == null){
                    logger.println("版本信息无效,无法执行自动部署.");
                    success = false;
                }else{
                    Fit2CloudClient fit2CloudClient = new Fit2CloudClient(this.f2cApiKey,
                            this.f2cApiSecret,
                            this.f2cRestApiEndpoint);
                    Long targetVmLong = null;
                    if(targetVm != null){
                        targetVmLong = Long.parseLong(targetVm);
                    }
                    Long noticeGroupLong = null;
                    if(noticeGroup != null){
                        noticeGroupLong = Long.parseLong(noticeGroup);
                    }
                    this.logger.println("通知组如下:"+noticeGroupLong);

                    ApplicationDeployment applicationDeployment = fit2CloudClient.addDeployment(applicationRevision.getApplicationName()
                            ,applicationRevision.getName()
                            ,targetCluster
                            ,targetClusterRole
                            ,targetVmLong
                            ,deployStrategy
                            ,"Jenkins触发"
                            ,noticeGroupLong);

                    success = true;


                    logger.println("触发FIT2CLOUD代码部署成功。");
//                    logger.println("具体部署结果请登录FIT2CLOUD控制台查看。");
                    HashMap deploymentStatusMap = new HashMap();
                    deploymentStatusMap.put("pendding", "等待部署");
                    deploymentStatusMap.put("executing", "部署中");
                    deploymentStatusMap.put("successed", "部署成功");
                    deploymentStatusMap.put("failed", "部署失败");
                    deploymentStatusMap.put("canceled", "取消部署");
                    this.logger.println(applicationRevision.getApplicationName()+"的部署状态:");
                    int i = 0;
                    while(true) {
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                        }
                        boolean allFinished = true;
                        success = true;
                        List<ApplicationDeploymentLog> logs = fit2CloudClient.getDeploymentLogs(applicationDeployment.getId());
                        for(ApplicationDeploymentLog log : logs){
                            this.logger.println("主机:"+log.getServerName()+ "->" +deploymentStatusMap.get(log.getStatus()));
                            if(log.getStatus().equals("failed")){
                                success = false;
                            }
                            if(log.getStatus().equals("executing")||log.getStatus().equals("pendding")){
                                allFinished = false;
                            }
                        }
                        if(allFinished){
                            if(success){
                                this.logger.println("部署成功！");
                            }else{
                                this.logger.println("部署失败！");
                            }
                            break;
                        }
                        i++;
                        if(i>90){
                            this.logger.println("部署超时,请查看FIT2CLOUD控制台！");
                            break;
                        }
                    }


                }
            }catch (Exception e){
                this.logger.println("触发FIT2CLOUD代码部署失败，错误消息如下:");
                this.logger.println(e.getMessage());
                e.printStackTrace(this.logger);
                success = false;
            }
        }


        return success;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(F2cCodeDeployPublisher.class);
            load();
        }



        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "FIT2CLOUD代码部署";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindParameters(this);
            save();
            return super.configure(req, formData);
        }

        public FormValidation doCheckAccount(
                @QueryParameter String f2cApiKey,
                @QueryParameter String f2cApiSecret,
                @QueryParameter String f2cRestApiEndpoint) {
            if (StringUtils.isEmpty(f2cApiKey)) {
                return FormValidation.error("FIT2CLOUD ConsumerKey不能为空！");
            }
            if (StringUtils.isEmpty(f2cApiSecret)) {
                return FormValidation.error("FIT2CLOUD SecretKey不能为空！");
            }
            if (StringUtils.isEmpty(f2cRestApiEndpoint)) {
                return FormValidation.error("FIT2CLOUD EndPoint不能为空！");
            }
            try {
                Fit2CloudClient fit2CloudClient = new Fit2CloudClient(f2cApiKey,f2cApiSecret,f2cRestApiEndpoint);
                fit2CloudClient.getClusters();
            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("验证FIT2CLOUD帐号成功！");
        }

        public FormValidation doCheckConfiguration(
                @QueryParameter String f2cApiKey,
                @QueryParameter String f2cApiSecret,
                @QueryParameter String f2cRestApiEndpoint,
                @QueryParameter String repoName,
                @QueryParameter String appName,
                @QueryParameter boolean autoDeploy,
                @QueryParameter String targetCluster,
                @QueryParameter String targetClusterRole,
                @QueryParameter String targetVm
                ) {


            try {
                if (StringUtils.isEmpty(f2cApiKey)) {
                    return FormValidation.error("FIT2CLOUD ConsumerKey不能为空！");
                }
                if (StringUtils.isEmpty(f2cApiSecret)) {
                    return FormValidation.error("FIT2CLOUD SecretKey不能为空！");
                }
                if (StringUtils.isEmpty(f2cRestApiEndpoint)) {
                    return FormValidation.error("FIT2CLOUD EndPoint不能为空！");
                }

                Fit2CloudClient fit2CloudClient = new Fit2CloudClient(f2cApiKey,f2cApiSecret,f2cRestApiEndpoint);
                if (!StringUtils.isEmpty(repoName)) {
                    ApplicationRepo applicationRepo = fit2CloudClient.getApplicationRepo(repoName);
                    if(applicationRepo == null){
                        return FormValidation.error("仓库不存在，请重新设置！");
                    }
                }else{
                    return FormValidation.error("仓库名不能为空！");
                }
                if (!StringUtils.isEmpty(appName)) {
                    Application application = fit2CloudClient.getApplication(appName);
                    if(application == null){
                        return FormValidation.error("应用不存在，请重新设置！");
                    }
                }else {
                    return FormValidation.error("应用名不能为空！");
                }
                if(autoDeploy){
                    if (!StringUtils.isEmpty(targetCluster)) {
                        List<Cluster> clusters = fit2CloudClient.getClusters();
                        Cluster cluster = null;
                        for(Cluster c:clusters){
                            if(c.getName().equals(targetCluster)){
                                cluster = c;
                            }
                        }
                        if(cluster == null){
                            return FormValidation.error("目标集群不存在，请重新设置！");
                        }else{
                            if (!StringUtils.isEmpty(targetClusterRole)) {
                                List<ClusterRole> clusterRoles = fit2CloudClient.getClusterRoles(cluster.getId());
                                ClusterRole clusterRole = null;
                                for(ClusterRole cr:clusterRoles){
                                    if(cr.getName().equals(targetClusterRole)){
                                        clusterRole = cr;
                                    }
                                }

                                if(clusterRole == null){
                                    return FormValidation.error("目标虚机组不存在，请重新设置！");
                                }
                            }
                            if (!StringUtils.isEmpty(targetVm)) {
                                Server server = fit2CloudClient.getServer(Long.parseLong(targetVm));
                                if(server == null){
                                    return FormValidation.error("目标虚机不存在，请重新设置！");
                                }
                            }
                        }

                    }else{
                        return FormValidation.error("目标集群不能为空！");
                    }
                }

            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("验证注册应用版本设置成功！");
        }

        public FormValidation doCheckRepoName(@QueryParameter String val)
                throws IOException, ServletException {
            if (Utils.isNullOrEmpty(val)) {
                return FormValidation.error("仓库名称不能为空！");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckDownloadAddress(@QueryParameter String val)
                throws IOException, ServletException {
            if (Utils.isNullOrEmpty(val)) {
                return FormValidation.error("下载地址不能为空！");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckAppName(@QueryParameter String val)
                throws IOException, ServletException {
            if (Utils.isNullOrEmpty(val)) {
                return FormValidation.error("应用名称不能为空！");
            }

            return FormValidation.ok();
        }
        public FormValidation doCheckAppVersionName(@QueryParameter String val)
                throws IOException, ServletException {
            if (Utils.isNullOrEmpty(val)) {
                return FormValidation.error("应用版本名称不能为空！");
            }

            return FormValidation.ok();
        }


        public FormValidation doCheckTargetCluster(@QueryParameter String val)
                throws IOException, ServletException {
            if (Utils.isNullOrEmpty(val)){
                return FormValidation.error("目标集群不能为空！");
            }
            return FormValidation.ok();
        }



        public ListBoxModel doFillRepoTypeItems() {
            ListBoxModel items = new ListBoxModel();

            List<Map<String,String>> supportRepoList = Utils.getRepoList();
            for(Map<String,String> repoType : supportRepoList){
                items.add(repoType.get("label"),repoType.get("value"));
            }
            return items;
        }

        public ListBoxModel doFillDeployStrategyItems() {
            ListBoxModel items = new ListBoxModel();

            List<Map<String,String>> supportRepoList = Utils.getStrategyList();
            for(Map<String,String> repoType : supportRepoList){
                items.add(repoType.get("label"),repoType.get("value"));
            }
            return items;
        }


    }

    public String getF2cApiKey() {
        return f2cApiKey;
    }

    public String getF2cApiSecret() {
        return f2cApiSecret;
    }

    public String getF2cRestApiEndpoint() {
        return f2cRestApiEndpoint;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getDownloadAddress() {
        return downloadAddress;
    }

    public String getAppName() {
        return appName;
    }

    public String getDescription() {
        return description;
    }

    public String getAppVersionName() {
        return appVersionName;
    }

    public String getTargetVm() {
        return targetVm;
    }

    public boolean isAutoDeploy() {
        return autoDeploy;
    }

    public String getTargetCluster() {
        return targetCluster;
    }

    public String getTargetClusterRole() {
        return targetClusterRole;
    }

    public String getDeployStrategy() {
        return deployStrategy;
    }

    public String getNoticeGroup() {
        return noticeGroup;
    }
}
