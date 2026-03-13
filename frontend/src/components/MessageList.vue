<template>
  <!-- 组件说明：站内消息列表组件，展示消息通知列表。
       未读消息以蓝色背景高亮显示，并在标题前展示蓝色圆点标记。
       点击未读消息触发 read 事件，通知父组件调用已读接口。 -->
  <div class="message-list">
    <!-- 无消息时展示空状态占位 -->
    <div v-if="messages.length === 0" class="empty">
      <el-empty description="暂无消息" />
    </div>
    <!-- 消息列表：遍历渲染每条消息 -->
    <div
      v-for="msg in messages"
      :key="msg.id"
      class="message-item"
      :class="{ unread: !msg.isRead }"
      @click="handleRead(msg)"
    >
      <!-- 消息头部：标题 + 时间 -->
      <div class="msg-header">
        <span class="msg-title">
          <!-- 未读圆点标记 -->
          <span v-if="!msg.isRead" class="unread-dot"></span>
          {{ msg.title }}
        </span>
        <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
      </div>
      <!-- 消息正文（单行省略） -->
      <p class="msg-content">{{ msg.content }}</p>
    </div>
  </div>
</template>

<script setup>
// 说明：消息列表组件，展示通知列表，点击未读消息时向父组件发送 read 事件
defineProps({
  messages: {
    type: Array,
    default: () => []  // 默认空数组，避免渲染报错
  }
})

// 声明 read 事件：携带消息 id，父组件负责调用已读接口
const emit = defineEmits(['read'])

/**
 * 点击消息处理：仅对未读消息触发 read 事件（已读消息无需重复调用接口）
 * @param {Object} msg - 被点击的消息对象
 */
function handleRead(msg) {
  if (!msg.isRead) {
    emit('read', msg.id)
  }
}

/**
 * 格式化消息时间，转换为本地化中文时间字符串
 * @param {string|Date} time - 消息创建时间
 */
function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { hour12: false }) // 使用24小时制
}
</script>

<style scoped>
/* ===== 消息列表容器 ===== */
.message-list {
  width: 100%;
}

/* ===== 单条消息样式 ===== */
.message-item {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}

.message-item:hover {
  background: #f5f7fa;
}

/* 未读消息：蓝色背景高亮 */
.message-item.unread {
  background: #ecf5ff;
}

/* ===== 消息头部 ===== */
.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.msg-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 未读消息圆点标记（蓝色实心圆） */
.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  flex-shrink: 0;
}

.msg-time {
  font-size: 12px;
  color: #909399;
}

/* ===== 消息正文（超出截断） ===== */
.msg-content {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 空状态 */
.empty {
  padding: 40px 0;
}
</style>

