package org.jeecg.modules.ltemai.order.mapper;

import java.util.List;
import org.jeecg.modules.ltemai.order.entity.CesOrderGoods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: 订单商品
 * @Author: jeecg-boot
 * @Date:   2021-07-30
 * @Version: V1.0
 */
public interface CesOrderGoodsMapper extends BaseMapper<CesOrderGoods> {

	public boolean deleteByMainId(@Param("mainId") String mainId);
    
	public List<CesOrderGoods> selectByMainId(@Param("mainId") String mainId);
}
