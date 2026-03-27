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

/**
 * 合同服务实现类
 *
 * @author HouseLeasingSystem开发团队
 * @description 实现租赁合同相关的所有业务逻辑，包括自动生成合同文本、风险分析、
 *              电子签署、PDF 导出和合同取消，支持中文字体的 PDF 导出
 */
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

    /**
     * 根据订单生成租赁合同，包括自动生成合同文本和风险分析
     *
     * @param request 合同生成请求（订单ID和补充条款）
     * @param userId  操作人用户 ID
     * @return 生成的合同对象
     */
    @Override
    @Transactional
    public Contract generateContract(ContractGenerateRequest request, Long userId) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 查询合同相关的房源、租客和房东信息
        House house = houseMapper.selectById(order.getHouseId());
        User tenant = userMapper.selectById(order.getTenantId());
        User landlord = userMapper.selectById(order.getLandlordId());

        // 自动生成合同正文
        String contractText = buildContractText(order, house, tenant, landlord, request.getAdditionalClauses());

        // 执行合同风险分析
        List<ContractRiskService.RiskItem> riskItems = contractRiskService.analyzeRisk(
                contractText, order.getMonthlyRent(), order.getDeposit());

        // 根据最高风险等级确定整体风险等级
        String riskLevel = "LOW";
        if (riskItems.stream().anyMatch(r -> "HIGH".equals(r.level()))) {
            riskLevel = "HIGH";
        } else if (riskItems.stream().anyMatch(r -> "MEDIUM".equals(r.level()))) {
            riskLevel = "MEDIUM";
        }

        // 将风险条目列表序列化为 JSON 存储
        String riskItemsJson;
        try {
            riskItemsJson = objectMapper.writeValueAsString(riskItems);
        } catch (Exception e) {
            riskItemsJson = "[]";
        }

        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
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
        contract.setStartDate(order.getStartDate());
        contract.setEndDate(order.getEndDate());
        contract.setMonthlyRent(order.getMonthlyRent());
        contract.setDeposit(order.getDeposit());
        contract.setCreateTime(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        contractMapper.insert(contract);
        return contract;
    }

    /**
     * 构建合同正文文本，包含双方信息、房屋信息、各类条款和补充条款
     *
     * @param order             订单对象
     * @param house             房源对象
     * @param tenant            租客用户对象
     * @param landlord          房东用户对象
     * @param additionalClauses 补充条款内容（可为空）
     * @return 格式化后的合同正文字符串
     */
    private String buildContractText(Order order, House house, User tenant, User landlord, String additionalClauses) {
        // 优先使用真实姓名，其次使用用户名，最后使用默认称谓
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

        // 如果有补充条款，添加第十条
        if (StringUtils.hasText(additionalClauses)) {
            sb.append("\n第十条 补充条款\n");
            sb.append(additionalClauses).append("\n");
        }

        sb.append("\n甲方签字：_______________    日期：_______________\n");
        sb.append("\n乙方签字：_______________    日期：_______________\n");

        return sb.toString();
    }

    /**
     * 用户签署合同，双方均签署后合同状态变为已签署并发送通知
     *
     * @param contractId 合同 ID
     * @param userId     签署人用户 ID
     * @param role       签署角色：TENANT 或 LANDLORD
     * @return 更新后的合同对象
     */
    @Override
    @Transactional
    public Contract signContract(Long contractId, Long userId, String role) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        if ("CANCELLED".equals(contract.getStatus())) {
            throw new BusinessException("合同已被取消");
        }

        // 根据角色设置对应的签署状态
        if ("TENANT".equals(role)) {
            if (!contract.getTenantId().equals(userId)) {
                throw new BusinessException(403, "无权以租客身份签署合同");
            }
            contract.setTenantSigned(true);
        } else if ("LANDLORD".equals(role)) {
            if (!contract.getLandlordId().equals(userId)) {
                throw new BusinessException(403, "无权以房东身份签署合同");
            }
            contract.setLandlordSigned(true);
        } else {
            throw new BusinessException("角色无效: " + role);
        }

        // 检查双方是否均已签署，若是则更新状态为已签署并发送通知
        if (Boolean.TRUE.equals(contract.getTenantSigned()) && Boolean.TRUE.equals(contract.getLandlordSigned())) {
            contract.setStatus("SIGNED");
            contract.setSignTime(LocalDateTime.now());
            messageProducer.sendContractStatusChange(contract.getTenantId(), "合同已签署完成");
            messageProducer.sendContractStatusChange(contract.getLandlordId(), "合同已签署完成");
        } else {
            // 只有一方签署时设置为待签署状态
            contract.setStatus("PENDING_SIGN");
        }
        contract.setUpdateTime(LocalDateTime.now());
        contractMapper.updateById(contract);
        return contract;
    }

    /**
     * 根据 ID 查询合同详情
     *
     * @param id 合同 ID
     * @return 合同详情对象
     */
    @Override
    public Contract getContractById(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        return contract;
    }

    /**
     * 查询用户的合同列表（分页）
     *
     * @param userId 用户 ID
     * @param role   角色筛选（TENANT/LANDLORD/null）
     * @param page   当前页码
     * @param size   每页大小
     * @return 分页合同列表
     */
    @Override
    public PageResult<Contract> listContracts(Long userId, String role, int page, int size) {
        Page<Contract> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        // 根据角色过滤合同
        if ("TENANT".equals(role)) {
            wrapper.eq(Contract::getTenantId, userId);
        } else if ("LANDLORD".equals(role)) {
            wrapper.eq(Contract::getLandlordId, userId);
        } else {
            // 未指定角色则查询用户作为租客或房东的所有合同
            wrapper.eq(Contract::getTenantId, userId).or().eq(Contract::getLandlordId, userId);
        }
        wrapper.orderByDesc(Contract::getCreateTime);
        Page<Contract> result = contractMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    /**
     * 将合同导出为 PDF 文件，优先使用中文字体
     *
     * @param contractId 合同 ID
     * @return PDF 文件的字节数组
     */
    @Override
    public byte[] exportPdf(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont;
            Font bodyFont;
            try {
                // 尝试使用中文字体（STSong-Light）
                BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
                titleFont = new Font(bf, 18, Font.BOLD);
                bodyFont = new Font(bf, 12);
            } catch (Exception fontEx) {
                // 中文字体不可用时降级使用默认字体
                log.warn("Chinese font not available, using default font: {}", fontEx.getMessage());
                titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
                bodyFont = new Font(Font.FontFamily.HELVETICA, 12);
            }

            // 添加合同标题
            Paragraph title = new Paragraph("House Lease Contract / 房屋租赁合同", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 逐行添加合同正文
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
            log.error("PDF 生成失败：{}", e.getMessage(), e);
            throw new BusinessException("PDF 生成失败：" + e.getMessage());
        }
    }

    /**
     * 取消合同，已签署的合同不可取消
     *
     * @param contractId 合同 ID
     * @param userId     操作人用户 ID（必须是租客或房东）
     */
    @Override
    @Transactional
    public void cancelContract(Long contractId, Long userId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // 验证操作人是否为合同当事方
        if (!contract.getTenantId().equals(userId) && !contract.getLandlordId().equals(userId)) {
            throw new BusinessException(403, "没有操作权限");
        }
        // 已签署的合同不可取消
        if ("SIGNED".equals(contract.getStatus())) {
            throw new BusinessException("已签署的合同不可取消");
        }
        contract.setStatus("CANCELLED");
        contract.setUpdateTime(LocalDateTime.now());
        contractMapper.updateById(contract);
    }

    /**
     * 生成唯一合同编号
     * 格式：HT{yyyyMMddHHmmss}{4位随机数}
     *
     * @return 合同编号字符串
     */
    private String generateContractNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 9999);
        return "HT" + timestamp + random;
    }
}
