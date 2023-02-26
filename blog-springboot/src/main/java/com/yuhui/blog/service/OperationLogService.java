package com.yuhui.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuhui.blog.dto.OperationLogDTO;
import com.yuhui.blog.vo.PageResult;
import com.yuhui.blog.entity.OperationLog;
import com.yuhui.blog.vo.ConditionVO;

/**
 * 操作日志服务
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 查询日志列表
     *
     * @param conditionVO 条件
     * @return 日志列表
     */
    PageResult<OperationLogDTO> listOperationLogs(ConditionVO conditionVO);

}
