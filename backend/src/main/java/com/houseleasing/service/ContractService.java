package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.ContractGenerateRequest;
import com.houseleasing.entity.Contract;

/**
 * 合同服务接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 定义租赁合同相关的业务操作，包括生成合同、签署、查询、导出和取消
 */
public interface ContractService {

    /**
     * 根据订单生成租赁合同（自动进行风险分析）
     *
     * @param request 包含订单 ID 和补充条款的请求对象
     * @param userId  操作人用户 ID
     * @return 生成的合同对象（含风险分析结果）
     */
    Contract generateContract(ContractGenerateRequest request, Long userId);

    /**
     * 用户对合同进行电子签署
     *
     * @param contractId 要签署的合同 ID
     * @param userId     签署人用户 ID
     * @param role       签署角色：TENANT（租客）或 LANDLORD（房东）
     * @return 签署后的合同对象
     */
    Contract signContract(Long contractId, Long userId, String role);

    /**
     * 根据合同 ID 查询合同详情
     *
     * @param id 合同 ID
     * @return 合同详情对象
     */
    Contract getContractById(Long id);

    /**
     * 查询用户的合同列表（分页）
     *
     * @param userId 用户 ID
     * @param role   查询角色：TENANT（作为租客）、LANDLORD（作为房东），为 null 时查询双方
     * @param page   当前页码
     * @param size   每页大小
     * @return 该用户的分页合同列表
     */
    PageResult<Contract> listContracts(Long userId, String role, int page, int size);

    /**
     * 导出合同为 PDF 文件
     *
     * @param contractId 要导出的合同 ID
     * @return PDF 文件的字节数组
     */
    byte[] exportPdf(Long contractId);

    /**
     * 取消指定合同（仅限草稿或待签署状态）
     *
     * @param contractId 要取消的合同 ID
     * @param userId     操作人用户 ID（必须是合同的租客或房东）
     */
    void cancelContract(Long contractId, Long userId);
}
