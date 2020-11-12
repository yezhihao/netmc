基于Netty实现Mvc开发模式的框架
====================

## 特性
* 基于Netty，实现传统的MVC开发方式

## 场景
* TCP协议服务端开发

## 代码仓库
 * Gitee仓库地址：[https://gitee.com/yezhihao/netmc/tree/master](https://gitee.com/yezhihao/netmc/tree/master)
 * Github仓库地址：[https://github.com/yezhihao/netmc/tree/master](https://github.com/yezhihao/netmc/tree/master)

## 下载方式
 * Gitee下载命令：`git clone https://gitee.com/yezhihao/netmc -b master`
 * Github下载命令：`git clone https://github.com/yezhihao/netmc -b master`

## 项目结构
```sh
└── framework
    ├── codec 编码解码
    ├── core 消息分发、处理
    └── session 消息发送和会话管理
 ```

## 使用说明

* @Endpoint，服务接入点，等价SpringMVC的 @Controller；
* @Mapping，定义消息ID，等价SpringMVC中 @RequestMapping；
* @AsyncBatch, 异步批量消息，对于并发较高的消息，如0x0200(位置信息汇报)，使用该注解，显著提升Netty和MySQL入库性能。

## 消息接入：
```java
@Endpoint
public class JT808Endpoint {

    @Autowired
    private LocationService locationService;
    
    @Autowired
    private DeviceService deviceService;

    //异步批量处理 队列大小20000 最大累积200处理一次 最大等待时间5秒
    @AsyncBatch(capacity = 20000, maxElements = 200, maxWait = 5000)
    @Mapping(types = 位置信息汇报, desc = "位置信息汇报")
    public void 位置信息汇报(List<T0200> list) {
        locationService.batchInsert(list);
    }

    @Async
    @Mapping(types = 终端注册, desc = "终端注册")
    public T8100 register(T0100 message, Session session) {
        Header header = message.getHeader();

        T8100 result = new T8100(session.nextSerialNo(), header.getMobileNo());
        result.setSerialNo(header.getSerialNo());

        String token = deviceService.register(message);
        if (token != null) {
            session.register(header);

            result.setResultCode(T8100.Success);
            result.setToken(token);
        } else {

            result.setResultCode(T8100.NotFoundTerminal);
        }
        return result;
    }
}
```

详细的例子请参考Test目录

使用该组件的项目：[https://gitee.com/yezhihao/jt808-server/tree/master](https://gitee.com/yezhihao/jt808-server/tree/master)

项目会不定期进行更新，建议star和watch一份，您的支持是我最大的动力。

如有任何疑问或者BUG，请联系我，非常感谢。

技术交流QQ群：[906230542]
