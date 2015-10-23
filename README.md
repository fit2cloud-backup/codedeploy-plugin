FIT2CLOUD CodeDeploy-Plugin for Jenkins
====================

Jenkins是当前最常用的CI服务器，FIT2CLOUD CodeDeploy-Plugin for Jenkins的功能是：将构建后并自动上传到指定仓库的应用版本注册到FIT2CLOUD,并通过FIT2CLOUD实现自动代码部署，打通从代码提交到部署的阻隔。

一、安装说明
-------------------------

插件下载地址：http://repository-proxy.fit2cloud.com:8080/content/repositories/releases/org/jenkins-ci/plugins/f2c-codedeploy/0.1/f2c-codedeploy-0.1.hpi
在Jenkins中安装插件, 请到 Manage Jenkins | Advanced | Upload，上传插件(.hpi文件)
安装完毕后请重新启动Jenkins


二、Post-build actions: FIT2CLOUD代码部署
-------------------------


#####FIT2CLOUD账号设置:   
1. FIT2CLOUD的Consumer Key   
2. FIT2CLOUD的Secret Key   
3. FIT2CLOUD的API EndPoint
所需的信息可以在FIT2LCOUD控制台用户的API信息中查看。
   
#####注册应用版本配置信息:   
1. 仓库名称 (FIT2CLOUD控制台中 代码部署 > 仓库管理 查看列表中对应的仓库名称。)    
2. 应用名称 (FIT2CLOUD控制台中 代码部署 > 应用管理 中的应用名称。)   
3. 应用版本名称 (将会注册到FIT2CLOUD中的应用版本的名称，应用版本名称需要动态变化，以防出现版本名重复的问题。我们推荐的应用版本名为：`V${POM_VERSION}-Build_${BUILD_NUMBER})`   
4. 应用版本存放路径 (应用版本的存放路径，具体请查看相应的帮助信息)   
5. 备注  

#####触发代码部署配置信息:   
1. 目标集群名称 (部署的目标集群的名字，请在 FIT2CLOUD控制台中 > 集群 查看列表中对应集群的名称。)   
2. 目标虚机组名称 (部署的目标虚机组的名称，请在 FIT2CLOUD控制台中 > 虚机组 查看列表中对应虚机组的名称。如果不填写，则默认部署到集群下的全部虚机组。)   
3. 目标虚机Id (部署的目标虚机的ID，请在 FIT2CLOUD控制台中 > 虚机组 查看列表中对应虚机组的ID。如果不填写，则默认部署到虚机组下的全部虚机。)   
4. 部署策略 (选择你希望使用的部署策略，与FIT2CLOUD中的部署策略相同。)   
假设一个job的名称是test，用户的设置如下
1. 仓库名称 repo-1   
2. 应用名称 app-1   
3. 应用版本名称 V${POM_VERSION}-Build_${BUILD_NUMBER}   
4. 应用版本存放路径 http://repository-proxy.fit2cloud.com:8080/service/local/repositories/releases/content/com/fit2cloud/example/${POM_ARTIFACTID}/${POM_VERSION}/${POM_ARTIFACTID}-${POM_VERSION}-${BUILD_NUMBER}.zip   
5. 备注   

#####触发代码部署配置信息:   
1. 目标集群名称 cluster1   
2. 目标虚机组名称 group1   
3. 目标虚机Id   
4. 部署策略 单台依次部署



三、插件开发说明
-------------------------

1. git clone git@github.com:fit2cloud/codedeploy-plugin.git
2. mvn -Declipse.workspace=codedeploy-plugin eclipse:eclipse eclipse:add-maven-repo
3. import project to eclipse
4. mvn hpi:run -Djetty.port=8090 -Pjenkins 进行本地调试
5. mvn package 打包生成hpi文件


四、历史版本说明
-------------------------
1. V0.1:  
FIT2CLOUD代码部署插件第一个版本.
如需使用该版本插件，可以从此下载：[http://repository-proxy.fit2cloud.com:8080/service/local/repositories/releases/content/org/jenkins-ci/plugins/f2c-codedeploy/0.1/f2c-codedeploy-0.1.hpi](http://repository-proxy.fit2cloud.com:8080/service/local/repositories/releases/content/org/jenkins-ci/plugins/f2c-codedeploy/0.1/f2c-codedeploy-0.1.hpi)
2. V0.2:   
去除系统配置中的FIT2CLOUD账号设置，并在每个Job的配置中增加FIT2CLOUD账号设置，以支持使用不同的FIT2CLOUD账号进行代码部署。
如需使用该版本插件，可以从此下载：[http://repository-proxy.fit2cloud.com:8080/service/local/repositories/releases/content/org/jenkins-ci/plugins/f2c-codedeploy/0.2/f2c-codedeploy-0.2.hpi](http://repository-proxy.fit2cloud.com:8080/service/local/repositories/releases/content/org/jenkins-ci/plugins/f2c-codedeploy/0.2/f2c-codedeploy-0.2.hpi)

如果有问题，请联系bohan@fit2cloud.com
