package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.ContractGenerateRequest;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.House;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.ContractRiskService;
import com.houseleasing.service.ContractService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractMapper contractMapper;
    private final OrderMapper orderMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final ContractRiskService contractRiskService;
    private final MessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Contract generateContract(ContractGenerateRequest request, Long userId) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        House house = houseMapper.selectById(order.getHouseId());
        User tenant = userMapper.selectById(order.getTenantId());
        User landlord = userMapper.selectById(order.getLandlordId());

        String contractText = buildContractText(order, house, tenant, landlord, request.getAdditionalClauses());

        // Run risk analysis
        List<ContractRiskService.RiskItem> riskItems = contractRiskService.analyzeRisk(
                contractText, order.getMonthlyRent(), order.getDeposit());

        String riskLevel = "LOW";
        if (riskItems.stream().anyMatch(r -> "HIGH".equals(r.level()))) {
            riskLevel = "HIGH";
        } else if (riskItems.stream().anyMatch(r -> "MEDIUM".equals(r.level()))) {
            riskLevel = "MEDIUM";
        }

        String riskItemsJson;
        try {
            riskItemsJson = objectMapper.writeValueAsString(riskItems);
        } catch (Exception e) {
            riskItemsJson = "[]";
        }

        Contract contract = new Contract();
        contract.setOrderId(order.getId());
        contract.setHouseId(order.getHouseId());
        contract.setTenantId(order.getTenantId());
        contract.setLandlordId(order.getLandlordId());
        contract.setContent(contractText);
        contract.setStatus("DRAFT");
        contract.setRiskLevel(riskLevel);
        contract.setRiskItems(riskItemsJson);
        contract.setTenantSigned(false);
        contract.setLandlordSigned(false);
        contract.setCreateTime(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        contractMapper.insert(contract);
        return contract;
    }

    private String buildContractText(Order order, House house, User tenant, User landlord, String additionalClauses) {
        String tenantName = tenant != null && StringUtils.hasText(tenant.getRealName())
                ? tenant.getRealName() : (tenant != null ? tenant.getUsername() : "租客");
        String landlordName = landlord != null && StringUtils.hasText(landlord.getRealName())
                ? landlord.getRealName() : (landlord != null ? landlord.getUsername() : "房东");
        String houseName = house != null ? house.getTitle() : "租赁房屋";
        String houseAddress = house != null ? house.getAddress() : "";
        BigDecimal monthlyRent = order.getMonthlyRent() != null ? order.getMonthlyRent() : BigDecimal.ZERO;
        BigDecimal deposit = order.getDeposit() != null ? order.getDeposit() : BigDecimal.ZERO;
        String startDate = order.getStartDate() != null ? order.getStartDate().toString() : "";
        String endDate = order.getEndDate() != null ? order.getEndDate().toString() : "";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        StringBuilder sb = new StringBuilder();
        sb.append("房屋租赁合同\n\n");
        sb.append("合同编号：HLS-").append(order.getId()).append("-").append(System.currentTimeMillis()).append("\n");
        sb.append("签署日期：").append(now).append("\n\n");

        sb.append("甲方（出租方/房东）：").append(landlordName).append("\n");
        if (landlord != null && StringUtils.hasText(landlord.getIdCard())) {
            sb.append("身份证号：").append(landlord.getIdCard()).append("\n");
        }
        if (landlord != null && StringUtils.hasText(landlord.getPhone())) {
            sb.append("联系电话：").append(landlord.getPhone()).append("\n");
        }
        sb.append("\n");
        sb.append("乙方（承租方/租客）：").append(tenantName).append("\n");
        if (tenant != null && StringUtils.hasText(tenant.getIdCard())) {
            sb.append("身份证号：").append(tenant.getIdCard()).append("\n");
        }
        if (tenant != null && StringUtils.hasText(tenant.getPhone())) {
            sb.append("联系电话：").append(tenant.getPhone()).append("\n");
        }
        sb.append("\n");

        sb.append("第一条 房屋基本信息\n");
        sb.append("1.1 房屋名称：").append(houseName).append("\n");
        sb.append("1.2 房屋地址：").append(houseAddress).append("\n");
        if (house != null) {
            sb.append("1.3 房屋面积：").append(house.getArea()).append("平方米\n");
            sb.append("1.4 房屋类型：").append(house.getHouseType()).append("\n");
        }
        sb.append("\n");

        sb.append("第二条 租赁期限\n");
        sb.append("2.1 租赁期限自").append(startDate).append("至").append(endDate).append("。\n");
        sb.append("2.2 租赁期满，甲方有权收回房屋，乙方应如期交还房屋。\n\n");

        sb.append("第三条 租金及付款方式\n");
        sb.append("3.1 月租金：人民币").append(monthlyRent).append("元整。\n");
        sb.append("3.2 租金支付方式：每月1日前支付当月租金。\n");
        sb.append("3.3 支付方式：银行转账或其他双方协商方式。\n\n");

        sb.append("第四条 押金条款\n");
        sb.append("4.1 乙方在签订合同时向甲方缴纳押金：人民币").append(deposit).append("元整。\n");
        sb.append("4.2 租赁期满，乙方按时交还房屋并结清所有费用后，甲方应在7个工作日内退还押金。\n");
        sb.append("4.3 若乙方违约，甲方有权从押金中扣除相应费用。\n\n");

        sb.append("第五条 维修责任\n");
        sb.append("5.1 房屋主体结构及配套设施的维修由甲方负责。\n");
        sb.append("5.2 因乙方使用不当造成的损坏，由乙方负责修缮或赔偿。\n");
        sb.append("5.3 乙方在租赁期间发现房屋设施损坏，应及时通知甲方进行维修。\n\n");

        sb.append("第六条 水电费标准\n");
        sb.append("6.1 水费：按实际用量计费，依据水表读数结算。\n");
        sb.append("6.2 电费：按实际用量计费，依据电表读数结算。\n");
        sb.append("6.3 网络费用：由乙方自行负担。\n\n");

        sb.append("第七条 提前解约条款\n");
        sb.append("7.1 乙方提前解约须提前30日书面通知甲方。\n");
        sb.append("7.2 甲方提前终止合同须提前60日书面通知乙方。\n");
        sb.append("7.3 因不可抗力导致合同无法继续履行，双方可协商解约。\n\n");

        sb.append("第八条 违约责任\n");
        sb.append("8.1 乙方未按时支付租金，每逾期一日按月租金的0.5%支付违约金。\n");
        sb.append("8.2 乙方擅自转租房屋，甲方有权解除合同并要求赔偿损失。\n");
        sb.append("8.3 甲方在租赁期间无故收回房屋，应赔偿乙方相当于一个月租金的违约金。\n\n");

        sb.append("第九条 其他约定\n");
        sb.append("9.1 乙方不得擅自改变房屋结构或用途。\n");
        sb.append("9.2 乙方应遵守物业管理规定及小区相关规定。\n");
        sb.append("9.3 本合同一式两份，甲乙双方各执一份，具有同等法律效力。\n");
        sb.append("9.4 本合同未尽事宜，双方协商解决；协商不成的，提交有管辖权的法院裁决。\n");

        if (StringUtils.hasText(additionalClauses)) {
            sb.append("\n第十条 补充条款\n");
            sb.append(additionalClauses).append("\n");
        }

        sb.append("\n甲方签字：_______________    日期：_______________\n");
        sb.append("\n乙方签字：_______________    日期：_______________\n");

        return sb.toString();
    }

    @Override
    @Transactional
    public Contract signContract(Long contractId, Long userId, String role) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "Contract not found");
        }
        if ("CANCELLED".equals(contract.getStatus())) {
            throw new BusinessException("Contract is already cancelled");
        }

        if ("TENANT".equals(role)) {
            if (!contract.getTenantId().equals(userId)) {
                throw new BusinessException(403, "Not authorized to sign as tenant");
            }
            contract.setTenantSigned(true);
        } else if ("LANDLORD".equals(role)) {
            if (!contract.getLandlordId().equals(userId)) {
                throw new BusinessException(403, "Not authorized to sign as landlord");
            }
            contract.setLandlordSigned(true);
        } else {
            throw new BusinessException("Invalid role: " + role);
        }

        if (Boolean.TRUE.equals(contract.getTenantSigned()) && Boolean.TRUE.equals(contract.getLandlordSigned())) {
            contract.setStatus("SIGNED");
            contract.setSignTime(LocalDateTime.now());
            messageProducer.sendContractStatusChange(contract.getTenantId(), "合同已签署完成");
            messageProducer.sendContractStatusChange(contract.getLandlordId(), "合同已签署完成");
        } else {
            contract.setStatus("PENDING_SIGN");
        }
        contract.setUpdateTime(LocalDateTime.now());
        contractMapper.updateById(contract);
        return contract;
    }

    @Override
    public Contract getContractById(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(404, "Contract not found");
        }
        return contract;
    }

    @Override
    public PageResult<Contract> listContracts(Long userId, String role, int page, int size) {
        Page<Contract> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        if ("TENANT".equals(role)) {
            wrapper.eq(Contract::getTenantId, userId);
        } else if ("LANDLORD".equals(role)) {
            wrapper.eq(Contract::getLandlordId, userId);
        } else {
            wrapper.eq(Contract::getTenantId, userId).or().eq(Contract::getLandlordId, userId);
        }
        wrapper.orderByDesc(Contract::getCreateTime);
        Page<Contract> result = contractMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    @Override
    public byte[] exportPdf(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "Contract not found");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont;
            Font bodyFont;
            try {
                BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
                titleFont = new Font(bf, 18, Font.BOLD);
                bodyFont = new Font(bf, 12);
            } catch (Exception fontEx) {
                log.warn("Chinese font not available, using default font: {}", fontEx.getMessage());
                titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
                bodyFont = new Font(Font.FontFamily.HELVETICA, 12);
            }

            Paragraph title = new Paragraph("House Lease Contract / 房屋租赁合同", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            String content = contract.getContent() != null ? contract.getContent() : "";
            String[] lines = content.split("\n");
            for (String line : lines) {
                Paragraph para = new Paragraph(line, bodyFont);
                para.setSpacingAfter(4);
                document.add(para);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage(), e);
            throw new BusinessException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancelContract(Long contractId, Long userId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "Contract not found");
        }
        if (!contract.getTenantId().equals(userId) && !contract.getLandlordId().equals(userId)) {
            throw new BusinessException(403, "Not authorized");
        }
        if ("SIGNED".equals(contract.getStatus())) {
            throw new BusinessException("Cannot cancel a signed contract");
        }
        contract.setStatus("CANCELLED");
        contract.setUpdateTime(LocalDateTime.now());
        contractMapper.updateById(contract);
    }
}
