package top.levygo.seckill.validator;

import org.springframework.util.StringUtils;
import top.levygo.seckill.utils.ValidatorUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @description：手机号校验规则
 * @author：LevyXie
 * @create：2022-04-11 17:53
 */
public class IsMobileValidator implements ConstraintValidator<IsMobile,String> {
    private boolean required = false;

    @Override
    public void initialize(IsMobile constraintAnnotation) {
        required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if(required){//必填情况下
            return ValidatorUtil.isMobile(value);
        }else{//非必填情况下
            if(StringUtils.isEmpty(value)){
                return true;
            }else {
                return ValidatorUtil.isMobile(value);
            }
        }
    }
}
