package site.aronnax.service;

import java.util.List;
import java.util.Map;

/**
 * 业主管理业务接口
 * 处理业主档案检索及房产关联关系的维护。
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public interface OwnerService {

    /**
     * 多维度搜索业主
     * 支持通过姓名、手机号或具体房间号进行模糊匹配。
     *
     * @param keyword 搜索关键字
     * @return 匹配的业主详情列表（含关联房产信息）
     */
    List<Map<String, Object>> searchOwners(String keyword);

    /**
     * 提取业主及其名下所有房产的完整视图
     *
     * @param userId 用户 ID
     * @return 包含基本资料及房产列表的映射对象
     */
    Map<String, Object> getOwnerWithProperties(Long userId);

    /**
     * 更新房产归属（房产变更/过户）
     * 核心资产管理逻辑，改变指定房产档案的关联用户 ID。
     *
     * @param propertyId 房产 ID
     * @param newOwnerId 新业主的用户 ID
     * @return 是否变更成功
     */
    boolean updatePropertyOwner(Long propertyId, Long newOwnerId);
}
