package top.levygo.seckill.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.levygo.seckill.entity.User;

/**
 * @description：
 * @author：LevyXie
 * @create：2022-04-13 16:19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailVo {
    private User user;
    private GoodsVo goodsVo;
    private int seckillStatus;
    private int remainSec;
}
