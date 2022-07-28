package top.levygo.seckill.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import top.levygo.seckill.entity.Goods;
import top.levygo.seckill.mapper.GoodsMapper;
import top.levygo.seckill.service.GoodsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.levygo.seckill.entity.vo.GoodsVo;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Levy
 * @since 2022-04-12
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Override
    public List<GoodsVo> getGoodsVo() {
        return goodsMapper.getVoList();
    }

    @Override
    public GoodsVo getGoodsVoById(String id) {
        return goodsMapper.getVoById(id);
    }
}
