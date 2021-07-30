package org.jeecg.modules.ltemai.order.service;

import org.jeecg.modules.ltemai.order.entity.CesOrderGoods;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: 订单商品
 * @Author: jeecg-boot
 * @Date:   2021-07-30
 * @Version: V1.0
 */
public interface ICesOrderGoodsService extends IService<CesOrderGoods> {

	public List<CesOrderGoods> selectByMainId(String mainId);
}
