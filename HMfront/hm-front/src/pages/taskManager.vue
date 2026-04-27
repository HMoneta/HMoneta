<script setup>
import {ref, onMounted} from 'vue'
import {http} from "@/common/request.js";
import {userNotificationStore} from "@/stores/app.js";
import PageHeader from "@/components/PageHeader.vue";
import StatusBadge from "@/components/StatusBadge.vue";

const notificationStore = userNotificationStore();

const loading = ref(false);
const tasks = ref([]);
const cronDialog = ref(false);
const cronForm = ref({
  taskName: '',
  currentCron: '',
  defaultCron: ''
});

const headers = ref([
  {title: '任务名称', key: 'taskName', align: 'center'},
  {title: '描述', key: 'description', align: 'center'},
  {title: '当前频率', key: 'currentCron', align: 'center'},
  {title: '默认频率', key: 'defaultCron', align: 'center'},
  {title: '下次执行', key: 'nextExecutionTime', align: 'center'},
  {title: '状态', key: 'enabled', align: 'center'},
  {title: '操作', key: 'action', align: 'center', sortable: false}
]);

const triggeringTasks = ref(new Set());

const loadTasks = async () => {
  loading.value = true;
  try {
    tasks.value = await http.get('/task/list');
  } catch (err) {
    notificationStore.showError("加载任务列表失败");
  } finally {
    loading.value = false;
  }
};

const triggerTask = async (task) => {
  triggeringTasks.value.add(task.taskName);
  try {
    await http.post(`/task/trigger/${task.taskName}`);
    notificationStore.showSuccess(`任务 ${task.taskName} 已触发`);
    await loadTasks();
  } catch (err) {
    notificationStore.showError(`触发失败`);
  } finally {
    triggeringTasks.value.delete(task.taskName);
  }
};

const openCronDialog = (task) => {
  cronForm.value = {
    taskName: task.taskName,
    currentCron: task.currentCron,
    defaultCron: task.defaultCron
  };
  cronDialog.value = true;
};

const saveCron = async () => {
  try {
    await http.put('/task/cron', {
      taskName: cronForm.value.taskName,
      cronExpression: cronForm.value.currentCron
    });
    notificationStore.showSuccess("Cron 表达式已更新");
    cronDialog.value = false;
    await loadTasks();
  } catch (err) {
    notificationStore.showError("更新失败，请检查 Cron 表达式格式");
  }
};

const toggleEnabled = async (task) => {
  try {
    await http.put('/task/enabled', {
      taskName: task.taskName,
      enabled: task.enabled
    });
    notificationStore.showSuccess(`任务已${task.enabled ? '启用' : '禁用'}`);
    await loadTasks();
  } catch (err) {
    task.enabled = !task.enabled;
    notificationStore.showError("操作失败");
  }
};

const formatDateTime = (dateTime) => {
  if (!dateTime) return '已禁用';
  return new Date(dateTime).toLocaleString('zh-CN');
};

const isTaskTriggering = (taskName) => {
  return triggeringTasks.value.has(taskName);
};

onMounted(() => {
  loadTasks();
});
</script>

