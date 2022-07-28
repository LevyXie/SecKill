package top.levygo.seckill.service.impl;

import top.levygo.seckill.entity.Order;
import top.levygo.seckill.mapper.OrderMapper;
import top.levygo.seckill.service.OrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Levy
 * @since 2022-04-12
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

}
