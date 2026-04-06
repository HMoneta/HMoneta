<!--
name: deploy
description: 合并 main 分支后部署前后端到本地服务器
-->

当你需要部署 HMoneta 项目到本地服务器时，执行以下工作流：

## 前置准备

确保本地环境已安装：
- `ssh` - SSH 客户端
- `scp` - 文件传输
- `curl` - API 调用
- `jq` - JSON 解析（可选，用于解析 API 响应）

## 工作流程

### Step 1: 获取服务器信息

执行部署前，需要收集以下信息：

| 服务 | 需要的信息 |
|------|-----------|
| 后端服务器 | IP 地址、SSH 端口、用户名、密码/密钥 |
| 前端服务器 (1Panel) | IP 地址、1Panel API URL、API Key |

### Step 2: 确认代码状态

```bash
# 确认当前分支和状态
git status

# 确认 main 分支已更新
git fetch origin
git log --oneline origin/main -3
```

### Step 3: 合并 main 分支

```bash
# 切换到 main 分支并合并
git checkout main
git pull origin main

# 切换回目标分支（如果是其他分支开发）
git checkout -
git merge main
```

### Step 4: 构建后端

```bash
# 清理并构建
mvn clean package

# 构建产物位置
# target/HMoneta-*.jar
```

### Step 5: 构建前端

```bash
cd HMfront/hm-front

# 安装依赖（如需要）
yarn install

# 生产构建
yarn build

# 构建产物位置
# dist/
```

### Step 6: 部署后端

**方式 A: 通过 SSH 远程执行**

```bash
# 上传 jar 文件
BACKEND_JAR=$(ls target/HMoneta-*.jar | head -1)
BACKEND_IP="<后端服务器IP>"
SSH_PORT="<SSH端口，默认22>"
USER="<用户名>"

scp -P $SSH_PORT $BACKEND_JAR $USER@$BACKEND_IP:/tmp/HMoneta.jar

# 停止旧进程并启动新进程
ssh -p $SSH_PORT $USER@$BACKEND_IP << 'EOF'
# 停止旧进程（如果正在运行）
pkill -f 'HMoneta.jar' || true

# 后台启动新进程
nohup java -jar /tmp/HMoneta.jar --spring.profiles.active=prod > /var/log/hmoneta.log 2>&1 &

# 等待启动并验证
sleep 5
if ps aux | grep -v grep | grep 'HMoneta.jar'; then
    echo "Backend started successfully"
else
    echo "Backend failed to start, check logs at /var/log/hmoneta.log"
fi
EOF
```

**方式 B: 分步执行**

如果需要分开执行，先上传再远程启动：

```bash
# 仅上传文件
scp -P $SSH_PORT target/HMoneta-*.jar $USER@$BACKEND_IP:/tmp/

# 远程启动（单独命令）
ssh -p $SSH_PORT $USER@$BACKEND_IP "pkill -f 'HMoneta.jar' || true; nohup java -jar /tmp/HMoneta.jar --spring.profiles.active=prod > /var/log/hmoneta.log 2>&1 &"
```

### Step 7: 部署前端 (1Panel API)

**1Panel API 基本调用格式：**

```bash
# 1Panel 配置
FRONTEND_IP="<前端服务器IP>"
PANEL_URL="https://$FRONTEND_IP:9999"  # 默认 1Panel 端口
PANEL_API_KEY="<你的API Key>"

# 创建压缩包
cd dist && tar -czvf /tmp/hmoneta-front.tar.gz .

# 获取上传路径（通过 1Panel API）
# 参考 1Panel API 文档获取具体端点
```

**典型 1Panel API 部署流程：**

```bash
# 1. 获取网站列表
curl -k -X GET "$PANEL_URL/api/v1/websites" \
  -H "Authorization: Bearer $PANEL_API_KEY"

# 2. 上传文件到指定目录
# 具体端点取决于 1Panel 配置的运行环境

# 3. 重启网站
curl -k -X POST "$PANEL_URL/api/v1/websites/<site_id>/reload" \
  -H "Authorization: Bearer $PANEL_API_KEY"
```

### Step 8: 验证部署

```bash
# 验证后端
curl -f http://$BACKEND_IP:<后端端口>/hm/health || echo "Backend health check failed"

# 验证前端
curl -f https://$FRONTEND_DOMAIN/ || echo "Frontend check failed"
```

---

## 快速部署脚本模板

```bash
#!/bin/bash
set -e

# ============== 配置区 ==============
BACKEND_IP="<后端IP>"
BACKEND_SSH_PORT="22"
BACKEND_SSH_USER="root"
BACKEND_JAR_PATH="target/HMoneta-*.jar"

FRONTEND_IP="<前端IP>"
FRONTEND_PANEL_URL="https://$FRONTEND_IP:9999"
FRONTEND_API_KEY="<你的API Key>"
# ====================================

echo "=== 1. Building Backend ==="
mvn clean package -DskipTests

echo "=== 2. Building Frontend ==="
cd HMfront/hm-front
yarn build
cd ../..

echo "=== 3. Deploying Backend ==="
JAR_FILE=$(ls $BACKEND_JAR_PATH | head -1)
scp -P $BACKEND_SSH_PORT $JAR_FILE $BACKEND_SSH_USER@$BACKEND_IP:/tmp/HMoneta.jar

ssh -p $BACKEND_SSH_PORT $BACKEND_SSH_USER@$BACKEND_IP << 'EOF'
pkill -f 'HMoneta.jar' || true
sleep 2
nohup java -jar /tmp/HMoneta.jar --spring.profiles.active=prod > /var/log/hmoneta.log 2>&1 &
sleep 5
ps aux | grep -v grep | grep 'HMoneta.jar' && echo "Backend OK" || echo "Backend FAILED"
EOF

echo "=== 4. Deploying Frontend via 1Panel API ==="
# 根据实际情况调用 1Panel API
# curl -k -X POST "$FRONTEND_PANEL_URL/api/..."

echo "=== Deployment Complete ==="
```

---

## 注意事项

1. **API Key 安全**: 不要将 API Key 提交到代码仓库，使用环境变量或单独的配置
2. **版本回滚**: 部署前备份旧版本jar，以便快速回滚
3. **日志位置**: 后端日志在 `/var/log/hmoneta.log`
4. **1Panel API**: 具体端点请参考 1Panel 官方 API 文档

## 验证检查清单

- [ ] 后端进程正在运行
- [ ] 后端健康检查通过
- [ ] 前端可访问
- [ ] 前后端联调正常