<template>
  <PageHeader
    icon="mdi-clock-outline"
    title="定时任务管理"
    subtitle="管理系统定时任务，执行频率和手动触发"
  >
    <template #actions>
      <v-btn icon="mdi-refresh" variant="text" @click="loadTasks"></v-btn>
    </template>
  </PageHeader>

  <v-card class="task-manager-card">
    <v-data-table
      :headers="headers"
      :items="tasks"
      :loading="loading"
      item-value="taskName"
      class="task-table"
    >
      <template v-slot:item.currentCron="{ item }">
        <code class="cron-text">{{ item.currentCron }}</code>
      </template>

      <template v-slot:item.defaultCron="{ item }">
        <code class="cron-text cron-default">{{ item.defaultCron }}</code>
      </template>

      <template v-slot:item.nextExecutionTime="{ item }">
        <span class="next-exec-text">{{ formatDateTime(item.nextExecutionTime) }}</span>
      </template>

      <template v-slot:item.enabled="{ item }">
        <v-switch
          v-model="item.enabled"
          color="success"
          hide-details
          density="compact"
          @update:model-value="toggleEnabled(item)"
        />
      </template>

      <template v-slot:item.action="{ item }">
        <v-tooltip text="修改频率">
          <template v-slot:activator="{ props }">
            <v-btn
              v-bind="props"
              icon="mdi-cog"
              size="small"
              variant="text"
              @click="openCronDialog(item)"
            />
          </template>
        </v-tooltip>

        <v-tooltip text="立即执行">
          <template v-slot:activator="{ props }">
            <v-btn
              v-bind="props"
              icon="mdi-play"
              size="small"
              variant="text"
              color="primary"
              :loading="isTaskTriggering(item.taskName)"
              @click="triggerTask(item)"
            />
          </template>
        </v-tooltip>
      </template>

      <template v-slot:loading>
        <v-skeleton-loader type="table-row@3"></v-skeleton-loader>
      </template>
    </v-data-table>
  </v-card>

  <!-- Cron 编辑对话框 -->
  <v-dialog
    v-model="cronDialog"
    width="auto"
    persistent
  >
    <v-card class="cron-dialog-card">
      <v-toolbar>
        <v-btn
          icon="mdi-close"
          @click="cronDialog = false"
        ></v-btn>
        <v-toolbar-title>编辑 Cron 表达式</v-toolbar-title>
      </v-toolbar>

      <v-card-text class="pt-4">
        <v-text-field
          v-model="cronForm.currentCron"
          label="Cron 表达式"
          variant="outlined"
          class="mb-2"
          hint="格式: 秒 分 时 日 月 周 (例: 0 0/5 * * * ? 表示每5分钟)"
          persistent-hint
        />

        <div class="cron-hints mt-4">
          <div class="text-caption text-medium-emphasis mb-2">常用表达式：</div>
          <v-chip-group>
            <v-chip size="small" @click="cronForm.currentCron = '0 0/5 * * * ?'">每5分钟</v-chip>
            <v-chip size="small" @click="cronForm.currentCron = '0 0/10 * * * ?'">每10分钟</v-chip>
            <v-chip size="small" @click="cronForm.currentCron = '0 0/30 * * * ?'">每30分钟</v-chip>
            <v-chip size="small" @click="cronForm.currentCron = '0 0 0 * * ?'">每天午夜</v-chip>
            <v-chip size="small" @click="cronForm.currentCron = '0 0 */2 * * ?'">每2小时</v-chip>
            <v-chip size="small" @click="cronForm.currentCron = '0 0 */6 * * ?'">每6小时</v-chip>
          </v-chip-group>
        </div>

        <div class="mt-4 text-caption">
          <div>默认表达式: <code>{{ cronForm.defaultCron }}</code></div>
        </div>
      </v-card-text>

      <v-card-actions>
        <v-btn
          variant="outlined"
          @click="cronDialog = false"
        >
          取消
        </v-btn>
        <v-spacer></v-spacer>
        <v-btn
          color="primary"
          variant="elevated"
          @click="saveCron"
        >
          保存
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.task-manager-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.task-table {
  background: transparent !important;
}

.cron-text {
  background: rgba(16, 185, 129, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.85em;
  color: #10b981;
}

.cron-default {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.6);
}

.next-exec-text {
  font-size: 0.9em;
  color: rgb(var(--v-theme-on-surface-variant));
}

.cron-dialog-card {
  border-radius: 12px;
  min-width: 400px;
}

.cron-hints {
  background: rgba(16, 185, 129, 0.05);
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(16, 185, 129, 0.15);
}

.cron-hints code {
  background: rgba(16, 185, 129, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.85em;
  color: #10b981;
}
</style>
