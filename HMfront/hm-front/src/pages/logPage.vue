<script setup>
import {ref, watch} from 'vue';
import {useWebSocket} from '@vueuse/core';

const wsUrl = import.meta.env.VITE_WS_BASE_URL;
const logs = ref([]);
const virtualScrollerRef = ref(null);
const autoScroll = ref(true);

const {status, open, close, send, data} = useWebSocket(wsUrl, {
  autoReconnect: {
    retries: 3,
    delay: 1000
  },
  heartbeat: {
    message: JSON.stringify({action: 'ping'}),
    interval: 30000
  },
  immediate: false, // 不立即连接，等待手动调用 open
});

const scrollToBottom = async () => {
  if (autoScroll.value && logs.value.length > 0) {
    await nextTick();
    if (virtualScrollerRef.value) {
      // 滚动到最后一条
      virtualScrollerRef.value.scrollToIndex(logs.value.length - 1);
    }
  }
};

// 连接成功后发送认证信息
watch(status, (newStatus) => {
  if (newStatus === 'OPEN') {
    send(JSON.stringify({
      action: 'subscribeAll'
    }));
  }
});

// 监听接收到的数据
watch(data, (newData) => {
  if (newData) {
    try {
      const logMessage = JSON.parse(newData);
      if (logMessage.type !== 'pong') {
        logs.value.push(JSON.stringify(logMessage));
      }
      if (logs.value.length > 100) {
        logs.value.pop();
      }
      scrollToBottom();
    } catch (error) {
      console.error('解析日志数据失败:', error);
    }
  }
});
onMounted(() => {
  open()
})
</script>

<template>
  <v-card>
    <v-card-title>实时日志查看 (VueUse)</v-card-title>
    <v-card-text>
      <v-virtual-scroll
        :height="300"
        :items="logs"
      >
        <template v-slot:default="{ item }">
          {{ item }}
        </template>
      </v-virtual-scroll>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="sass">

</style>
