package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.common.security.IdCardCryptoService;
import com.houseleasing.dto.ContractGenerateRequest;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.House;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.User;
import com.houseleasing.activiti.WorkflowService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 合同服务实现类
 *
 * @author hongwenhao
 * @description 实现租赁合同相关的所有业务逻辑，包括自动生成合同文本、风险分析、
 *              电子签署、PDF 导出（落盘保存路径）和合同取消，支持中文字体的 PDF 导出
 */
@Slf4j
@Service // 声明为合同业务服务
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService { // 合同全流程实现：生成、签署、导出、取消

    /** PDF 文件存储子目录，位于上传根目录下的 contracts/ 文件夹 */
    private static final String CONTRACT_PDF_SUBDIR = "contracts"; // 统一合同PDF子目录名，避免散落硬编码

    /** 从 application.yml 读取文件上传根目录，默认为 uploads */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir; // 这里执行当前语句的核心处理

    /** 固定正文条款数量（不含补充条款） */
    private static final int BASE_ARTICLE_COUNT = 13; // 按既定流程继续处理后续逻辑
    /** 租金逾期违约金比例（千分比，0.5%/日） */
    private static final BigDecimal DAILY_LATE_FEE_RATE_PERCENT = new BigDecimal("0.5"); // 创建对象承载本步骤数据
    /** 租金逾期达到该天数后，出租方可按约解除合同 */
    private static final int RENT_OVERDUE_TERMINATION_DAYS = 15; // 在当前步骤完成必要业务动作
    /** PDF 导出可选中文字体路径（按优先级降级） */
    private static final String[] CJK_FONT_CANDIDATES = new String[] { // 实例化新对象用于后续操作
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0",
            "/usr/share/fonts/truetype/arphic/uming.ttc,0",
            "/System/Library/Fonts/PingFang.ttc,0",
            "/System/Library/Fonts/Hiragino Sans GB.ttc,0",
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simhei.ttf"
    };

    private final WorkflowService workflowService; // 工作流服务（驱动合同签署流程）
    private final ContractMapper contractMapper; // 合同表访问组件
    private final OrderMapper orderMapper; // 订单表访问组件
    private final HouseMapper houseMapper; // 房源表访问组件
    private final UserMapper userMapper; // 用户表访问组件
    private final ContractRiskService contractRiskService; // 合同风控服务
    private final MessageProducer messageProducer; // MQ 消息发送组件
    private final ObjectMapper objectMapper; // JSON 序列化组件
    private final IdCardCryptoService idCardCryptoService; // 身份证加解密服务

    /**
     * 根据订单生成租赁合同，包括自动生成合同文本和风险分析
     *
     * @param request 合同生成请求（订单ID和补充条款）
     * @param userId  操作人用户 ID
     * @return 生成的合同对象
     */
    @Override
    @Transactional
    public Contract generateContract(ContractGenerateRequest request, Long userId) { // 根据订单生成合同并做风险分析
        Order order = orderMapper.selectById(request.getOrderId()); // 根据请求中的订单ID加载订单主数据
        if (order == null) { // 依据当前状态决定后续处理路径
            throw new BusinessException(404, "订单不存在"); // 以异常形式提示调用方当前问题
        }
        if (request.getStartDate() == null || request.getEndDate() == null) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "请填写完整的租赁起止日期"); // 抛出业务异常并中断当前流程
        }
        if (request.getEndDate().isBefore(request.getStartDate())) { // 依据当前状态决定后续处理路径
            throw new BusinessException(400, "租赁结束日期不能早于起始日期"); // 抛出业务异常并中断当前流程
        }
        // 生成合同时以房东确认的租赁期限为准，同时回写订单，保证后续详情与合同正文一致
        order.setStartDate(request.getStartDate()); // 回写订单起租日期，确保订单与合同一致
        order.setEndDate(request.getEndDate()); // 回写订单止租日期，避免后续页面展示不一致
        order.setUpdateTime(LocalDateTime.now()); // 更新订单修改时间用于审计
        orderMapper.updateById(order); // 持久化订单租期变更
        // 查询合同相关的房源、租客和房东信息
        House house = houseMapper.selectById(order.getHouseId()); // 加载合同关联房源信息
        User tenant = userMapper.selectById(order.getTenantId()); // 加载租客实名信息
        User landlord = userMapper.selectById(order.getLandlordId()); // 加载房东实名信息
        if (tenant == null || landlord == null) { // 依据当前状态决定后续处理路径
            throw new BusinessException(404, "合同关联用户不存在"); // 立即返回错误避免继续执行
        }
        if (!Boolean.TRUE.equals(tenant.getIsRealNameAuth()) || !Boolean.TRUE.equals(landlord.getIsRealNameAuth())) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(403, "双方需完成实名认证后才能生成合同"); // 以异常形式提示调用方当前问题
        }
        if (!StringUtils.hasText(tenant.getRealName()) || !StringUtils.hasText(landlord.getRealName())) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(400, "实名认证信息不完整，无法生成合同"); // 立即返回错误避免继续执行
        }

        // 自动生成合同正文
        String contractText = buildContractText(order, house, tenant, landlord, request.getAdditionalClauses()); // 借助已有方法完成该业务动作

        // 执行合同风险分析
        List<ContractRiskService.RiskItem> riskItems = contractRiskService.analyzeRisk(
                contractText, order.getMonthlyRent(), order.getDeposit()); // 执行对应服务/DAO方法推进流程

        // 根据最高风险等级确定整体风险等级
        String riskLevel = "LOW"; // 在当前步骤完成必要业务动作
        if (riskItems.stream().anyMatch(r -> "HIGH".equals(r.level()))) { // 依据当前状态决定后续处理路径
            riskLevel = "HIGH"; // 在当前步骤完成必要业务动作
        } else if (riskItems.stream().anyMatch(r -> "MEDIUM".equals(r.level()))) { // 执行对应服务/DAO方法推进流程
            riskLevel = "MEDIUM"; // 在当前步骤完成必要业务动作
        }

        // 将风险条目列表序列化为 JSON 存储
        String riskItemsJson; // 在当前步骤完成必要业务动作
        try { // 在当前步骤完成必要业务动作
            riskItemsJson = objectMapper.writeValueAsString(riskItems); // 调用组件能力完成当前步骤
        } catch (Exception e) { // 在当前步骤完成必要业务动作
            riskItemsJson = "[]"; // 这里执行当前语句的核心处理
        }

        Contract contract = new Contract(); // 初始化对象以便填充业务字段
        contract.setContractNo(generateContractNo()); // 给对象写入当前步骤需要的字段值
        contract.setOrderId(order.getId()); // 补齐对象属性供后续流程使用
        contract.setHouseId(order.getHouseId()); // 给对象写入当前步骤需要的字段值
        contract.setTenantId(order.getTenantId()); // 补齐对象属性供后续流程使用
        contract.setLandlordId(order.getLandlordId()); // 补齐对象属性供后续流程使用
        contract.setContent(contractText); // 补齐对象属性供后续流程使用
        contract.setStatus("DRAFT"); // 把变更结果同步到数据库
        contract.setRiskLevel(riskLevel); // 补齐对象属性供后续流程使用
        contract.setRiskItems(riskItemsJson); // 给对象写入当前步骤需要的字段值
        contract.setTenantSigned(false); // 补齐对象属性供后续流程使用
        contract.setLandlordSigned(false); // 给对象写入当前步骤需要的字段值
        contract.setStartDate(order.getStartDate()); // 给对象写入当前步骤需要的字段值
        contract.setEndDate(order.getEndDate()); // 补齐对象属性供后续流程使用
        contract.setMonthlyRent(order.getMonthlyRent()); // 补齐对象属性供后续流程使用
        contract.setDeposit(order.getDeposit()); // 补齐对象属性供后续流程使用
        contract.setCreateTime(LocalDateTime.now()); // 补齐对象属性供后续流程使用
        contract.setUpdateTime(LocalDateTime.now()); // 把变更结果同步到数据库
        contractMapper.insert(contract); // 持久化新增内容便于后续追踪
        // 启动合同签署流程并回写流程实例 ID
        String processInstanceId = workflowService.startContractSigningProcess(
                contract.getId(), contract.getTenantId(), contract.getLandlordId()); // 调用组件能力完成当前步骤
        contract.setWorkflowInstanceId(processInstanceId); // 补齐对象属性供后续流程使用
        contractMapper.updateById(contract); // 调用组件能力完成当前步骤

        // 合同生成后将对应房源设为下架状态（OFFLINE），防止同一房源被重复预订
        // 房源在合同取消时会自动恢复上架（见 cancelContract 方法）
        if (house != null) { // 依据当前状态决定后续处理路径
            house.setStatus("OFFLINE"); // 把变更结果同步到数据库
            houseMapper.updateById(house); // 调用组件能力完成当前步骤
            log.info("合同 {} 生成，房源 {} 已自动下架", contract.getContractNo(), house.getId()); // 调用组件能力完成当前步骤
        }

        return contract; // 返回当前阶段的处理结果
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
    private String buildContractText(Order order, House house, User tenant, User landlord, String additionalClauses) { // 借助已有方法完成该业务动作
        // 合同生成前已校验双方实名认证完整，这里直接使用实名
        String tenantName = tenant.getRealName(); // 调用组件能力完成当前步骤
        String landlordName = landlord.getRealName(); // 执行对应服务/DAO方法推进流程
        String houseName = house != null ? house.getTitle() : "租赁房屋"; // 执行对应服务/DAO方法推进流程
        String houseAddress = house != null ? house.getAddress() : ""; // 调用组件能力完成当前步骤
        BigDecimal monthlyRent = order.getMonthlyRent() != null ? order.getMonthlyRent() : BigDecimal.ZERO; // 执行对应服务/DAO方法推进流程
        BigDecimal deposit = order.getDeposit() != null ? order.getDeposit() : BigDecimal.ZERO; // 执行对应服务/DAO方法推进流程
        String startDate = order.getStartDate() != null ? order.getStartDate().toString() : ""; // 借助已有方法完成该业务动作
        String endDate = order.getEndDate() != null ? order.getEndDate().toString() : ""; // 借助已有方法完成该业务动作
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")); // 借助已有方法完成该业务动作
        // users.id_card 改为密文存储后，合同文本中若需展示证件号，必须先解密再脱敏。
        String landlordIdCard = maskIdCard(landlord != null ? idCardCryptoService.decryptFromStorage(landlord.getIdCard()) : null); // 借助已有方法完成该业务动作
        String tenantIdCard = maskIdCard(tenant != null ? idCardCryptoService.decryptFromStorage(tenant.getIdCard()) : null); // 调用组件能力完成当前步骤
        String waterRule = buildFeeRule(house != null ? house.getWaterFeeType() : null, house != null ? house.getWaterFee() : null, "元/吨"); // 借助已有方法完成该业务动作
        String electricRule = buildFeeRule(house != null ? house.getElectricFeeType() : null, house != null ? house.getElectricFee() : null, "元/度"); // 借助已有方法完成该业务动作
        String gasRule = buildFeeRule(house != null ? house.getGasFeeType() : null, house != null ? house.getGasFee() : null, "元/方"); // 调用组件能力完成当前步骤
        String propertyRule = buildFeeRule(house != null ? house.getPropertyFeeType() : null, house != null ? house.getPropertyFee() : null, "元/月"); // 执行对应服务/DAO方法推进流程
        String internetRule = buildFeeRule(house != null ? house.getInternetFeeType() : null, house != null ? house.getInternetFee() : null, "元/月"); // 借助已有方法完成该业务动作

        StringBuilder sb = new StringBuilder(); // 初始化对象以便填充业务字段
        sb.append("房屋租赁合同\n\n"); // 执行对应服务/DAO方法推进流程
        sb.append("合同编号：HLS-").append(order.getId()).append("-").append(System.currentTimeMillis()).append("\n"); // 借助已有方法完成该业务动作
        sb.append("签署日期：").append(now).append("\n\n"); // 借助已有方法完成该业务动作

        sb.append("甲方（出租方/房东）：").append(landlordName).append("\n"); // 借助已有方法完成该业务动作
        if (StringUtils.hasText(landlordIdCard)) { // 按该条件分支处理不同业务场景
            sb.append("身份证号：").append(landlordIdCard).append("\n"); // 调用组件能力完成当前步骤
        }
        if (landlord != null && StringUtils.hasText(landlord.getPhone())) { // 依据当前状态决定后续处理路径
            sb.append("联系电话：").append(landlord.getPhone()).append("\n"); // 调用组件能力完成当前步骤
        }
        sb.append("\n"); // 执行对应服务/DAO方法推进流程
        sb.append("乙方（承租方/租客）：").append(tenantName).append("\n"); // 调用组件能力完成当前步骤
        if (StringUtils.hasText(tenantIdCard)) { // 在该判断成立时执行对应逻辑
            sb.append("身份证号：").append(tenantIdCard).append("\n"); // 执行对应服务/DAO方法推进流程
        }
        if (tenant != null && StringUtils.hasText(tenant.getPhone())) { // 按该条件分支处理不同业务场景
            sb.append("联系电话：").append(tenant.getPhone()).append("\n"); // 执行对应服务/DAO方法推进流程
        }
        sb.append("\n"); // 执行对应服务/DAO方法推进流程

        sb.append("第一条 房屋基本信息\n"); // 借助已有方法完成该业务动作
        sb.append("1.1 房屋名称：").append(houseName).append("\n"); // 调用组件能力完成当前步骤
        sb.append("1.2 房屋地址：").append(houseAddress).append("\n"); // 调用组件能力完成当前步骤
        if (house != null) { // 依据当前状态决定后续处理路径
            sb.append("1.3 房屋面积：").append(house.getArea()).append("平方米\n"); // 调用组件能力完成当前步骤
            sb.append("1.4 房屋类型：").append(house.getHouseType()).append("\n"); // 调用组件能力完成当前步骤
        }
        sb.append("\n"); // 执行对应服务/DAO方法推进流程

        sb.append("第二条 租赁期限\n"); // 借助已有方法完成该业务动作
        sb.append("2.1 租赁期限自").append(startDate).append("至").append(endDate).append("。\n"); // 借助已有方法完成该业务动作
        sb.append("2.2 租赁期满，甲方有权收回房屋，乙方应如期交还房屋。\n\n"); // 调用组件能力完成当前步骤

        // 第三条：租金与支付周期（补充“首期支付”“逾期处理”等常见市场条款）
        sb.append("第三条 租金及付款方式\n"); // 借助已有方法完成该业务动作
        sb.append("3.1 月租金：人民币").append(monthlyRent).append("元整。\n"); // 调用组件能力完成当前步骤
        sb.append("3.2 租金按先付后住原则支付，乙方应于每自然月1日前支付当月租金；首次租金应于合同签署当日支付。\n"); // 借助已有方法完成该业务动作
        sb.append("3.3 支付方式：银行转账、线上支付或双方书面确认的其他方式；乙方支付后应保存支付凭证。\n"); // 借助已有方法完成该业务动作
        sb.append("3.4 乙方逾期支付租金的，甲方有权书面催告；催告后仍未支付的，按本合同违约条款处理。\n\n"); // 借助已有方法完成该业务动作

        // 第四条：押金处理（明确“可扣减范围、退还时限、补足机制”）
        sb.append("第四条 押金条款\n"); // 执行对应服务/DAO方法推进流程
        sb.append("4.1 乙方在签订合同时向甲方缴纳押金：人民币").append(deposit).append("元整。\n"); // 执行对应服务/DAO方法推进流程
        sb.append("4.2 押金用于担保乙方履行本合同义务，不得直接抵扣最后一期租金，除非双方另有书面约定。\n"); // 借助已有方法完成该业务动作
        sb.append("4.3 租赁期满或合同依法解除后，乙方按约返还房屋并结清应付费用的，甲方应在7个工作日内无息退还剩余押金。\n"); // 调用组件能力完成当前步骤
        sb.append("4.4 因乙方原因造成房屋或设施损坏、欠费、违约金等，甲方有权从押金中据实扣减，并向乙方提供明细；不足部分乙方应补足。\n\n"); // 调用组件能力完成当前步骤

        // 第五条：维修与保养责任（细化双方责任边界）
        sb.append("第五条 维修责任\n"); // 执行对应服务/DAO方法推进流程
        sb.append("5.1 房屋主体结构、原有水电管线及房东提供的基础设施自然损耗维修由甲方负责。\n"); // 调用组件能力完成当前步骤
        sb.append("5.2 因乙方保管或使用不当导致房屋、家具家电及附属设施损坏的，由乙方负责维修或按市场价赔偿。\n"); // 调用组件能力完成当前步骤
        sb.append("5.3 乙方发现安全隐患或设施故障时，应及时通知甲方；因未及时通知导致损失扩大的，由过错方承担相应责任。\n"); // 执行对应服务/DAO方法推进流程
        sb.append("5.4 甲方进行必要维修时，应提前合理通知乙方并尽量减少对乙方居住影响；紧急情况除外。\n\n"); // 借助已有方法完成该业务动作

        // 第六条：费用结算（保留系统配置并补充账单规则）
        sb.append("第六条 水电费标准\n"); // 借助已有方法完成该业务动作
        sb.append("6.1 水费：").append(waterRule).append("。\n"); // 执行对应服务/DAO方法推进流程
        sb.append("6.2 电费：").append(electricRule).append("。\n"); // 执行对应服务/DAO方法推进流程
        sb.append("6.3 燃气费：").append(gasRule).append("。\n"); // 调用组件能力完成当前步骤
        sb.append("6.4 物业费：").append(propertyRule).append("。\n"); // 调用组件能力完成当前步骤
        sb.append("6.5 网络费：").append(internetRule).append("。\n"); // 执行对应服务/DAO方法推进流程
        sb.append("6.6 除房租已含项目外，其余费用按实际发生额据实结算；双方应按月核对费用清单。\n"); // 借助已有方法完成该业务动作
        sb.append("6.7 乙方应于每个结算周期结束后3个自然日内结清本条约定费用。\n\n"); // 调用组件能力完成当前步骤

        // 第七条：交付与返还（突出交接清单、结算与腾退）
        sb.append("第七条 交付与返还\n"); // 调用组件能力完成当前步骤
        sb.append("7.1 甲方应于租赁起始日前完成房屋及附属设施交付，并与乙方共同确认交接清单。\n"); // 调用组件能力完成当前步骤
        sb.append("7.2 交接清单应载明房屋钥匙数量、家具家电状态、表计读数及其他应交接事项。\n"); // 借助已有方法完成该业务动作
        sb.append("7.3 租赁期满或合同解除时，乙方应保持房屋及设施完好并按约返还，不得无故拖延腾退。\n"); // 调用组件能力完成当前步骤
        sb.append("7.4 房屋返还后，双方应在现场完成水电气及物业费用结算确认。\n\n"); // 调用组件能力完成当前步骤

        // 第八条：居住使用与转租限制（增加常见风险控制条款）
        sb.append("第八条 房屋使用及转租限制\n"); // 借助已有方法完成该业务动作
        sb.append("8.1 乙方应将房屋用于合法居住用途，不得用于违法活动或擅自改变房屋用途。\n"); // 借助已有方法完成该业务动作
        sb.append("8.2 未经甲方书面同意，乙方不得转租、分租、出借或以经营名义对外开放房屋。\n"); // 借助已有方法完成该业务动作
        sb.append("8.3 乙方进行装饰装修应事先征得甲方书面同意，并遵守物业管理规范及施工时间要求。\n\n"); // 调用组件能力完成当前步骤

        // 第九条：提前解约（细化通知期限与费用承担）
        sb.append("第九条 提前解约条款\n"); // 借助已有方法完成该业务动作
        sb.append("9.1 乙方提前解约须提前30日书面通知甲方，并承担因此给甲方造成的合理损失。\n"); // 执行对应服务/DAO方法推进流程
        sb.append("9.2 甲方提前终止合同须提前60日书面通知乙方，并承担因此给乙方造成的合理损失。\n"); // 调用组件能力完成当前步骤
        sb.append("9.3 因不可抗力导致合同无法继续履行的，双方互不承担违约责任，但应及时通知并提供证明材料。\n\n"); // 执行对应服务/DAO方法推进流程

        // 第十条：违约责任（拆分典型违约场景，增强可执行性）
        sb.append("第十条 违约责任\n"); // 执行对应服务/DAO方法推进流程
        sb.append("10.1 乙方未按时支付租金，每逾期一日按应付租金的")
                .append(DAILY_LATE_FEE_RATE_PERCENT)
                .append("%支付违约金；逾期超过")
                .append(RENT_OVERDUE_TERMINATION_DAYS)
                .append("日的，甲方有权解除合同。\n"); // 调用组件能力完成当前步骤
        sb.append("10.2 乙方擅自转租、从事违法活动或严重扰民的，甲方有权解除合同并要求乙方赔偿损失。\n"); // 借助已有方法完成该业务动作
        sb.append("10.3 甲方在租赁期间无正当理由收回房屋或妨碍乙方正常使用房屋的，应承担违约责任并赔偿乙方损失。\n"); // 借助已有方法完成该业务动作
        sb.append("10.4 因一方违约导致诉讼、仲裁、律师费等维权成本的，违约方应承担相应费用。\n\n"); // 调用组件能力完成当前步骤

        // 第十一条：不可抗力与通知送达（市场合同常见通用条款）
        sb.append("第十一条 不可抗力与通知送达\n"); // 借助已有方法完成该业务动作
        sb.append("11.1 因地震、火灾、洪水、政府行为等不可抗力导致合同无法履行的，受影响方应在合理期限内通知对方并提供证明。\n"); // 执行对应服务/DAO方法推进流程
        sb.append("11.2 双方基于本合同的通知可通过平台站内消息、短信、电子邮件或书面方式送达，发送至约定联系方式即视为有效通知。\n\n"); // 借助已有方法完成该业务动作

        // 第十二条：争议解决
        sb.append("第十二条 争议解决\n"); // 调用组件能力完成当前步骤
        sb.append("12.1 因本合同引起的或与本合同有关的争议，双方应先友好协商解决。\n"); // 借助已有方法完成该业务动作
        sb.append("12.2 协商不成的，任一方可向房屋所在地有管辖权的人民法院提起诉讼。\n\n"); // 调用组件能力完成当前步骤

        // 第十三条：其他约定
        sb.append("第十三条 其他约定\n"); // 调用组件能力完成当前步骤
        sb.append("13.1 本合同自双方在平台完成签署之日起生效。\n"); // 借助已有方法完成该业务动作
        sb.append("13.2 本合同一式两份，甲乙双方各执一份，具有同等法律效力。\n"); // 执行对应服务/DAO方法推进流程
        sb.append("13.3 本合同未尽事宜，由双方另行协商并以书面补充协议确定；补充协议与本合同具有同等法律效力。\n"); // 调用组件能力完成当前步骤

        // 如果有补充条款，自动按固定正文条款数 + 1 生成条款编号，降低后续维护成本
        if (StringUtils.hasText(additionalClauses)) { // 在该判断成立时执行对应逻辑
            sb.append("\n第").append(BASE_ARTICLE_COUNT + 1).append("条 补充条款\n"); // 调用组件能力完成当前步骤
            sb.append(additionalClauses).append("\n"); // 借助已有方法完成该业务动作
        }

        sb.append("\n甲方签字：_______________    日期：_______________\n"); // 借助已有方法完成该业务动作
        sb.append("\n乙方签字：_______________    日期：_______________\n"); // 执行对应服务/DAO方法推进流程

        return sb.toString(); // 输出本方法最终结果
    }

    /**
     * 构建费用规则文本，兼容“计量收费/固定收费/房租已含”三种类型。
     */
    private String buildFeeRule(String feeType, BigDecimal amount, String unit) { // 调用组件能力完成当前步骤
        if ("INCLUDED".equalsIgnoreCase(feeType)) { // 按该条件分支处理不同业务场景
            return "房租已包含"; // 输出本方法最终结果
        }
        if ("FIXED".equalsIgnoreCase(feeType)) { // 按该条件分支处理不同业务场景
            if (amount != null) { // 按该条件分支处理不同业务场景
                return "固定费用 " + amount + " " + unit; // 把结果交还给上层调用方
            }
            return "固定费用，具体金额按双方确认清单执行"; // 返回当前阶段的处理结果
        }
        if ("METERED".equalsIgnoreCase(feeType)) { // 在该判断成立时执行对应逻辑
            if (amount != null) { // 按该条件分支处理不同业务场景
                return "按表计量，单价 " + amount + " " + unit; // 输出本方法最终结果
            }
            return "按表计量，据实结算"; // 输出本方法最终结果
        }
        if (amount != null) { // 按该条件分支处理不同业务场景
            return "按双方约定执行（参考金额 " + amount + " " + unit + "）"; // 返回当前阶段的处理结果
        }
        return "按双方约定执行"; // 把结果交还给上层调用方
    }

    /**
     * 身份证号脱敏展示：保留前 6 位与后 4 位，降低敏感信息泄露风险。
     */
    private String maskIdCard(String idCard) { // 执行对应服务/DAO方法推进流程
        if (!StringUtils.hasText(idCard)) { // 按该条件分支处理不同业务场景
            return ""; // 返回当前阶段的处理结果
        }
        String trimmed = idCard.trim(); // 执行对应服务/DAO方法推进流程
        if (trimmed.length() <= 8) { // 按该条件分支处理不同业务场景
            return trimmed; // 返回当前阶段的处理结果
        }
        return trimmed.substring(0, 6) + "********" + trimmed.substring(trimmed.length() - 4); // 输出本方法最终结果
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
    public Contract signContract(Long contractId, Long userId, String role) { // 合同签署（租客/房东）
        Contract contract = contractMapper.selectById(contractId); // 读取合同主记录，作为签署动作的操作对象
        if (contract == null) { // 依据当前状态决定后续处理路径
            throw new BusinessException(404, "合同不存在"); // 抛出业务异常并中断当前流程
        }
        if ("CANCELLED".equals(contract.getStatus())) { // 按该条件分支处理不同业务场景
            throw new BusinessException("合同已被取消"); // 立即返回错误避免继续执行
        }

        // 根据角色设置对应的签署状态和签署时间
        if ("TENANT".equals(role)) { // 按该条件分支处理不同业务场景
            if (!contract.getTenantId().equals(userId)) { // 在该判断成立时执行对应逻辑
                throw new BusinessException(403, "无权以租客身份签署合同"); // 抛出业务异常并中断当前流程
            }
            contract.setTenantSigned(true); // 记录租客已签署，供后续判断是否达到“双方签完”
            // 记录租客的实际签署时间，该时间将持久化到 contracts.tenant_sign_time
            contract.setTenantSignTime(LocalDateTime.now()); // 写入租客签署时间用于审计与合同展示
        } else if ("LANDLORD".equals(role)) { // 借助已有方法完成该业务动作
            if (!contract.getLandlordId().equals(userId)) { // 按该条件分支处理不同业务场景
                throw new BusinessException(403, "无权以房东身份签署合同"); // 抛出业务异常并中断当前流程
            }
            contract.setLandlordSigned(true); // 记录房东已签署，推动合同状态向“双方已签”演进
            // 记录房东的实际签署时间，该时间将持久化到 contracts.landlord_sign_time
            contract.setLandlordSignTime(LocalDateTime.now()); // 写入房东签署时间，保证签约时间轴完整
        } else { // 这里执行当前语句的核心处理
            throw new BusinessException("角色无效: " + role); // 以异常形式提示调用方当前问题
        }

        // 检查双方是否均已签署，若是则更新状态为双方已签并发送通知
        if (Boolean.TRUE.equals(contract.getTenantSigned()) && Boolean.TRUE.equals(contract.getLandlordSigned())) { // 按该条件分支处理不同业务场景
            contract.setStatus("FULLY_SIGNED"); // 双方都签完后，合同进入“已生效可履约”状态
            contract.setSignTime(LocalDateTime.now()); // 记录双方签署完成时间，作为合同正式生效时间
            // 关键业务口径：
            // 当租客与房东都完成合同签署后，预约订单应从“房东已确认(APPROVED)”进入“已签约(SIGNED)”。
            // 这样前端列表与详情能准确区分“仅确认预约”与“双方已签合同”两个阶段，
            // 同时为后续支付、履约等流程提供明确的状态边界。
            if (contract.getOrderId() != null) { // 依据当前状态决定后续处理路径
                // 使用“带状态条件”的原子更新代替“先查后改”，避免并发签署时出现竞态覆盖。
                // 返回值为 0 表示订单不存在或状态已不是 APPROVED（例如已被其他事务更新），
                // 该场景属于幂等可接受结果，无需抛错中断合同签署流程。
                orderMapper.markOrderSignedIfApproved(contract.getOrderId()); // 将关联订单从 APPROVED 原子推进到 SIGNED
            }
            messageProducer.sendContractStatusChange(contract.getTenantId(), "合同已签署完成", contract.getId()); // 通知租客合同完成签约
            messageProducer.sendContractStatusChange(contract.getLandlordId(), "合同已签署完成", contract.getId()); // 通知房东合同完成签约
        } else { // 这里执行当前语句的核心处理
            // 只有一方签署时，根据签署方设置明确状态，便于前端清晰展示流程
            if ("TENANT".equals(role)) { // 按该条件分支处理不同业务场景
                contract.setStatus("TENANT_SIGNED"); // 仅租客签完，等待房东签署
            } else { // 这里执行当前语句的核心处理
                contract.setStatus("LANDLORD_SIGNED"); // 仅房东签完，等待租客签署
            }
        }
        contract.setUpdateTime(LocalDateTime.now()); // 刷新更新时间，准确记录本次签署动作
        contractMapper.updateById(contract); // 持久化签署结果到合同表
        if (contract.getWorkflowInstanceId() != null) { // 依据当前状态决定后续处理路径
            workflowService.completeContractTask(contract.getWorkflowInstanceId(), userId, role, true); // 同步推进工作流节点，保持流程引擎状态一致
        }
        return contract; // 返回最新合同状态，前端可立即刷新签约进度
    }

    /**
     * 根据 ID 查询合同详情
     *
     * @param id 合同 ID
     * @return 合同详情对象
     */
    @Override
    public Contract getContractById(Long id) { // 查询合同详情
        Contract contract = contractMapper.selectById(id); // 先查出目标记录再做业务判断
        if (contract == null) { // 依据当前状态决定后续处理路径
            throw new BusinessException(404, "合同不存在"); // 抛出业务异常并中断当前流程
        }
        if (contract.getTenantId() != null) { // 按该条件分支处理不同业务场景
            User tenant = userMapper.selectById(contract.getTenantId()); // 读取当前业务所需数据
            if (tenant != null) { // 按该条件分支处理不同业务场景
                tenant.setPassword(null); // 补齐对象属性供后续流程使用
                // 合同详情仅返回脱敏身份证号，既满足合同展示诉求，也避免泄露敏感信息。
                tenant.setIdCard(maskIdCard(idCardCryptoService.decryptFromStorage(tenant.getIdCard()))); // 给对象写入当前步骤需要的字段值
                contract.setTenant(tenant); // 补齐对象属性供后续流程使用
            }
        }
        if (contract.getLandlordId() != null) { // 依据当前状态决定后续处理路径
            User landlord = userMapper.selectById(contract.getLandlordId()); // 先查出目标记录再做业务判断
            if (landlord != null) { // 依据当前状态决定后续处理路径
                landlord.setPassword(null); // 设置业务字段以形成完整数据
                landlord.setIdCard(maskIdCard(idCardCryptoService.decryptFromStorage(landlord.getIdCard()))); // 设置业务字段以形成完整数据
                contract.setLandlord(landlord); // 设置业务字段以形成完整数据
            }
        }
        if (contract.getHouseId() != null) { // 按该条件分支处理不同业务场景
            contract.setHouse(houseMapper.selectById(contract.getHouseId())); // 读取当前业务所需数据
        }
        if (contract.getOrderId() != null) { // 依据当前状态决定后续处理路径
            Order order = orderMapper.selectById(contract.getOrderId()); // 先查出目标记录再做业务判断
            if (order != null) { // 依据当前状态决定后续处理路径
                contract.setOrderNo(order.getOrderNo()); // 设置业务字段以形成完整数据
            }
        }
        return contract; // 返回当前阶段的处理结果
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
    public PageResult<Contract> listContracts(Long userId, String role, int page, int size) { // 分页查询用户合同列表
        Page<Contract> pageObj = new Page<>(page, size); // 初始化对象以便填充业务字段
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>(); // 先查出目标记录再做业务判断
        // 根据角色过滤合同
        if ("TENANT".equals(role)) { // 按该条件分支处理不同业务场景
            wrapper.eq(Contract::getTenantId, userId); // 借助已有方法完成该业务动作
        } else if ("LANDLORD".equals(role)) { // 借助已有方法完成该业务动作
            wrapper.eq(Contract::getLandlordId, userId); // 调用组件能力完成当前步骤
        } else { // 这里执行当前语句的核心处理
            // 未指定角色则查询用户作为租客或房东的所有合同
            wrapper.eq(Contract::getTenantId, userId).or().eq(Contract::getLandlordId, userId); // 调用组件能力完成当前步骤
        }
        wrapper.orderByDesc(Contract::getCreateTime); // 调用组件能力完成当前步骤
        Page<Contract> result = contractMapper.selectPage(pageObj, wrapper); // 从数据库加载后续处理对象
        return PageResult.of(result.getTotal(), result.getRecords(), page, size); // 输出本方法最终结果
    }

    /**
     * 将合同导出为 PDF 文件，优先使用中文字体。
     * 生成后将 PDF 落盘保存至 {uploadDir}/contracts/ 目录，
     * 并将相对路径写入数据库 contracts.pdf_path，供前端通过 /api/uploads/ 直接下载。
     * 若相同合同已有 PDF 文件，则重新生成覆盖（保证内容为最新签署状态）。
     *
     * @param contractId 合同 ID
     * @return PDF 文件的字节数组
     */
    @Override
    public byte[] exportPdf(Long contractId) { // 导出合同 PDF
        Contract contract = contractMapper.selectById(contractId); // 先查出目标记录再做业务判断
        if (contract == null) { // 依据当前状态决定后续处理路径
            throw new BusinessException(404, "合同不存在"); // 抛出业务异常并中断当前流程
        }
        try { // 在当前步骤完成必要业务动作
            ByteArrayOutputStream baos = new ByteArrayOutputStream(); // 创建对象承载本步骤数据
            Document document = new Document(PageSize.A4, 50, 50, 50, 50); // 创建对象承载本步骤数据
            PdfWriter.getInstance(document, baos); // 借助已有方法完成该业务动作
            document.open(); // 调用组件能力完成当前步骤

            // PDF 导出优先使用可嵌入中文字体，避免下载后中文显示方块或乱码。
            // 若系统未安装中文字体，则降级到 iText 内置 CJK 字体；最终兜底为 Helvetica。
            BaseFont baseFont = resolvePdfBaseFont(); // 调用组件能力完成当前步骤
            Font titleFont = new Font(baseFont, 18, Font.BOLD); // 初始化对象以便填充业务字段
            Font bodyFont = new Font(baseFont, 12); // 初始化对象以便填充业务字段

            // 添加合同标题
            Paragraph title = new Paragraph("House Lease Contract / 房屋租赁合同", titleFont); // 实例化新对象用于后续操作
            title.setAlignment(Element.ALIGN_CENTER); // 给对象写入当前步骤需要的字段值
            title.setSpacingAfter(20); // 设置业务字段以形成完整数据
            document.add(title); // 执行对应服务/DAO方法推进流程

            // 逐行添加合同正文
            String content = contract.getContent() != null ? contract.getContent() : ""; // 借助已有方法完成该业务动作
            String[] lines = content.split("\n"); // 借助已有方法完成该业务动作
            for (String line : lines) { // 循环处理当前批次元素
                Paragraph para = new Paragraph(line, bodyFont); // 实例化新对象用于后续操作
                para.setSpacingAfter(4); // 补齐对象属性供后续流程使用
                document.add(para); // 调用组件能力完成当前步骤
            }

            document.close(); // 借助已有方法完成该业务动作
            byte[] pdfBytes = baos.toByteArray(); // 借助已有方法完成该业务动作

            // 将 PDF 保存到磁盘，并把相对路径写入数据库的 pdf_path 字段，
            // 以便后续可通过 /api/uploads/contracts/{filename} 直接下载，无需重新生成
            savePdfAndUpdatePath(contract, pdfBytes); // 调用组件能力完成当前步骤

            return pdfBytes; // 输出本方法最终结果
        } catch (Exception e) { // 在当前步骤完成必要业务动作
            log.error("PDF 生成失败：{}", e.getMessage(), e); // 调用组件能力完成当前步骤
            throw new BusinessException("PDF 生成失败：" + e.getMessage()); // 以异常形式提示调用方当前问题
        }
    }

    /**
     * 将 PDF 字节数组落盘保存，并回写 contracts.pdf_path 字段。
     * 文件保存路径：{uploadDir}/contracts/{contractNo}.pdf
     * 数据库存储的相对路径：contracts/{contractNo}.pdf
     * 前端访问 URL（经 WebMvcConfig 映射）：/api/uploads/contracts/{contractNo}.pdf
     *
     * @param contract 合同对象（需含 contractNo 和 id）
     * @param pdfBytes PDF 文件内容
     */
    private void savePdfAndUpdatePath(Contract contract, byte[] pdfBytes) { // 调用组件能力完成当前步骤
        try { // 在当前步骤完成必要业务动作
            // 确保 contracts 子目录存在。
            // 不依赖 exists() 预检查（避免 TOCTOU 竞态），直接调用 mkdirs()；
            // mkdirs() 在目录已存在时返回 false，需再判断是否为合法目录。
            File contractsDir = new File(uploadDir, CONTRACT_PDF_SUBDIR).getAbsoluteFile(); // 实例化新对象用于后续操作
            contractsDir.mkdirs(); // 借助已有方法完成该业务动作
            if (!contractsDir.isDirectory()) { // 按该条件分支处理不同业务场景
                log.warn("PDF 目录不可用，跳过落盘：{}", contractsDir.getAbsolutePath()); // 执行对应服务/DAO方法推进流程
                return; // 按既定流程继续处理后续逻辑
            }
            // 对合同编号做路径安全过滤：仅保留字母、数字、连字符和下划线，
            // 防止 contractNo 中含有 '/'、'\' 或 '..' 等路径穿越字符写到目标目录之外
            String safeContractNo = contract.getContractNo().replaceAll("[^A-Za-z0-9_\\-]", "_"); // 调用组件能力完成当前步骤
            String filename = safeContractNo + ".pdf"; // 这里执行当前语句的核心处理
            File pdfFile = new File(contractsDir, filename); // 初始化对象以便填充业务字段
            // 校验最终路径仍在 contractsDir 内部，防御符号链接等绕过手段
            if (!pdfFile.getCanonicalPath().startsWith(contractsDir.getCanonicalPath())) { // 在该判断成立时执行对应逻辑
                log.warn("合同 {} 路径异常，跳过落盘：{}", contract.getContractNo(), pdfFile.getAbsolutePath()); // 执行对应服务/DAO方法推进流程
                return; // 按既定流程继续处理后续逻辑
            }
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) { // 创建对象承载本步骤数据
                fos.write(pdfBytes); // 执行对应服务/DAO方法推进流程
            }
            // 相对路径格式为 "contracts/{safeContractNo}.pdf"，与上传图片的存储约定保持一致
            String relativePath = CONTRACT_PDF_SUBDIR + "/" + filename; // 按既定流程继续处理后续逻辑
            contract.setPdfPath(relativePath); // 设置业务字段以形成完整数据
            contract.setUpdateTime(LocalDateTime.now()); // 把变更结果同步到数据库
            contractMapper.updateById(contract); // 调用组件能力完成当前步骤
            log.info("合同 {} PDF 已保存至：{}，文件路径已更新至数据库", contract.getContractNo(), pdfFile.getAbsolutePath()); // 调用组件能力完成当前步骤
        } catch (Exception e) { // 在当前步骤完成必要业务动作
            // PDF 落盘失败不应阻断下载流程，仅记录警告日志
            log.warn("合同 {} PDF 落盘失败，数据库文件路径字段未更新：{}", contract.getContractNo(), e.getMessage()); // 执行对应服务/DAO方法推进流程
        }
    }

    /**
     * 解析 PDF 导出使用的基础字体：
     * 1) 优先加载系统中文字体并嵌入（IDENTITY_H + EMBEDDED）；
     * 2) 系统字体不可用时退回 iText 内置 CJK 字体；
     * 3) 最终再退回 Helvetica，保证文件可下载。
     */
    private BaseFont resolvePdfBaseFont() { // 执行对应服务/DAO方法推进流程
        for (String candidate : CJK_FONT_CANDIDATES) { // 循环处理当前批次元素
            String fontPath = candidate; // 按既定流程继续处理后续逻辑
            int ttcIndex; // 按既定流程继续处理后续逻辑
            int suffixCommaIndex = candidate.length() - 2; // 调用组件能力完成当前步骤
            if (suffixCommaIndex >= 0 && candidate.charAt(suffixCommaIndex) == ',') { // 按该条件分支处理不同业务场景
                fontPath = candidate.substring(0, suffixCommaIndex); // 执行对应服务/DAO方法推进流程
                char indexChar = candidate.charAt(candidate.length() - 1); // 执行对应服务/DAO方法推进流程
                if (!Character.isDigit(indexChar)) { // 按该条件分支处理不同业务场景
                    log.warn("字体索引解析失败，跳过该候选字体：{}", candidate); // 调用组件能力完成当前步骤
                    continue; // 按既定流程继续处理后续逻辑
                }
                ttcIndex = indexChar - '0'; // 在当前步骤完成必要业务动作
            } else { // 这里执行当前语句的核心处理
                ttcIndex = -1; // 按既定流程继续处理后续逻辑
            }
            if (!new File(fontPath).exists()) { // 按该条件分支处理不同业务场景
                continue; // 按既定流程继续处理后续逻辑
            }
            try { // 在当前步骤完成必要业务动作
                // TTC 字体需要“文件路径,索引”格式，TTF/OTF 直接使用文件路径即可。
                String effectiveFontPath = ttcIndex >= 0 ? candidate : fontPath; // 这里执行当前语句的核心处理
                BaseFont baseFont = BaseFont.createFont(effectiveFontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED); // 调用组件能力完成当前步骤
                log.info("PDF 导出使用系统中文字体：{}", effectiveFontPath); // 调用组件能力完成当前步骤
                return baseFont; // 输出本方法最终结果
            } catch (Exception ex) { // 这里执行当前语句的核心处理
                log.warn("系统字体加载失败，尝试下一个：{}，原因：{}", candidate, ex.getMessage()); // 调用组件能力完成当前步骤
            }
        }
        try { // 在当前步骤完成必要业务动作
            BaseFont fallback = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED); // 调用组件能力完成当前步骤
            log.warn("未找到系统中文字体，PDF 导出降级为 STSong-Light"); // 执行对应服务/DAO方法推进流程
            return fallback; // 返回当前阶段的处理结果
        } catch (Exception ex) { // 这里执行当前语句的核心处理
            log.error("CJK 字体加载失败，PDF 导出最终降级 Helvetica：{}", ex.getMessage()); // 借助已有方法完成该业务动作
            try { // 在当前步骤完成必要业务动作
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED); // 输出本方法最终结果
            } catch (Exception helveticaEx) { // 这里执行当前语句的核心处理
                throw new IllegalStateException("PDF 字体初始化失败", helveticaEx); // 抛出业务异常并中断当前流程
            }
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
    public void cancelContract(Long contractId, Long userId) { // 取消合同并联动业务状态
        Contract contract = contractMapper.selectById(contractId); // 先查出目标记录再做业务判断
        if (contract == null) { // 依据当前状态决定后续处理路径
            throw new BusinessException(404, "合同不存在"); // 抛出业务异常并中断当前流程
        }
        // 验证操作人是否为合同当事方
        if (!contract.getTenantId().equals(userId) && !contract.getLandlordId().equals(userId)) { // 依据当前状态决定后续处理路径
            throw new BusinessException(403, "没有操作权限"); // 抛出业务异常并中断当前流程
        }
        // 双方已签的合同不可取消
        if ("FULLY_SIGNED".equals(contract.getStatus())) { // 按该条件分支处理不同业务场景
            throw new BusinessException("已签署的合同不可取消"); // 抛出业务异常并中断当前流程
        }
        contract.setStatus("CANCELLED"); // 把变更结果同步到数据库
        contract.setUpdateTime(LocalDateTime.now()); // 把变更结果同步到数据库
        contractMapper.updateById(contract); // 调用组件能力完成当前步骤
        // 关键一致性规则（本次需求）：
        // 当租客或房东取消合同时，关联订单也必须同步置为 CANCELLED，
        // 防止出现“合同已取消但订单仍可流转”的不一致状态。
        // 该更新与合同取消处于同一事务中，保证跨表状态原子提交/回滚。
        cancelRelatedOrderForContract(contract); // 执行对应服务/DAO方法推进流程

        // 合同取消后将对应房源恢复为上架状态（ONLINE），使其可以被重新预订
        House house = houseMapper.selectById(contract.getHouseId()); // 读取当前业务所需数据
        if (house != null) { // 依据当前状态决定后续处理路径
            house.setStatus("ONLINE"); // 持久化本次状态更新
            houseMapper.updateById(house); // 调用组件能力完成当前步骤
            log.info("合同 {} 已取消，房源 {} 已自动恢复上架", contractId, house.getId()); // 执行对应服务/DAO方法推进流程
        }
    }

    /**
     * 合同取消时联动取消对应订单（若存在且尚未取消）。
     *
     * 设计说明：
     * 1) orderId 为空时直接忽略，兼容历史脏数据；
     * 2) 订单不存在或已取消时直接返回，保证幂等；
     * 3) 不额外限制订单当前业务阶段，统一落库为 CANCELLED，满足“取消动作双向同步”的业务口径。
     */
    private void cancelRelatedOrderForContract(Contract contract) { // 执行对应服务/DAO方法推进流程
        if (contract == null || contract.getOrderId() == null) { // 依据当前状态决定后续处理路径
            return; // 按既定流程继续处理后续逻辑
        }
        Order relatedOrder = orderMapper.selectById(contract.getOrderId()); // 读取当前业务所需数据
        if (relatedOrder == null || "CANCELLED".equals(relatedOrder.getStatus())) { // 依据当前状态决定后续处理路径
            return; // 按既定流程继续处理后续逻辑
        }
        relatedOrder.setStatus("CANCELLED"); // 把变更结果同步到数据库
        relatedOrder.setUpdateTime(LocalDateTime.now()); // 回写最新字段值保持数据一致
        orderMapper.updateById(relatedOrder); // 调用组件能力完成当前步骤
    }

    /**
     * 生成唯一合同编号
     * 格式：HT{yyyyMMddHHmmss}{4位随机数}
     *
     * @return 合同编号字符串
     */
    private String generateContractNo() { // 调用组件能力完成当前步骤
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")); // 借助已有方法完成该业务动作
        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 9999); // 借助已有方法完成该业务动作
        return "HT" + timestamp + random; // 返回当前阶段的处理结果
    }
}
