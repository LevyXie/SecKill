package top.levygo.seckill.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import top.levygo.seckill.entity.Order;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.service.OrderService;
import top.levygo.seckill.entity.vo.RespBean;
import top.levygo.seckill.entity.vo.RespBeanEnum;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Levy
 * @since 2022-04-12
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/detail")
    @ResponseBody
    public RespBean getOrderDetail(User user,String id){
        if(null == user){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        Order order = orderService.getById(id);
        if(null == order){
            return RespBean.error(RespBeanEnum.ORDER_ERROR);
        }
        return RespBean.success(order);
    }
}

