package top.levygo.seckill.service;

import top.levygo.seckill.entity.Goods;
import com.baomidou.mybatisplus.extension.service.IService;
import top.levygo.seckill.entity.vo.GoodsVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Levy
 * @since 2022-04-12
 */
public interface GoodsService extends IService<Goods> {

    List<GoodsVo> getGoodsVo();

    GoodsVo getGoodsVoById(String id);
}
