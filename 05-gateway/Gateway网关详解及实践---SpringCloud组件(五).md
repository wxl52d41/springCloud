@[TOC](Gateway网关详解及实践)
# 一 简介

## 1.1介绍

Spring Cloud GateWay是Spring Cloud的⼀个全新项⽬，⽬标是取代Netﬂix Zuul， 它基于Spring5.0+SpringBoot2.0+WebFlux（基于⾼性能的Reactor模式响应式通信 框架Netty，异步⾮阻塞模型）等技术开发，性能⾼于Zuul，官⽅测试，GateWay是 Zuul的1.6倍，旨在为微服务架构提供⼀种简单有效的统⼀的API路由管理⽅式。 
Spring Cloud GateWay不仅提供统⼀的路由⽅式（反向代理）并且基于 Filter(定义 过滤器对请求过滤，完成⼀些功能) 链的⽅式提供了⽹关基本的功能，例如：鉴权、 流量控制、熔断、路径重写、⽇志监控等。

**⽹关在架构中的位置**

![在这里插入图片描述](https://img-blog.csdnimg.cn/8a20757134e1451f9880bd79325e3e3c.png)
## 1.2.GateWay核⼼概念 
Zuul1.x 阻塞式IO 2.x 基于Netty 
Spring Cloud GateWay天⽣就是异步⾮阻塞的，基于Reactor模型 

==⼀个请求—>⽹关根据⼀定的条件匹配—匹配成功之后可以将请求转发到指定的服务地址；⽽在这个过程中，我们可以进⾏⼀些⽐较具体的控制（限流、⽇志、⿊⽩名 单）== 

 - `路由（route）`： ⽹关最基础的部分，也是⽹关⽐较基础的⼯作单元。路由由⼀个ID、⼀个⽬标URL（最终路由到的地址）、⼀系列的**断⾔（匹配条件判断）** 和 **Filter过滤器（精细化控制）** 组成。如果断⾔为true，则匹配该路由。
 - `断⾔（predicates）`：参考了Java8中的断⾔java.util.function.Predicate，开发⼈员可以匹配Http请求中的所有内容（包括请求头、请求参数等）（类似于 nginx中的location匹配⼀样），如果断⾔与请求相匹配则路由。
 - `过滤器（ﬁlter）`：⼀个标准的Spring webFilter，使⽤过滤器，可以在请求之前或者之后执⾏业务逻辑。

**来⾃官⽹的⼀张图**
![在这里插入图片描述](https://img-blog.csdnimg.cn/485c0103f00e4da5992e2f3216038a3b.png)
其中，Predicates断⾔就是我们的匹配条件，⽽Filter就可以理解为⼀个⽆所不能的拦截器，有了这两个元素，结合⽬标URL，就可以实现⼀个具体的路由转发。
## 1.3.GateWay核心功能 
![在这里插入图片描述](https://img-blog.csdnimg.cn/fd4606ace1134c9fa7b972c0badc4bb4.png)

 - **路由**：gateway加入后，一切请求都必须先经过gateway，因此gateway就必须根据某种规则，把请求转发到某个微				服务，这个过程叫做路由。
 - **权限控制**：请求经过路由时，我们可以判断请求者是否有请求资格，如果没有则进行拦截。
 - **限流**：当请求流量过高时，在网关中按照下流的微服务能够接受的速度来放行请求，避免服务压力过大。

# 二Gateway入门案例
[源码地址](https://github.com/wxl52d41/springCloud/tree/master/05-gateway)
Gateway的路由功能，基本步骤如下：

1. 创建SpringBoot工程gateway_server，引入网关依赖
2. 编写启动类
3. 编写基础配置：服务端口，应用名称
4. 编写路由规则
5. 启动网关服务进行测试

![在这里插入图片描述](https://img-blog.csdnimg.cn/f2748be2f5f747799563ae144d8ae3c6.png)

## 2.1 gateway依赖

```xml
    <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
```
## 2.2 gateway配置

```yml
server:
  port: 10010
spring:
  application:
    name: gateway-server # 服务名
  cloud:
    gateway:
      routes: #路由规则的列表
        - id: consumer-service # 当前路由的唯一标识
          uri: http://127.0.0.1:8080 # 路由的目标微服务地址
          predicates: # 断言，定义请求的匹配规则
            - Path=/consumer/** # Path代表按照路径匹配的规则，/consumer/**是指路径必须以/consumer开头

```
## 2.3 gateway测试
启动下列三个服务
![在这里插入图片描述](https://img-blog.csdnimg.cn/af1d67d8397b4e4f806010aa4223b547.png)
当我们访问http://localhost:10010/consumer/depart/get/1时，首先会进入网关服务，断言判断符合=/consumer/**，因此请求会被代理到http://localhost:8080/consumer/depart/get/1
![在这里插入图片描述](https://img-blog.csdnimg.cn/e886b03a9a8c4a4981b5f8b7f1f6afc9.png)

# 三Gateway面向服务的路由

## 3.1.门案例问题

![在这里插入图片描述](https://img-blog.csdnimg.cn/e851457145b44316827d0133aa6e6274.png)
在入门案例中路由的目标地址是写死的，在微服务的情况下，可能目标服务是个集群那么这样做显然不合理。我们应该根据服务的名称去Eureka注册中心查找 服务对应的所有实例列表，并且对服务列表进行负载均衡才对！

**案例模块**
![在这里插入图片描述](https://img-blog.csdnimg.cn/3e194e3a44a449b8b9a3783c6ae0f801.png)

## 3.2.gateway-server结合eureka步骤
feign-consumer-8080  feign-eureka-server  feign-provider-8081这三个模块沿用了之前旧的部分，具体搭建步骤不在赘述。着重讲解gateway-server结合eureka。


### 3.2.1.添加Eureka客户端依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 3.2.2.添加Eureka配置
将gateway服务注到eure中
```yaml
eureka:
  client:
    service-url: # EurekaServer地址
      defaultZone: http://127.0.0.1:10086/eureka/
  instance:
    prefer-ip-address: true #偏好使用ip地址，而不是host主机名
    ip-address: 127.0.0.1
    instance-id: ${spring.application.name}.${eureka.instance.ip-address}.${server.port}
```

### 3.2.3修改映射配置
因为已经有了Eureka客户端，我们可以从Eureka获取服务的地址信息，因此映射时无需指定IP地址，而是通过服务名称来访问，而且Zuul已经集成了Ribbon的负载均衡功能。

```yaml
server:
  port: 10010

eureka:
  client:
    service-url: # EurekaServer地址
      defaultZone: http://127.0.0.1:10086/eureka/
  instance:
    prefer-ip-address: true #偏好使用ip地址，而不是host主机名
    ip-address: 127.0.0.1
    instance-id: ${spring.application.name}.${eureka.instance.ip-address}.${server.port}

spring:
  application:
    name: gateway-server # 服务名
  cloud:
    gateway:
      routes: #路由规则的列表,可以有多个
        - id: feign-consumer # 当前路由的唯一标识
          uri: lb://feign-consumer  # 路由的目标微服务,lb:代表负载均衡，feign-consumer:代表服务id
          predicates: # 断言，定义请求的匹配规则
            - Path=/consumer/** # Path代表按照路径匹配的规则，/consumer/**是指路径必须以/consumer开头
```


这里修改了uri的路由方式：

- lb：负载均衡的协议，将来会使用Ribbon实现负载均衡
- feign-consumer：服务的id

![在这里插入图片描述](https://img-blog.csdnimg.cn/a5d9ee46317c4c32b85b904a13a68ca0.png)

### 3.2.4.启动测试

 - 启动模块

	![在这里插入图片描述](https://img-blog.csdnimg.cn/d2e32cc2231e49e18fc6e945125d6ac1.png)

 - 查看eureka客户端

	![在这里插入图片描述](https://img-blog.csdnimg.cn/fee0b2cfbd7246d5bdc448bde93dfa5e.png)

 - 访问并观察结果

	当我们访问http://localhost:10010/consumer/depart/get/1时，首先会进入网关服务，断言判断符合=/consumer/**，因此请求会被代理到http://localhost:8080/consumer/depart/get/1
	![在这里插入图片描述](https://img-blog.csdnimg.cn/12d12ae741584c229679f2795f534e2c.png)

	![在这里插入图片描述](https://img-blog.csdnimg.cn/618c4b00dd704ed1a438786a19d7359b.png)
## 3.3局部过滤器
GatewayFilter Factories是Gateway中的局部过滤器工厂，作用于某个特定路由，允许以某种方式修改传入的HTTP请求或返回的HTTP响应。![在这里插入图片描述](https://img-blog.csdnimg.cn/e39ec735c8b34842aed2bc59af0718fa.png)
### 3.3.1.Hystrix

网关做请求路由转发，如果被调用的请求阻塞，需要通过Hystrix来做线程隔离和熔断，防止出现故障。
#### 1）引入Hystrix的依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
```
#### 2）开启Hystrix，添加@EnableHystrix

```java
@EnableHystrix
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

#### 3）定义降级处理规则

可以通过default-filter来配置，会作用于所有的路由规则。

```yml
spring:
  application:
    name: gateway-server # 服务名
  cloud:
    gateway:
      routes: #路由规则的列表,可以有多个
        - id: feign-consumer # 当前路由的唯一标识
          uri: lb://feign-consumer  # 路由的目标微服务,lb:代表负载均衡，feign-consumer:代表服务id
          predicates: # 断言，定义请求的匹配规则
            - Path=/consumer/** # Path代表按照路径匹配的规则，/consumer/**是指路径必须以/consumer开头
      default-filters: # 默认过滤项
        - name: Hystrix # 指定过滤工厂名称（可以是任意过滤工厂类型）
          args: # 指定过滤的参数
            name: fallbackcmd  # hystrix的指令名
            fallbackUri: forward:/fallbackTest # 失败后的跳转路径
hystrix:
    command:
      default:
        execution.isolation.thread.timeoutInMilliseconds: 1000 # 失败超时时长
```
- default-filters：默认过滤项，作用于所有的路由规则
  - name：过滤工厂名称，这里指定Hystrix，意思是配置Hystrix类型
  - args：配置过滤工厂的配置
    - name：Hystrix的指令名称，用于配置例如超时时长等信息
    - fallbackUri：失败降级时的跳转路径
![在这里插入图片描述](https://img-blog.csdnimg.cn/ed287ed56ed04a6ab0f1a902c8fbea4a.png)
#### 4）定义降级的处理函数

定义一个controller，用来编写失败的处理逻辑：

```java
@RestController
public class FallbackController {

    @RequestMapping(value = "/fallbackTest")
    public Map<String, String> fallBackController() {
        Map<String, String> response = new HashMap<>();
        response.put("code", "502");
        response.put("msg", "服务超时");
        return response;
    }
}

```
#### 5）测试
重启gateway，不启动consumer，访问http://localhost:10010/consumer/depart/get/1一秒后观察结果发现走了超时方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/e0bc8ef5989b44f4b81e367471ecb26c.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/c0de66e4f43d4c48a09a608d629ad61d.png)
### 3.3.2.路由前缀

#### 1）问题演示
![在这里插入图片描述](https://img-blog.csdnimg.cn/6fcb48ab447a48c3a96b0d35b95ec473.png)

我们之前用/consumer/**这样的映射路径代表feign-consumer这个服务。因此请求feign-consumer服务的一切路径要以/consumer/**开头

比如，访问：localhost:10010/consumer/depart/get/1会被代理到：http://localhost:8080/consumer/depart/get/1

现在，我们在feign-consumer中的controller中定义一个新的接口：


```java
@RestController
@RequestMapping("/test/depart")
public class TestController {
    @GetMapping("/get/{id}")
    public DepartVO getHandle(@PathVariable("id") int id) {
        DepartVO departVO = new DepartVO();
        departVO.setId(id);
        departVO.setName("测试名称");
        return departVO;
    }
}
```
这个接口的路径是/test/depart/get/1，并不是以/consumer/开头。当访问：localhost:10010/test/depart/get/时，并不符合映射路径，因此会得到404.

无论是 /consumer/**还是/test/**都是feign-consumer中的一个controller路径，都不能作为网关到feign-consumer的映射路径。

因此我们需要定义一个额外的映射路径，例如：/feign-consumer，配置如下

```yml
# 路由前缀配置
spring:
  application:
    name: gateway-server # 服务名
  cloud:
    gateway:
      routes: #路由规则的列表,可以有多个
        - id: feign-consumer # 当前路由的唯一标识
          uri: lb://feign-consumer  # 路由的目标微服务,lb:代表负载均衡，feign-consumer:代表服务id
          predicates: # 断言，定义请求的匹配规则
            - Path=/feign-consumer/** # Path代表按照路径匹配的规则，feign-consumer：代表服务id。/feign-consumer/**是指路径必须以/feign-consumer
      default-filters: # 默认过滤项
        - name: feign-consumer # 指定过滤工厂名称（可以是任意过滤工厂类型）
          args: # 指定过滤的参数
            name: fallbackcmd  # hystrix的指令名
            fallbackUri: forward:/fallbackTest # 失败后的跳转路径
```
那么问题来了:

当我们访问：：`localhost:10010/feign-consumer/consumer/depart/get/1`,映射路径/feign-consumer指向用户服务,会被代理到：`http://localhost:8080/feign-consumer/consumer/depart/get/1`

当我们访问：`localhost:10010/feign-consumer/test/depart/get/1`,映射路径/feign-consumer指向用户服务,会被代理到：`http://localhost:8080/feign-consumer/test/depart/get/1`

而在`feign-consumer`中，无论是`/feign-consumer/consumer/depart/get/1`还是`/feign-consumer/test/depart/get/1`都是错误的，因为多了一个 **/feign-consumer** 。

这个 **/feign-consumer** 是gateway中的映射路径，不应该被代理到微服务，怎办吧？

#### 2）去除路由前缀

解决思路很简单，当我们访问`http://localhost:10010/feign-consumer/consumer/depart/get/1`时，网关利用 **/feign-consumer** 这个映射路径匹配到了用户微服务，请求代理时，只要把 **/feign-consumer** 这个映射路径去除不就可以了吗。

恰好有一个过滤器：`StripPrefixFilterFactory`可以满足我们的需求。

https://cloud.spring.io/spring-cloud-static/spring-cloud-gateway/2.2.3.RELEASE/reference/html/#the-stripprefix-gatewayfilter-factory

我们修改刚才的路由配置：
![在这里插入图片描述](https://img-blog.csdnimg.cn/ede94901b9b94b98969385feca39d76c.png)

```yml
# 路由前缀配置
spring:
  application:
    name: gateway-server # 服务名
  cloud:
    gateway:
      routes: #路由规则的列表,可以有多个
        - id: feign-consumer # 当前路由的唯一标识
          uri: lb://feign-consumer  # 路由的目标微服务,lb:代表负载均衡，feign-consumer:代表服务id
          predicates: # 断言，定义请求的匹配规则
            - Path=/feign-consumer/** # Path代表按照路径匹配的规则，feign-consumer：代表服务id。/feign-consumer/**是指路径必须以/feign-consumer
          filters:
            - StripPrefix=1
      default-filters: # 默认过滤项
        - name: feign-consumer # 指定过滤工厂名称（可以是任意过滤工厂类型）
          args: # 指定过滤的参数
            name: fallbackcmd  # hystrix的指令名
            fallbackUri: forward:/fallbackTest # 失败后的跳转路径
```
此时，网关做路由的代理时，就不会把/feign-consumer作为目标请求路径的一部分了。
当我们访问：：`localhost:10010/feign-consumer/consumer/depart/get/1`,会被代理到：`http://localhost:8080/consumer/depart/get/1`

当我们访问：`localhost:10010/feign-consumer/test/depart/get/1`,映射路径/feign-consumer指向用户服务,会被代理到：`http://localhost:8080/test/depart/get/1`

**访问测试**
![在这里插入图片描述](https://img-blog.csdnimg.cn/1af0ec10f19d449898c94c98abea01be.png)

## 3.4全局过滤器


全局过滤器Global Filter 与局部的GatewayFilter会在运行时合并到一个过滤器链中，并且根据`org.springframework.core.Ordered`来排序后执行，顺序可以通过`getOrder()`方法或者`@Order`注解来指定。




### 3.4.1.GlobalFilter接口

全局过滤器的顶级接口：
![在这里插入图片描述](https://img-blog.csdnimg.cn/dd93b3041a0b45a8af593436b7188902.png)
实现接口，就要实现其中的filter方法，在方法内部完成过滤逻辑，其中的参数包括：

- ServerWebExchange：一个类似于Context的域对象，封装了Request、Response等服务相关的属性

	![在这里插入图片描述](https://img-blog.csdnimg.cn/81d89d11a04846cbb040216701d13a25.png)

- GatewayFilterChain：过滤器链，用于放行请求到下一个过滤器

	![在这里插入图片描述](https://img-blog.csdnimg.cn/f28a7c3734564926a92cfc46a003d44a.png)
### 3.4.2.过滤器顺序

通过添加`@Order`注解，可以控制过滤器的优先级，从而决定了过滤器的执行顺序。

>一个过滤器的执行包括`"pre"`和`"post"`两个过程:
在`GlobalFilter.filter()`方法中编写的逻辑属于**pre阶段**，
在使用`GatewayFilterChain.filter().then()`的阶段，属于**Post阶段**。


优先级最高的过滤器，会在pre过程的第一个执行，在post过程的最后一个执行，如图：

![在这里插入图片描述](https://img-blog.csdnimg.cn/233f876c2eec4d81bc5d9137f5e86e02.png)

我们可以在pre阶段做很多事情，诸如：

- 登录状态判断
- 权限校验
- 请求限流等

### 3.4.3.自定义过滤器

定义过滤器只需要实现GlobalFilter即可，不过我们有多种方式来完成：

- 方式一：定义过滤器类，实现GlobalFilter接口
- 方式二：通过@Configuration类结合lambda表达式

#### 3.4.3.1.登录拦截器(实现GlobalFilter接口方式)

现在，通过自定义过滤器，模拟一个登录校验功能，逻辑非常简单：

- 获取用户请求参数中的 access-token 参数
- 判断是否为"admin"
  - 如果不是，证明未登录，拦截请求
  - 如果是，证明已经登录，放行请求

代码如下：

```java
@Order(0) // 通过注解声明过滤器顺序
@Component
public class LoginFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取token
        String token = exchange.getRequest().getQueryParams().toSingleValueMap().get("access-token");
        // 判断请求参数是否正确
        if(StringUtils.equals(token, "admin")){
            // 正确，放行
            return chain.filter(exchange);
        }
        // 错误，需要拦截，设置状态码
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // 结束任务
        return exchange.getResponse().setComplete();
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/bb47e940442e468da34d5ea02ff2dc04.png)
**测试：**

 - 带错误参数的情况：
 	http://localhost:10010/feign-consumer/consumer/depart/get/1![在这里插入图片描述](https://img-blog.csdnimg.cn/f7a9f1bc318246caa4082baa4edc5d07.png)

 - 带正确参数的情况：
	http://localhost:10010/feign-consumer/consumer/depart/get/1?access-token=admin![在这里插入图片描述](https://img-blog.csdnimg.cn/1d3cc490ef9043d1b021d29f48877070.png)
#### 3.4.3.2.多过滤器演示(lambda表达式)

```java
@Configuration
public class FilterConfiguration {

    @Bean
    @Order(-2)
    public GlobalFilter globalFilter1(){
        return ((exchange, chain) -> {
            System.out.println("过滤器1的pre阶段！");
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                System.out.println("过滤器1的post阶段！");
            }));
        });
    }

    @Bean
    @Order(-1)
    public GlobalFilter globalFilter2(){
        return ((exchange, chain) -> {
            System.out.println("过滤器2的pre阶段！");
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                System.out.println("过滤器2的post阶段！");
            }));
        });
    }

    @Bean
    @Order(0)
    public GlobalFilter globalFilter3(){
        return ((exchange, chain) -> {
            System.out.println("过滤器3的pre阶段！");
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                System.out.println("过滤器3的post阶段！");
            }));
        });
    }
}
```

http://localhost:10010/feign-consumer/consumer/depart/get/1?access-token=admin
![在这里插入图片描述](https://img-blog.csdnimg.cn/0c515cc4d3d84e12a9ad63b06ea1a728.png)



## 3.5.网关限流

网关除了请求路由、身份验证，还有一个非常重要的作用：请求限流。当系统面对高并发请求时，为了减少对业务处理服务的压力，需要在网关中对请求限流，按照一定的速率放行请求。

![在这里插入图片描述](https://img-blog.csdnimg.cn/1074ba382d9848838f3fb1f22adc9df7.png)


常见的限流算法包括：

- 计数器算法
- 漏桶算法
- 令牌桶算法

### 3.5.1.令牌桶算法原理

SpringGateway中采用的是令牌桶算法，令牌桶算法原理：

- 准备一个令牌桶，有固定容量，一般为服务并发上限
- 按照固定速率，生成令牌并存入令牌桶，如果桶中令牌数达到上限，就丢弃令牌。
- 每次请求调用需要先获取令牌，只有拿到令牌，才继续执行，否则选择选择等待或者直接拒绝。

![在这里插入图片描述](https://img-blog.csdnimg.cn/0e59671a83384a8ba86a65f9e308e5d8.png)


### 3.5.2.Gateway中限流

SpringCloudGateway是采用令牌桶算法，其令牌相关信息记录在redis中，因此我们需要安装redis，并引入Redis相关依赖。

#### 1) 引入redis

引入Redis有关依赖：

```xml
<!--redis-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

注意：这里不是普通的redis依赖，而是响应式的Redis依赖，因为SpringGateway是基于WebFlux的响应式项目。

在application.yml中配置Redis地址：

```yaml
spring:
  redis:
    host: localhost
```



#### 2) 配置过滤条件key

Gateway会在Redis中记录令牌相关信息，我们可以自己定义令牌桶的规则，例如：

- 给不同的请求URI路径设置不同令牌桶
- 给不同的登录用户设置不同令牌桶
- 给不同的请求IP地址设置不同令牌桶

Redis中的一个Key和Value对就是一个令牌桶。因此Key的生成规则就是桶的定义规则。SpringCloudGateway中key的生成规则定义在`KeyResolver`接口中：

```java
public interface KeyResolver {

	Mono<String> resolve(ServerWebExchange exchange);

}
```

这个接口中的方法返回值就是给令牌桶生成的key。API说明：

- Mono：是一个单元素容器，用来存放令牌桶的key
- ServerWebExchange：上下文对象，可以理解为ServletContext，可以从中获取request、response、cookie等信息



比如上面的三种令牌桶规则，生成key的方式如下：

- 给不同的请求URI路径设置不同令牌桶，示例代码：

  ```java
  return Mono.just(exchange.getRequest().getURI().getPath());// 获取请求URI
  ```

- 给不同的登录用户设置不同令牌桶

  ```java
  return exchange.getPrincipal().map(Principal::getName);// 获取用户
  ```

- 给不同的请求IP地址设置不同令牌桶

  ```java
  return Mono.just(exchange.getRequest().getRemoteAddress().getHostName());// 获取请求者IP
  ```

这里我们选择最后一种，使用IP地址的令牌桶key。

我们在`config`中定义一个类，配置一个IpKeyResolver 的Bean实例：

```java
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class IpKeyResolver implements KeyResolver {
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
    }
}

```

#### 3) 配置桶参数

另外，令牌桶的参数需要通过yaml文件来配置，参数有2个：

- `replenishRate`：每秒钟生成令牌的速率，基本上就是每秒钟允许的最大请求数量

- `burstCapacity`：令牌桶的容量，就是令牌桶中存放的最大的令牌的数量

完整配置如下：

```yaml
spring:
  application:
    name: ly-gateway
  cloud:
    gateway:
      default-filters: # 默认过滤项
      - StripPrefix=1 # 去除路由前缀
      - name: Hystrix # 指定过滤工厂名称（可以是任意过滤工厂类型）
        args: # 指定过滤的参数
          name: fallbackcmd  # hystrix的指令名
          fallbackUri: forward:/hystrix/fallback # 失败后的跳转路径
      - name: RequestRateLimiter #请求数限流 名字不能随便写
        args:
          key-resolver: "#{@ipKeyResolver}" # 指定一个key生成器
          redis-rate-limiter.replenishRate: 2 # 生成令牌的速率
          redis-rate-limiter.burstCapacity: 2 # 桶的容量
```

这里配置了一个过滤器：RequestRateLimiter，并设置了三个参数：

- `key-resolver`：`"#{@ipKeyResolver}"`是SpEL表达式，写法是`#{@bean的名称}`，ipKeyResolver就是我们定义的Bean名称

- `redis-rate-limiter.replenishRate`：每秒钟生成令牌的速率

- `redis-rate-limiter.burstCapacity`：令牌桶的容量

这样的限流配置可以达成的效果：

- 每一个IP地址，每秒钟最多发起2次请求
- 每秒钟超过2次请求，则返回429的异常状态码



### 3.8.3.测试

我们快速在浏览器多次访问http://localhost:10010/feign-consumer/consumer/depart/get/1?access-token=admin，就会得到一个错误：
![在这里插入图片描述](https://img-blog.csdnimg.cn/d42d74b3f15b4c8cb3bb8a8b3013707b.png)


429：代表请求次数过多，触发限流了。

# 源码地址

源码地址
[05-gateway-eureka](https://github.com/wxl52d41/springCloud/tree/master/05-gateway-eureka)


**上一篇：**[Hystrix详解及实践---SpringCloud组件(四)](https://blog.csdn.net/weixin_43811057/article/details/130630265?spm=1001.2014.3001.5501)
