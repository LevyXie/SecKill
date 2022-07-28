package top.levygo.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.levygo.seckill.entity.Order;
import top.levygo.seckill.entity.SeckillGoods;
import top.levygo.seckill.entity.SeckillOrder;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.mapper.OrderMapper;
import top.levygo.seckill.mapper.SeckillGoodsMapper;
import top.levygo.seckill.mapper.SeckillOrderMapper;
import top.levygo.seckill.service.SeckillGoodsService;
import top.levygo.seckill.service.SeckillOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.levygo.seckill.entity.vo.GoodsVo;
import top.levygo.seckill.utils.MD5Util;
import top.levygo.seckill.utils.UUIDUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Levy
 * @since 2022-04-12
 */
@Service
public class SeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements SeckillOrderService {

    @Autowired
    private SeckillGoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Transactional //开启事务
    @Override
    public Order saveSecKillOrder(GoodsVo goodsVo, User user) {
        //先查后改：商品库存减一
        String id = goodsVo.getId();
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                .eq("goods_id", id));
        seckillGoods.setSeckillStock(seckillGoods.getSeckillStock() - 1);

        //解决超卖问题，库存不允许小于一
        boolean updateRes = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>()
                .setSql("seckill_stock = seckill_stock - 1")
                .eq("goods_id", goodsVo.getId())
                .gt("seckill_stock", 0)
        );

        if(!updateRes){
            //未更新成功的情况，设置空库存
            redisTemplate.opsForValue().set("isEmptyStock:" + id,"1");
            return null;
        }

        //生成订单
        Order order = new Order();
        order.setUserId(user.getId());
        order.setGoodsId(id);
        order.setGoodsCount(1);
        order.setGoodsName(goodsVo.getGoodsName());
        order.setGoodsPrice(goodsVo.getSeckillPrice());
        order.setOrderChannel(1);
        order.setDeliveryAddrId("");
        order.setStatus(0);
        order.setGoodsImg(goodsVo.getGoodsImg());
        orderMapper.insert(order);

        //生成秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(user.getId());
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setGoodsId(goodsVo.getId());
        seckillOrderMapper.insert(seckillOrder);
        //订单存入redis
        redisTemplate.opsForValue().set("order:" + user.getId() + goodsVo.getId(), seckillOrder);
        return order;
    }

    @Override
    public String getKillResult(User user, String goodId) {
        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        wrapper.eq("goods_id", goodId);
        SeckillOrder order = baseMapper.selectOne(wrapper);
        if(null != order){
            return order.getOrderId();
        }else if(redisTemplate.hasKey("isEmptyStock:" + goodId)){
            return "-1";
        }else {
            return "0";
        }
    }

    @Override
    public String createPath(User user, String goodId) {
        String path = MD5Util.md5(UUIDUtil.uuid() + "123456");//生成接口地址
        //存入数据库，设置超时时间60s
        redisTemplate.opsForValue().set("seckillPath:" + user.getId() + ":" + goodId, path,60, TimeUnit.SECONDS);
        return path;
    }

    //校验秒杀地址
    @Override
    public boolean checkPath(User user, String goodId, String path) {
        if(null == user || StringUtils.isEmpty(path)){
            return false;
        }
        String redisPath = (String) redisTemplate.opsForValue().get("seckillPath:" + user.getId() + ":" + goodId);
        if(path.equals(redisPath)){
            return true;
        }
        return false;
    }

}
