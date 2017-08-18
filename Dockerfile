#easyid-server
FROM centos:latest
ADD jdk-8u131-linux-x64.tar.gz /usr/java/
ADD EasyID-Server-STABLE-1.0.jar /user/java/
ENV JAVA_HOME=/usr/java/jdk1.8.0_131
ENV JRE_HOME=/usr/java/jdk1.8.0_131/jre
ENV CLASSPATH=$JAVA_HOME/lib:$JAVA_HOME/jre/lib
ENV PATH=$JAVA_HOME/bin:$PATH
EXPOSE 9131
CMD ["java","-jar","/user/java/EasyID-Server-STABLE-1.0.jar"]
