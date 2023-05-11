
@[TOC](Robbin负载均衡详解及实践)

# 一 为什么使用Robbin？

在[Eureka详解及实践---SpringCloud组件(二)](https://blog.csdn.net/weixin_43811057/article/details/114806230)案例中，我们启动了一个feign-provider-modules，然后通过feign-consumer-8080调用feign接口来访问。
但是实际环境中，我们往往会开启很多个feign-provider-modules的集群。此时我们获取的服务列表中就会有多个，到底该访问哪一个呢？
一般这种情况下我们就需要编写负载均衡算法，在多个实例列表中进行选择。

# 二 Robbin概念
![在这里插入图片描述](https://img-blog.csdnimg.cn/6b472676cd07470db95fc2b5926528fd.png)

Ribbon属于进程内LB，它只是一个类库，集成于消费方进程，消费方通过它来获取服务提供方的地址。
所以说我们只需要在消费者端配置负载均衡策略。
>进程内LB：将LB集成到消费方，消费方从服务注册中心获知哪些地址可用，然后自己再从可用地址中选择一个合适的服务器。

# 三 负载均衡实践
案例中的负载均衡是基于 openfeign+eureka实现的。
引入 spring-cloud-starter-openfeign 后，使用 Ribbon 是客户端负载均衡器 则无需引入额外依赖，因为引入的 spring-cloud-starter-openfeign 依赖中集成了 Ribbon。

实现步骤：
> 1.启动eureka客户端
> 2.启动多个provider服务，注册到eureka
> 3.在consumer端配置负载均衡参数

## 1.启动eureka客户端
![在这里插入图片描述](https://img-blog.csdnimg.cn/2db074831cda4b9da82dca85dfee0825.png)

## 2.启动多个provider服务，注册到eureka

 1. 为了观察负载均衡效果，改造一下feign-provider-modules服务中的getHandle方法。

	![在这里插入图片描述](https://img-blog.csdnimg.cn/c4191246b24e4778a9a4111a796dc724.png)

 2. 启动多个provider实例项目

	![在这里插入图片描述](https://img-blog.csdnimg.cn/3d7bcf00bb5a41869594a710749901be.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/7502d034e4c242ef85e4247b44f0f981.png)

 3. 观察eureka控制台发现3个provider成功注册
http://127.0.0.1:10086/
	![在这里插入图片描述](https://img-blog.csdnimg.cn/2fc1ad63e8364d4983c54be47ac67395.png)


## 3.在consumer端配置负载均衡参数

```yml
feign-provider: # 服务名
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule  # 选择负载均衡策略，默认为轮询方式，当前配置为随机方式
    ConnectTimeout: 250                 # 连接超时时间
    ReadTimeout: 1000                   # ribbon 读取超时时间
    OkToRetryOnAllOperations: true      # 是否对所有操作都进行重试
    MaxAutoRetriesNextServer: 1         # 切换实例的重试次数
    MaxAutoRetries: 1                   # 对当前实例的重试次数
```

多次访问观察结果为随机
http://localhost:8080/consumer/depart/get/2
![在这里插入图片描述](https://img-blog.csdnimg.cn/8a1ea62bd14048c2bde1013018facf0a.png)
# 四 Robbin源码剖析
