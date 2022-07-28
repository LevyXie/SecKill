package top.levygo.seckill.service;

import top.levygo.seckill.entity.Order;
import top.levygo.seckill.entity.SeckillOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.entity.vo.GoodsVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Levy
 * @since 2022-04-12
 */
public interface SeckillOrderService extends IService<SeckillOrder> {

    Order saveSecKillOrder(GoodsVo goodsVo, User user);

    String getKillResult(User user, String goodId);

    String createPath(User user, String goodId);

    boolean checkPath(User user, String goodId, String path);
}
