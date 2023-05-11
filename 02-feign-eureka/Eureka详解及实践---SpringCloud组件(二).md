@[TOC](Eureka详解及实践)
# 案例分析

通过[OpenFeign简单使用案例](https://blog.csdn.net/weixin_43811057/article/details/126662596)分析会发现存在什么问题？
![在这里插入图片描述](https://img-blog.csdnimg.cn/ae62a0ef198e4ed28a50b359caa69d96.png)

- consumer需要记忆user-service的地址，如果出现变更，可能得不到通知，地址将失效
- consumer不清楚user-service的状态，服务宕机也不知道
- user-service只有1台服务，不具备高可用性
- 即便user-service形成集群，consumer还需自己实现负载均衡

其实上面说的问题，概括一下就是分布式服务必然要面临的问题：

- 服务管理（注册中心）
  - 如何自动注册和发现
  - 如何实现状态监管
- 服务如何实现负载均衡
- 服务如何解决容灾问题
- 服务如何实现统一配置

以上的问题，我们都将在SpringCloud中得到答案。

# 1.Eureka注册中心
首先我们来解决第一问题，服务的管理。

 - 问题分析
   >在刚才的案例中，user-service对外提供服务，需要对外暴露自己的地址。而consumer（调用者）需要记录服务提供者的地址。将来地址出现变更，还需要及时更新。这在服务较少的时候并不觉得有什么，但是在现在日益复杂的互联网环境，一个项目肯定会拆分出十几，甚至数十个微服务。此时如果还人为管理地址，不仅开发困难，将来测试、发布上线都会非常麻烦，这与DevOps的思想是背道而驰的。
 - 网约车
   >这就好比是 网约车出现以前，人们出门叫车只能叫出租车。一些私家车想做出租却没有资格，被称为黑车。而很多人想要约车，但是无奈出租车太少，不方便。私家车很多却不敢拦，而且满大街的车，谁知道哪个才是愿意载人的。一个想要，一个愿意给，就是缺少引子，缺乏管理啊。
此时滴滴这样的网约车平台出现了，所有想载客的私家车全部到滴滴注册，记录你的车型（服务类型），身份信息（联系方式）。这样提供服务的私家车，在滴滴那里都能找到，一目了然。
此时要叫车的人，只需要打开APP，输入你的目的地，选择车型（服务类型），滴滴自动安排一个符合需求的车到你面前，为你服务，完美！

 - Eureka做什么？

   >Eureka就好比是滴滴，负责管理、记录服务提供者的信息。服务调用者无需自己寻找服务，而是把自己的需求告诉Eureka，然后Eureka会把符合你需求的服务告诉你。
同时，服务提供方与Eureka之间通过“心跳”机制进行监控，当某个服务提供方出现问题，Eureka自然会把它从服务列表中剔除。
这就实现了服务的自动注册、发现、状态监控。


# 2.Eureka原理图

![在这里插入图片描述](https://img-blog.csdnimg.cn/1be9b4efeb284c7d84c5dd27587a63d4.png)
renewal：续约

- Eureka-Server：就是服务注册中心（可以是一个集群），对外暴露自己的地址。
- 提供者：启动后向Eureka注册自己信息（地址，服务名称等），并且定期进行服务续约
- 消费者：服务调用方，会定期去Eureka拉取服务列表，然后使用负载均衡算法选出一个服务进行调用。
- 心跳(续约)：提供者定期通过http方式向Eureka刷新自己的状态

# 3.Eureka和Feign结合基本使用

- 搭建eureka服务端
  > 1.引入依赖 eureka-server
  > 2.配置eureka地址
  > 3.添加@EnableEurekaServer注解
- 提供者去注册服务
   > 1.引入依赖 eureka-client
  > 2.配置eureka地址
- 消费者拉取服务
   > 1.引入依赖 eureka-client
  > 2.配置eureka地址

## 3.1.搭建eureka服务

基本步骤：

 - 创建feign-eureka-server项目


	![在这里插入图片描述](https://img-blog.csdnimg.cn/8d5dcae496fc46099d65234f9d104722.png)

- 导入依赖
案例中使用的springBoot版本（2.2.9.RELEASE）和springcloud版本（Hoxton.SR6）
```xml
<!-- Eureka服务端 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

- 编写启动类，添加@EnableEurekaServer的注解
   

```java
@SpringBootApplication
@EnableEurekaServer
public class FeignEurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeignEurekaServerApplication.class, args);
    }
}

```

- 在项目的resource目录下编写配置文件application.yml，配置下列信息
  - server.port
  - spring.application.name
  - eureka.client.serviceUrl.defaultZone  （默认的是http://localhost:8761，可以手动修改）

```yaml
server:
  port: 10086
spring:
  application:
    name: eureka-server  # 应用名称，会在Eureka中作为服务的id标识（serviceId
eureka:
  client:           # 配置其他Eureka服务的地址列表，多个以“,”隔开
    service-url:   # EurekaServer的地址，现在是自己的地址，如果是集群，需要写其它Server的地址。
      defaultZone: http://127.0.0.1:10086/eureka # 配置eureka的地址
    register-with-eureka: false # 不注册自己
    fetch-registry: false #不拉取服务
```

 - 启动eign-eureka-server项目

   访问 http://127.0.0.1:10086   可以看见eureka目录
![在这里插入图片描述](https://img-blog.csdnimg.cn/35184998ecc44d56af89a1e3c854c00c.png)

## 3.2.服务注册(service)
改造[OpenFeign使用案例](https://blog.csdn.net/weixin_43811057/article/details/126662596)中的provider服务
基本步骤：

- 1）feign-provider-api 导入依赖
由于feign-provider-modules和feign-consumer-8080都引入了feign-provider-api，所以只需要在feign-provider-api添加eureka-client的依赖
```xml
<!-- Eureka客户端 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```
- 2）修改feign接口
![在这里插入图片描述](https://img-blog.csdnimg.cn/58e48a867ef149478c225fe929efcf70.png)


- 3）在feign-provider-modules配置eureka地址

```yaml
server:
  port: 8081
spring:
  application:
    name: user-service
eureka:
  client:
    service-url: # EurekaServer地址
      defaultZone: http://127.0.0.1:10086/eureka/
  instance:
    prefer-ip-address: true #偏好使用ip地址，而不是host主机名
    ip-address: 127.0.0.1
    instance-id: ${spring.application.name}.${eureka.instance.ip-address}.${server.port}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/4d7384d21ca447e3998ce1f25d87452c.png)

 - 4）启动feign-provider-modules项目
