# HMoneta官方文档

# 快速开始

## 从源码构建项目及部署项目

> HMoneta项目为前后端分离项目，因为处于SNAPSHOT版本，暂时仅提供从源码构建及部署

> 目前仅在amd64架构的Ubuntu24.04部署过该套源码

### 1.环境准备

- 前端使用yarn作为包管理器，请确保yarn版本为4.10.3
- 后端使用maven3作为项目管理器，Java JDK 版本为 25

> 使用Java 25 LTS作为开发语言，是为了后期更好的使用Springboot4

### 1.从Github拉取项目源码

```bash
git clone https://github.com/HMoneta/HMoneta.git
```

### 2.前端工程编译

```bash
// 进入前端工程
cd ~/Hmoneta/HMfront/hm-front
// 安装依赖
yarn install
// 创建.env.pronduction文件
vim .env.pronduction
```

请在环境文件中添加如下环境变量

```aiignore
VITE_API_BASE_URL=http://后端服务器地址/hm
VITE_WS_BASE_URL=ws://后端服务器地址/ws/logs
```

```bash
// 编译前端工程
yarn vite build
```

编译产物将生成在dist文件文件夹中，将文件夹中产物部署在Nginx或其他Web服务器上即可

### 3.后端工程编译

文档编辑中

# 插件系统

HMoneta支持自定义插件，插件均需实现*HMoneta-Official-Plugin-Api*
> HMoneta-Official-Plugin-Api [项目地址](https://github.com/HMoneta/HMoneta-Official-Plugin-Api)
