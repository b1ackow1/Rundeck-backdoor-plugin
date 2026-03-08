# Rundeck-backdoor-plugin
A backdoor plugin for rundeck

> 一个用于学习的后门Rundeck 插件
>
> 
[![License](https://img.shields.io/badge/license-Educational-blue.svg)](LICENSE)
[![Rundeck](https://img.shields.io/badge/Rundeck-5.15.0-green.svg)](https://www.rundeck.com/)
[![Java](https://img.shields.io/badge/Java-11-orange.svg)](https://openjdk.java.net/)


## 多功能注入
本插件集成了三种经典的 Web 后门实现：

| 版本 | 名称 | 路径 | 技术特点 |
|------|------|------|----------|
| **V4** | RCE Shell | `/static/exec` | 简单命令执行 + Web UI |
| **V5** | Suo5 Tunnel | `/static/suo5` | defineClass 注入 + HTTP 隧道 |
| **V6** | Godzilla | `/static/godzilla` | AES 加密 + 动态 payload |

![](https://b1ackow1.github.io/2026/03/08/rundeck-backdoor-for-java/2026-03-08-16-38-18.png)

## 编译

```bash
# 编译插件
mvn clean package

```

## 使用方法

[Rundeck 实战：Java Agent 插件后门植入全流程](https://b1ackow1.github.io/2026/03/08/rundeck-backdoor-for-java/)


#### 步骤 1: 登录 Rundeck

访问 `http://localhost:4440`，使用默认账号登录：
- 用户名: `admin`
- 密码: `admin`

#### 步骤 2: 创建新作业

1. 点击 **"Jobs"** → **"Create a New Job"**
2. 填写作业信息：
   ```
   Job Name: Security Audit
   Group: /
   Description: Multi-route backdoor injection
   ```

#### 步骤 3: 添加插件步骤

1. 在 **"Workflow"** 部分点击 **"Add a step"**
2. 搜索并选择 **"Security Audit Step"**
3. 配置路由选项：
   ```
   Route Selection: v4,v6
   ```

#### 步骤 4: 保存并执行

1. 点击 **"Create"** 保存作业
2. 点击 **"Run Job Now"** 执行
3. 查看执行日志

#### 预期输出

```
========================================
  Multi-Route Injection Tool
========================================
Route Configuration: v4,v5,v6

[Step 1] Finding ClassLoader...
✓ ClassLoader: WebappClassLoader

[Step 2] Getting ServletHandler...
✓ ServletHandler: ServletHandler

[Step 3] Registering routes...
  V4 (RCE Shell): ENABLED
  V5 (Suo5 Tunnel): ENABLED
  V6 (Godzilla): ENABLED

  ✓ RCEServlet registered at: /static/exec
  ✓ Suo5 Tunnel registered at: /static/suo5
  ✓ GodzillaServlet registered at: /static/godzilla
  
[Step 4] Summary
  Total registered: 2 route(s)

========================================
✓ Injection completed successfully!
========================================
```


本项目采用 [MIT License](https://opensource.org/licenses/MIT) 开源。

如果这个项目对你有帮助，欢迎给个Star！


