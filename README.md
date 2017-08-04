# EasyID 分布式主键生成系统

EasyID是一个以snowflake算法为基础的轻量、高效的生成唯一ID的服务。<br/>


# 特点

  轻量级，微服务部署，不需要单独的服务器，以jar包的形式提供服务；<br/>
  一次编译，多处部署；<br/>
  高并发，高可用，负载均衡；<br/>
  
  
# 技术选型

  基于snowflake算法生成ID；<br/>
  基于netty实现远程通信；<br/>
  基于zookeeper实现服务注册、负载均衡；<br/>
  基于redis实现批量存储；<br/>
  
  
# 部署

  在easyid-server中的conf.properties文件中，配置redis和zookeeper信息；<br/>
  使用maven命令打包easyid-server:mvn clean instll -Dmaven.test.skip=true；<br/>
  部署easyid-server：java -jar EasyID-Server-1.0-SNAPSHOT.jar -workerid10 -datacenterid11；<br/>
  
  参数说明：<br/>
    -workerid：工作ID，取值1~31；<br/>
    -datacenterid：数据中心ID，取值1~31；<br/>
    若不指定参数，默认workerid为10，datacenterid为11。<br/>
    建议不同的机器，设置不同的值<br/>
  
  
  