![在这里插入图片描述](https://img-blog.csdnimg.cn/904369c8cae2401bad33a80d4496c1c3.png)


## 3.3.服务发现(consumer)

基本步骤：

- 1）引入eurekaClient依赖

	feign-consumer-8080项目中引入了feign-provider-api服务依赖，自动添加eurekaClient依赖

  
- 2）配置eureka服务端地址
 

```yaml
server:
  port: 8080
spring:
  application:
    name: feign-consumer  # 微服务名称
eureka:
  client:
    service-url: # EurekaServer地址 服务上有几个就可以写几个
      defaultZone: http://127.0.0.1:10086/eureka
  instance:
    prefer-ip-address: true #偏好使用ip地址，而不是host主机名
    ip-address: 127.0.0.1
    instance-id: ${spring.application.name}.${eureka.instance.ip-address}.${server.port}
```
- 3）启动feign-consumer-8080项目
    
![在这里插入图片描述](https://img-blog.csdnimg.cn/00c8dd42379443b68c11cc29bceac4d4.png)

## 3.4 通过消费者调用feign接口验证成功性
http://localhost:8080/consumer/depart/get/2
![在这里插入图片描述](https://img-blog.csdnimg.cn/b7b36ab2862d40d2816e13b7e074a63f.png)

# 4.Eureka详解

## 4.1.基础架构
Eureka架构中的三个核心角色：

- 服务注册中心

  Eureka的服务端应用，提供服务注册和发现功能，就是刚刚我们建立的eureka-server

- 服务提供者

  提供服务的应用，可以是SpringBoot应用，也可以是其它任意技术实现，只要对外提供的是Rest风格服务即可。本例中就是我们实现的feign-provider

- 服务消费者

  消费应用从注册中心获取服务列表，从而得知每个服务方的信息，知道去哪里调用服务方。本例中就是我们实现的feign-consumer

## 4.2.高可用的Eureka Server

Eureka Server即服务的注册中心，在刚才的案例中，我们只有一个EurekaServer，事实上EurekaServer也可以是一个集群，形成高可用的Eureka中心。

### 4.2.1.服务同步

多个Eureka Server之间也会互相注册为服务，当服务提供者注册到Eureka Server集群中的某个节点时，该节点会把服务的信息同步给集群中的每个节点，从而实现高可用集群。因此，无论客户端访问到Eureka Server集群中的任意一个节点，都可以获取到完整的服务列表信息。

而作为客户端，需要把信息注册到每个Eureka中：
![在这里插入图片描述](https://img-blog.csdnimg.cn/e04104612ea44ecfb96ccb2da770adab.png)
如果有三个Eureka，则每一个EurekaServer都需要注册到其它几个Eureka服务中，例如：有三个分别为10086、10087、10088则：

- 10086要注册到10087和10088上
- 10087要注册到10086和10088上
- 10088要注册到10086和10087上

### 4.2.2.动手搭建高可用的EurekaServer

我们假设要搭建两条EurekaServer的集群，端口分别为：10086和10087

 - 1）我们修改原来的EurekaServer配置


```yml
server:
  port: 10086
spring:
  application:
    name: eureka-server # 应用名称，会在Eureka中作为服务的id标识（serviceId）
eureka:
  client:
    service-url: # EurekaServer的地址，现在是自己的地址，如果是集群，需要写其它Server的地址。
      defaultZone: http://127.0.0.1:10086/eureka,http://127.0.0.1:10087/eureka
#    register-with-eureka: false # 不注册自己
#    fetch-registry: false #不拉取服务
```

 - 2）再启动一台eureka服务

	注意：idea中一个应用不能启动两次，我们需要重新配置一个启动器：
	![在这里插入图片描述](https://img-blog.csdnimg.cn/3273715867f44f9481fdc024a2ebc6bc.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/16090c77c01744bcb28399a11e672c30.png)

```sql
-Dserver.port=10087
```

 - 3）启动项目观察eureka控制台

	![在这里插入图片描述](https://img-blog.csdnimg.cn/c47bbd2494e646f980887415e32569db.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/3f38c93f3de945058e32168bba739774.png)

 - 4）客户端注册服务到集群

	因为EurekaServer不止一个，因此eureka的客户端配置服务端地址的时候，service-url参数需要变化。需要在feign-provider-modules和feign-consumer-8080两个服务中修改eureka服务端地址：
```yml
eureka:
  client:
    service-url: # EurekaServer地址,多个地址以','隔开
      defaultZone: http://127.0.0.1:10086/eureka,http://127.0.0.1:10087/eureka
```


## 4.3.Eureka客户端

服务提供者要向EurekaServer注册服务，并且完成服务续约等工作。

### 4.3.1  服务注册

==服务提供者在启动时，会检测配置属性中的：`eureka.client.register-with-erueka=true`参数是否正确，事实上默认就是true。如果值确实为true，则会向EurekaServer发起一个Rest请求，并携带自己的元数据信息，Eureka Server会把这些信息保存到一个双层Map结构中。==

- 第一层Map的Key就是服务id，一般是配置中的`spring.application.name`属性
- 第二层Map的key是服务的实例id。一般host+ serviceId + port，例如：`locahost:user-service:8081`
- 值则是服务的实例对象，也就是说一个服务，可以同时启动多个不同实例，形成集群。


user-service默认注册时使用的是主机名，如果我们想用ip进行注册，可以在user-service的application.yml添加配置：

```yaml
eureka:
  instance:
    ip-address: 127.0.0.1 # ip地址
    prefer-ip-address: true # 更倾向于使用ip，而不是host名
    instance-id: ${eureka.instance.ip-address}:${server.port} # 自定义实例的id
```



### 4.3.2 服务续约

==在注册服务完成以后，服务提供者会维持一个**心跳（定时向EurekaServer发起Rest请求）**，告诉EurekaServer：“我还活着”。这个我们称为服务的续约（renewal）==

有两个重要参数可以修改服务续约的行为：

```yaml
eureka:
  instance:
    lease-expiration-duration-in-seconds: 90
    lease-renewal-interval-in-seconds: 30
```

- `lease-renewal-interval-in-seconds`：服务续约(renew)的间隔，默认为30秒
- `lease-expiration-duration-in-seconds`：服务失效时间，默认值90秒

也就是说，默认情况下每隔30秒服务会向注册中心发送一次心跳，证明自己还活着。如果超过90秒没有发送心跳，EurekaServer就会认为该服务宕机，会从服务列表中移除，这两个值在生产环境不要修改，默认即可。
### 4.3.3  获取服务列表

当服务消费者启动是，会检测`eureka.client.fetch-registry=true`参数的值，如果为true，则会从Eureka Server服务的列表只读备份，然后缓存在本地。并且`每隔30秒`会重新获取并更新数据。我们可以通过下面的参数来修改：

```yaml
eureka:
  client:
    registry-fetch-interval-seconds: 30
```
## 4.4.Eureka服务端
服务下线、失效剔除和自我保护

### 4.4.1.服务下线

当服务**进行正常关闭**操作时，它会**触发一个服务下线的REST请求给Eureka Server**，告诉服务注册中心：“我要下线了”。服务中心接受到请求之后，将该服务置为**下线状态**。

### 4.4.2.失效剔除

有时我们的服务可能由于内存溢出或网络故障等原因使得服务不能正常的工作，而服务注册中心并未收到“**服务下线**”的请求。相对于服务提供者的“**服务续约**”操作，==服务注册中心在启动时会创建一个定时任务，默认每隔一段时间（默认为60秒）将当前清单中超时（默认为90秒）没有续约的服务剔除，这个操作被称为**失效剔除**。==

可以通过`eureka.server.eviction-interval-timer-in-ms`参数对其进行修改，单位是毫秒。


### 4.4.3. 自我保护

我们关停一个服务，就会在Eureka面板看到一条警告：
![在这里插入图片描述](https://img-blog.csdnimg.cn/6347b94152d9423d8453a361b60345a3.png)
这是触发了Eureka的自我保护机制。当服务未按时进行心跳续约时，Eureka会统计服务实例最近15分钟心跳续约的比例是否低于了85%。在生产环境下，因为网络延迟等原因，心跳失败实例的比例很有可能超标，但是此时就把服务剔除列表并不妥当，因为服务可能没有宕机。Eureka在这段时间内不会剔除任何服务实例，直到网络恢复正常。生产环境下这很有效，保证了大多数服务依然可用，不过也有可能获取到失败的服务实例，因此服务调用者必须做好服务的失败容错。

我们可以通过下面的配置来关停自我保护：

```yaml
eureka:
  server:
    enable-self-preservation: false # 关闭自我保护模式（缺省为打开）
```

## 4.5.总结：

- 服务的注册和发现都是可控制的，可以关闭也可以开启。默认都是开启
- 注册后需要心跳，心跳周期默认30秒一次，超过90秒没法认为宕机
- 服务拉取默认30秒拉取一次
- Eureka每个60秒会剔除标记为宕机的服务
- Eureka会有自我保护，当心跳失败比例超过阈值，那么开启自我保护，不再剔除服务。
- Eureka高可用就是多台Eureka互相注册在对方上





## 4.6.Eureka的面试点

- eureka的高可用

```bash
      多台Eureka互相注册在对方上。
      多个Eureka Server之间也会互相注册为服务，当服务提供者注册到Eureka Server集群中的某个节点时，
      该节点会把服务的信息同步给集群中的每个节点，从而实现高可用集群。
      因此，无论客户端访问到Eureka Server集群中的任意一个节点，都可以获取到完整的服务列表信息
- eureka的服务注册拉取等时间值
```
  - 服务续约（心跳周期）
    

```bash
默认情况下每个30秒服务会向注册中心发送一次心跳，证明自己还活着。
如果超过90秒没有发送心跳，EurekaServer就会认为该服务宕机，会从服务列表中移除，这两个值在生产环境不要修改，默认即可。
    eureka:
      instance:
        lease-expiration-duration-in-seconds: 90 #服务失效时间，默认值90秒
        lease-renewal-interval-in-seconds: 30  #服务续约(renew)的间隔，默认为30秒
```

  - 服务拉取周期
   

```bash
 当服务消费者启动是，会检测eureka.client.fetch-registry=true参数的值，
 如果为true，则会从Eureka Server服务的列表只读备份，然后缓存在本地。
 并且每隔30秒会重新获取并更新数据
    eureka:
      client:
        registry-fetch-interval-seconds: 30
```
- 服务下线
 

```bash
 当服务进行正常关闭操作时，它会触发一个服务下线的REST请求给Eureka Server，
 告诉服务注册中心：“我要下线了”。服务中心接受到请求之后，将该服务置为下线状态。
```
- 服务的失效剔除和自我保护

```bash
  失效剔除：（ Eureka每隔60秒会剔除标记为宕机的服务）
    有时我们的服务可能由于内存溢出或网络故障等原因使得服务不能正常的工作，
    而服务注册中心并未收到“服务下线”的请求。相对于服务提供者的“服务续约”操作，
    服务注册中心在启动时会创建一个定时任务，默认每隔一段时间（默认为60秒）将当前清单中超时（默认为90秒）没有续约的服务剔除，
    这个操作被称为失效剔除
 自我保护：（当心跳失败比例超过阈值，那么开启自我保护，不再剔除服务）
    在生产环境下，因为网络延迟等原因，心跳失败实例的比例很有可能超标，
    但是此时就把服务剔除列表并不妥当，因为服务可能没有宕机.
    Eureka在这段时间内不会剔除任何服务实例，直到网络恢复正常。生产环境下这很有效，保证了大多数服务依然可用，
    不过也有可能获取到失败的服务实例，因此服务调用者必须做好服务的失败容错。
```




  
- eureka和zookeeper的区别






