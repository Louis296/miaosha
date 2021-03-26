## 秒杀系统模型
### 主要采用的技术
* 通过乐观锁和悲观锁防止超卖
* 基于令牌桶的阻塞或超时算法进行用户限流
* 通过md5加盐生成校验码的方式对抢购接口进行隐藏，防止抢购脚本
* 通过记录单个用户抢购频率并限制的方式杜绝抢购脚本
* 基于Redis实现热点数据的缓存（以库存为例）
* 使用缓时双删的方式实现缓存和数据库的最终一致性
* 使用rabbitmq消息队列实现删除缓存的失败重试机制
* 使用rabbitmq消息队列实现订单的异步处理