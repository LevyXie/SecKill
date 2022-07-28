package top.levygo.seckill.entity.vo;

import lombok.Data;
import top.levygo.seckill.entity.Goods;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @description：
 * @author：LevyXie
 * @create：2022-04-12 10:26
 */
@Data
public class GoodsVo extends Goods {
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Date gmtStart;
    private Date gmtEnd;
}
