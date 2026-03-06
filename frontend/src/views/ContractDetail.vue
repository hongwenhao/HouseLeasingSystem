<template>
  <div class="contract-detail-page">
    <NavBar />
    <div class="page-content" v-if="contract">
      <div class="page-inner">
        <div class="page-header">
          <el-button @click="$router.back()" text>
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <h2 class="page-title">合同详情</h2>
          <el-tag :type="statusType" size="large" effect="dark">{{ statusLabel }}</el-tag>
        </div>

        <!-- Risk Warning -->
        <RiskWarning :risks="risks" />

        <!-- Contract Terms -->
        <el-card class="contract-card">
          <template #header>
            <div class="card-header">
              <span>合同信息</span>
              <span class="contract-no">合同编号：{{ contract.contractNo || contract.id }}</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="签订日期">{{ formatDate(contract.signDate || contract.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="租期开始">{{ formatDate(contract.startDate) }}</el-descriptions-item>
            <el-descriptions-item label="租期结束">{{ formatDate(contract.endDate) }}</el-descriptions-item>
            <el-descriptions-item label="月租金">¥{{ contract.rent }}</el-descriptions-item>
            <el-descriptions-item label="押金">¥{{ contract.deposit }}</el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- Parties Info -->
        <el-card class="contract-card">
          <template #header>双方信息</template>
          <div class="parties-grid">
            <div class="party-card landlord">
              <h4>出租方（房东）</h4>
              <div class="party-info" v-if="contract.landlord">
                <el-avatar :size="48" :icon="UserFilled" :src="contract.landlord.avatarUrl" />
                <div>
                  <p class="party-name">{{ contract.landlord.username }}</p>
                  <p class="party-phone">{{ contract.landlord.phone }}</p>
                </div>
              </div>
              <p v-else class="no-info">房东ID：{{ contract.landlordId }}</p>
            </div>
            <div class="party-divider">VS</div>
            <div class="party-card tenant">
              <h4>承租方（租客）</h4>
              <div class="party-info" v-if="contract.tenant">
                <el-avatar :size="48" :icon="UserFilled" :src="contract.tenant.avatarUrl" />
                <div>
                  <p class="party-name">{{ contract.tenant.username }}</p>
                  <p class="party-phone">{{ contract.tenant.phone }}</p>
                </div>
              </div>
              <p v-else class="no-info">租客ID：{{ contract.tenantId }}</p>
            </div>
          </div>
        </el-card>

        <!-- Contract Clauses -->
        <el-card class="contract-card" v-if="contract.clauses || contract.terms">
          <template #header>合同条款</template>
          <div class="clauses-content">
            <pre class="clauses-text">{{ contract.clauses || contract.terms }}</pre>
          </div>
        </el-card>

        <!-- Sign Dates -->
        <el-card class="contract-card">
          <template #header>签署状态</template>
          <div class="sign-status">
            <div class="sign-item">
              <el-icon :class="contract.landlordSigned ? 'signed' : 'unsigned'">
                <component :is="contract.landlordSigned ? 'CircleCheckFilled' : 'CircleCloseFilled'" />
              </el-icon>
              <span>房东：{{ contract.landlordSigned ? `已于 ${formatDate(contract.landlordSignDate)} 签署` : '待签署' }}</span>
            </div>
            <div class="sign-item">
              <el-icon :class="contract.tenantSigned ? 'signed' : 'unsigned'">
                <component :is="contract.tenantSigned ? 'CircleCheckFilled' : 'CircleCloseFilled'" />
              </el-icon>
              <span>租客：{{ contract.tenantSigned ? `已于 ${formatDate(contract.tenantSignDate)} 签署` : '待签署' }}</span>
            </div>
          </div>
        </el-card>

        <!-- Actions -->
        <div class="action-section" v-if="showSignBtn || showTerminateBtn">
          <el-button
            v-if="showSignBtn"
            type="primary"
            size="large"
            :loading="actioning"
            @click="handleSign"
          >
            <el-icon><Edit /></el-icon> 签署合同
          </el-button>
          <el-button
            v-if="showTerminateBtn"
            type="danger"
            size="large"
            @click="terminateDialogVisible = true"
          >
            终止合同
          </el-button>
        </div>
      </div>
    </div>
    <div v-else-if="loading" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>
    <el-empty v-else description="合同不存在" />

    <!-- Terminate Dialog -->
    <el-dialog v-model="terminateDialogVisible" title="终止合同" width="400px">
      <el-form>
        <el-form-item label="终止原因">
          <el-input
            v-model="terminateReason"
            type="textarea"
            :rows="3"
            placeholder="请输入终止合同的原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="terminateDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="actioning" @click="handleTerminate">确认终止</el-button>
      </template>
    </el-dialog>

    <Footer />
  </div>
</template>

<script setup>
// 说明：合同详情页逻辑，展示合同完整信息、AI 风险检测结果，并支持签署和终止合同操作
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import RiskWarning from '../components/RiskWarning.vue'
import { getContractDetail, signContract, terminateContract, getContractRisks } from '../api/contract.js'

const route = useRoute()
const loading = ref(false)                  // 页面加载状态
const actioning = ref(false)               // 签署/终止按钮 loading 状态
const contract = ref(null)                 // 合同详情数据
const risks = ref([])                      // AI 检测到的合同风险列表
const terminateDialogVisible = ref(false)  // 终止合同对话框显隐
const terminateReason = ref('')            // 终止原因
const role = localStorage.getItem('role') || ''  // 当前用户角色

/** 合同状态对应的中文标签 */
const statusLabel = computed(() => {
  const map = { PENDING: '待签署', ACTIVE: '生效中', TERMINATED: '已终止', EXPIRED: '已到期' }
  return map[contract.value?.status] || contract.value?.status || '-'
})

/** 合同状态对应的 Element Plus Tag 类型 */
const statusType = computed(() => {
  const map = { PENDING: 'warning', ACTIVE: 'success', TERMINATED: 'danger', EXPIRED: 'info' }
  return map[contract.value?.status] || 'info'
})

/**
 * 计算是否显示"签署合同"按钮
 * 条件：合同处于待签署状态，且当前用户尚未签署
 */
const showSignBtn = computed(() => {
  if (!contract.value || contract.value.status !== 'PENDING') return false
  if (role === 'LANDLORD' && !contract.value.landlordSigned) return true
  if (role === 'TENANT' && !contract.value.tenantSigned) return true
  return false
})

/** 计算是否显示"终止合同"按钮（仅生效中的合同可终止） */
const showTerminateBtn = computed(() => {
  return contract.value?.status === 'ACTIVE'
})

onMounted(async () => {
  loading.value = true
  try {
    // 并发加载合同详情和风险检测结果，任一失败不影响另一个
    const [contractRes, risksRes] = await Promise.allSettled([
      getContractDetail(route.params.id),
      getContractRisks(route.params.id)
    ])
    if (contractRes.status === 'fulfilled') contract.value = contractRes.value
    if (risksRes.status === 'fulfilled') {
      risks.value = Array.isArray(risksRes.value) ? risksRes.value : []
    }
  } catch (e) {
    ElMessage.error('加载合同详情失败')
  } finally {
    loading.value = false
  }
})

/**
 * 当前用户签署合同
 * 签署成功后重新加载合同数据（更新签署状态和合同状态）
 */
async function handleSign() {
  actioning.value = true
  try {
    await signContract(route.params.id)
    ElMessage.success('合同签署成功')
    // 重新拉取合同数据以更新双方签署状态
    const res = await getContractDetail(route.params.id)
    contract.value = res
  } catch (e) {
    ElMessage.error(e.message || '签署失败')
  } finally {
    actioning.value = false
  }
}

/**
 * 终止合同（附带终止原因）
 * 终止成功后本地更新合同状态，避免重新请求
 */
async function handleTerminate() {
  actioning.value = true
  try {
    await terminateContract(route.params.id, { reason: terminateReason.value })
    ElMessage.success('合同已终止')
    contract.value.status = 'TERMINATED'  // 本地更新状态
    terminateDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

/** 格式化日期为本地化中文短日期 */
function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.contract-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.page-content {
  flex: 1;
  padding: 24px 20px;
}

.page-inner {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  flex: 1;
}

.contract-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.contract-no {
  font-size: 13px;
  color: #909399;
}

.parties-grid {
  display: flex;
  align-items: center;
  gap: 24px;
}

.party-card {
  flex: 1;
  padding: 16px;
  border-radius: 8px;
  background: #f9fafb;
}

.party-card.landlord {
  border-left: 4px solid #409eff;
}

.party-card.tenant {
  border-left: 4px solid #67c23a;
}

.party-card h4 {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.party-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.party-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.party-phone {
  font-size: 13px;
  color: #909399;
}

.party-divider {
  font-size: 20px;
  font-weight: 700;
  color: #909399;
  flex-shrink: 0;
}

.no-info {
  color: #909399;
  font-size: 14px;
}

.clauses-content {
  padding: 8px 0;
}

.clauses-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  font-family: inherit;
}

.sign-status {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.sign-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.sign-item .el-icon {
  font-size: 20px;
}

.sign-item .signed {
  color: #67c23a;
}

.sign-item .unsigned {
  color: #f56c6c;
}

.action-section {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  padding: 20px 0;
}

.loading-wrap {
  max-width: 900px;
  margin: 40px auto;
  padding: 0 20px;
  width: 100%;
}
</style>
