package top.levygo.seckill.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description：秒杀信息，用于MQ发送
 * @author：LevyXie
 * @create：2022-04-14 11:00
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillMsg {
    private User user;
    private String goodsId;
}
