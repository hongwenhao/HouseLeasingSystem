package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.ContractGenerateRequest;
import com.houseleasing.entity.Contract;

public interface ContractService {
    Contract generateContract(ContractGenerateRequest request, Long userId);
    Contract signContract(Long contractId, Long userId, String role);
    Contract getContractById(Long id);
    PageResult<Contract> listContracts(Long userId, String role, int page, int size);
    byte[] exportPdf(Long contractId);
    void cancelContract(Long contractId, Long userId);
}
