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

@Tag(name = "Contract", description = "Contract management")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ContractController {

    private final ContractService contractService;
    private final ContractRiskService contractRiskService;
    private final UserMapper userMapper;

    @Operation(summary = "Generate contract from order")
    @PostMapping("/generate")
    public Result<Contract> generateContract(@RequestBody ContractGenerateRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(contractService.generateContract(request, user.getId()));
    }

    @Operation(summary = "Sign contract")
    @PostMapping("/{id}/sign")
    public Result<Contract> signContract(@PathVariable Long id,
                                          @RequestBody Map<String, String> request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        String role = request.get("role");
        return Result.success(contractService.signContract(id, user.getId(), role));
    }

    @Operation(summary = "Get contract detail")
    @GetMapping("/{id}")
    public Result<Contract> getContractById(@PathVariable Long id) {
        return Result.success(contractService.getContractById(id));
    }

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

    @Operation(summary = "Download contract PDF")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] pdfBytes = contractService.exportPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contract-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @Operation(summary = "Run risk analysis on contract")
    @PostMapping("/{id}/risk-check")
    public Result<List<ContractRiskService.RiskItem>> riskCheck(@PathVariable Long id) {
        Contract contract = contractService.getContractById(id);
        List<ContractRiskService.RiskItem> risks = contractRiskService.analyzeRisk(
                contract.getContent(), null, null);
        return Result.success(risks);
    }

    @Operation(summary = "Cancel contract")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelContract(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        contractService.cancelContract(id, user.getId());
        return Result.success();
    }

    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }
}
