# EasyID 分布式主键生成系统

EasyID是一个以snowflake算法为基础的轻量、高效的生成唯一ID的服务。<br/>

# 特点

  轻量级，微服务部署，不需要单独的服务器，以jar包的形式提供服务；<br/>
  高并发，高可用，负载均衡；<br/>
  
  
# 技术选型
  基于snowflake算法生成ID；<br/>
  基于netty实现远程通信；<br/>
  基于zookeeper实现服务注册、负载均衡；<br/>
  基于redis实现批量存储；<br/>
  
  



