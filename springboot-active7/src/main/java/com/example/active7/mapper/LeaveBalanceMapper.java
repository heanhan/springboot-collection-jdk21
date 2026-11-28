package com.example.active7.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.active7.model.LeaveBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface LeaveBalanceMapper extends BaseMapper<LeaveBalance> {

    @Update("UPDATE leave_balance SET used_days = used_days + #{days} "
            + "WHERE user_id = #{userId} AND balance_year = #{year} "
            + "AND used_days + #{days} <= annual_days")
    int addUsedDays(@Param("userId") String userId, @Param("year") int year,
                    @Param("days") BigDecimal days);
}
