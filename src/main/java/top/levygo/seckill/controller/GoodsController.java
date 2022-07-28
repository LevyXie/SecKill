package top.levygo.seckill.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.service.GoodsService;
import top.levygo.seckill.service.UserService;
import top.levygo.seckill.entity.vo.DetailVo;
import top.levygo.seckill.entity.vo.GoodsVo;
import top.levygo.seckill.entity.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @description：商品列表页面的Controller
 * @author：LevyXie
 * @create：2022-04-11 21:57
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private UserService userService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    //以下为手动添加页面缓存的形式
    @RequestMapping(value = "toList",produces = "text/html;charset=utf-8")
    @ResponseBody
    public String goodsList(Model model, User user,HttpServletRequest request,HttpServletResponse response){
        //从redis中获取页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        if(!StringUtils.isEmpty(html)){//不为空，直接返回redis中查取的值
            return html;
        }

        model.addAttribute("user",user);
        List<GoodsVo> goodsVoList = goodsService.getGoodsVo();
        model.addAttribute("goodsList",goodsVoList);
        //为空时手动渲染：
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext);
        if(!StringUtils.isEmpty(html)){
            valueOperations.set("goodsList", html,1, TimeUnit.MINUTES);//设置缓存时间1min
        }
        return html;
    }

    //以下为手动添加页面缓存的形式
    @RequestMapping(value = "goodDetail/{goodsId}",produces = "text/html;charset=utf-8")
    @ResponseBody
    public String goodsDetail(Model model, User user, @PathVariable String goodsId,HttpServletRequest request,HttpServletResponse response){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodDetail:" + goodsId);
        if(!StringUtils.isEmpty(html)){
            return html;
        }
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        Date gmtStart = goodsVo.getGmtStart();
        Date gmtEnd = goodsVo.getGmtEnd();
        Date now = new Date();
        int seckillStatus;
        int remainSec;

        if(now.before(gmtStart)){//秒杀未开始
            seckillStatus = 0;
            remainSec = (int)((gmtStart.getTime() - now.getTime()) / 1000);
        }else if(now.after(gmtEnd)){//秒杀已结束
            seckillStatus = 2;
            remainSec = -1;
        }else {//秒杀进行中
            seckillStatus = 1;
            remainSec = 0;
        }
        model.addAttribute("user",user);
        model.addAttribute("goods",goodsVo);
        model.addAttribute("seckillStatus",seckillStatus);
        model.addAttribute("remainSec",remainSec);
//        return "goodsDetail";
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", webContext);
        if(!StringUtils.isEmpty(html)){
            valueOperations.set("goodDetail:" + goodsId,html, 1,TimeUnit.MINUTES);
        }
        return html;
    }

    @RequestMapping(value = "goodDetail2/{goodsId}")
    @ResponseBody
    public RespBean goodsDetail2(User user, @PathVariable String goodsId){
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        Date gmtStart = goodsVo.getGmtStart();
        Date gmtEnd = goodsVo.getGmtEnd();
        Date now = new Date();
        int seckillStatus;
        int remainSec;
        if(now.before(gmtStart)){//秒杀未开始
            seckillStatus = 0;
            remainSec = (int)((gmtStart.getTime() - now.getTime()) / 1000);
        }else if(now.after(gmtEnd)){//秒杀已结束
            seckillStatus = 2;
            remainSec = -1;
        }else {//秒杀进行中
            seckillStatus = 1;
            remainSec = 0;
        }
        DetailVo detailVo = new DetailVo();
        detailVo.setUser(user);
        detailVo.setGoodsVo(goodsVo);
        detailVo.setSeckillStatus(seckillStatus);
        detailVo.setRemainSec(remainSec);

        System.out.println(detailVo);

        return RespBean.success(detailVo);
    }
}
