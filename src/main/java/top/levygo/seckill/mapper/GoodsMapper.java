package top.levygo.seckill.mapper;

import top.levygo.seckill.entity.Goods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.levygo.seckill.entity.vo.GoodsVo;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Levy
 * @since 2022-04-12
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    List<GoodsVo> getVoList();

    GoodsVo getVoById(String id);
}
