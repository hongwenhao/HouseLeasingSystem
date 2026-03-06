<template>
  <div class="message-list">
    <div v-if="messages.length === 0" class="empty">
      <el-empty description="暂无消息" />
    </div>
    <div
      v-for="msg in messages"
      :key="msg.id"
      class="message-item"
      :class="{ unread: !msg.isRead }"
      @click="handleRead(msg)"
    >
      <div class="msg-header">
        <span class="msg-title">
          <span v-if="!msg.isRead" class="unread-dot"></span>
          {{ msg.title }}
        </span>
        <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
      </div>
      <p class="msg-content">{{ msg.content }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  messages: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['read'])

function handleRead(msg) {
  if (!msg.isRead) {
    emit('read', msg.id)
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.message-list {
  width: 100%;
}

.message-item {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}

.message-item:hover {
  background: #f5f7fa;
}

.message-item.unread {
  background: #ecf5ff;
}

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

.msg-content {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty {
  padding: 40px 0;
}
</style>
