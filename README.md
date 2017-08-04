# EasyID 分布式主键生成系统

EasyID是一个以snowflake算法为基础的轻量、高效的生成唯一ID的服务。<br/>


# 特点

  轻量级，微服务部署，不需要单独的服务器，以jar包的形式提供服务；<br/>
  一次编译，多处部署，无需多余配置；<br/>
  高并发；<br/>
  高可用，可部署多台EasyID服务，通过zookeeper集中管理服务的注册与退出；<br/>
  负载均衡，采用轮询算法实现。<br/>
  
  
# 技术选型

  基于snowflake算法生成ID；<br/>
  基于netty实现远程通信；<br/>
  基于zookeeper实现服务注册、负载均衡；<br/>
  基于redis实现批量存储；<br/>
  
  
# 服务部署

  在easyid-server中的conf.properties文件中，配置redis和zookeeper信息；<br/>
  使用maven命令打包easyid-server:mvn clean instll -Dmaven.test.skip=true；<br/>
  部署easyid-server：java -jar EasyID-Server-1.0-SNAPSHOT.jar -workerid10 -datacenterid11；<br/>
  
  参数说明：<br/>
    -workerid：工作ID，取值1至31；<br/>
    -datacenterid：数据中心ID，取值1至31；<br/>
    若不指定参数，默认workerid为10，datacenterid为11。<br/>
    建议不同的机器，设置不同的值<br/>
  

# 客户端

  添加easy-cli项目的依赖；<br/>
  在spring配置文件中配置EasyID、ZkClient、Observer类，参考easy-demo项目的配置；<br/>
  通过EasyID类的nextId()，获取id。<br/>
  



