package com.houseleasing.controller;

import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.ContractGenerateRequest;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.ContractRiskService;
import com.houseleasing.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 合同管理控制器
 *
 * @author HouseLeasingSystem开发团队
 * @description 提供租赁合同相关的 REST API，包括合同生成、签署、查询、PDF 导出、
 *              风险分析和取消，所有接口均需要 JWT 认证
 */
@Tag(name = "Contract", description = "Contract management")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ContractController {

    private final ContractService contractService;
    private final ContractRiskService contractRiskService;
    private final UserMapper userMapper;

    /**
     * 根据订单生成租赁合同（包含自动风险分析）
     *
     * @param request     合同生成请求（订单ID和补充条款）
     * @param userDetails 当前登录用户信息
     * @return 生成的合同对象
     */
    @Operation(summary = "Generate contract from order")
    @PostMapping("/generate")
    public Result<Contract> generateContract(@RequestBody ContractGenerateRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(contractService.generateContract(request, user.getId()));
    }

    /**
     * 用户对合同进行电子签署
     *
     * @param id          合同 ID
     * @param request     请求体，包含 role 字段（TENANT 或 LANDLORD）
     * @param userDetails 当前登录用户信息
     * @return 签署后的合同对象
     */
    @Operation(summary = "Sign contract")
    @PostMapping("/{id}/sign")
    public Result<Contract> signContract(@PathVariable Long id,
                                          @RequestBody Map<String, String> request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        String role = request.get("role");
        return Result.success(contractService.signContract(id, user.getId(), role));
    }

    /**
     * 根据合同 ID 查询合同详情
     *
     * @param id 合同 ID
     * @return 合同详情
     */
    @Operation(summary = "Get contract detail")
    @GetMapping("/{id}")
    public Result<Contract> getContractById(@PathVariable Long id) {
        return Result.success(contractService.getContractById(id));
    }

    /**
     * 查询当前用户的合同列表（分页）
     *
     * @param userDetails 当前登录用户信息
     * @param role        角色筛选（TENANT/LANDLORD，可选）
     * @param page        当前页码
     * @param size        每页大小
     * @return 分页合同列表
     */
    @Operation(summary = "List user's contracts")
    @GetMapping
    public Result<PageResult<Contract>> listContracts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(contractService.listContracts(user.getId(), role, page, size));
    }

    /**
     * 下载合同的 PDF 文件
     *
     * @param id 合同 ID
     * @return PDF 文件的字节流响应
     */
    @Operation(summary = "Download contract PDF")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] pdfBytes = contractService.exportPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contract-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * 对指定合同执行风险分析并返回风险条目
     *
     * @param id 合同 ID
     * @return 风险分析结果列表
     */
    @Operation(summary = "Run risk analysis on contract")
    @PostMapping("/{id}/risk-check")
    public Result<List<ContractRiskService.RiskItem>> riskCheck(@PathVariable Long id) {
        Contract contract = contractService.getContractById(id);
        List<ContractRiskService.RiskItem> risks = contractRiskService.analyzeRisk(
                contract.getContent(), null, null);
        return Result.success(risks);
    }

    /**
     * 取消指定合同（仅限草稿或待签署状态）
     *
     * @param id          合同 ID
     * @param userDetails 当前登录用户信息（必须是合同当事方）
     * @return 操作成功的响应
     */
    @Operation(summary = "Cancel contract")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelContract(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        contractService.cancelContract(id, user.getId());
        return Result.success();
    }

    /**
     * 根据用户名解析用户信息
     *
     * @param username 用户名
     * @return 对应的用户实体
     */
    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }
}
