---
name: user-domain
status: backlog
created: 2025-11-17T01:03:31Z
progress: 0%
prd: .claude/prds/user-domain.md
github: https://github.com/HKDCryptoExchange/hkd-project-management/blob/main/.claude/epics/user-domain/epic.md
---

# Epic: 用户域开发 (User Domain)

## Overview

实现HKD交易所的用户管理、身份认证和KYC审核系统,包括3个微服务:user-service(用户管理)、kyc-service(KYC认证)、auth-service(认证授权)。采用Java 21 + Spring Boot 3.2技术栈,支持邮箱/手机/OAuth2多种注册登录方式,提供L0-L3四级KYC认证体系,实现JWT Token + TOTP MFA多重安全保障。

## Architecture Decisions

### 1. 微服务拆分策略
- **user-service(8001)**: 用户CRUD + 密码管理 + 会话管理
- **kyc-service(8003)**: KYC认证 + Activiti工作流 + OCR/人脸识别集成
- **auth-service(8013)**: JWT Token + RBAC权限 + TOTP MFA

**理由**: 按业务领域拆分,KYC与认证逻辑复杂且独立,便于扩展和维护

### 2. 认证方案
- JWT Token (Access Token 1小时 + Refresh Token 7天)
- BCrypt密码加密(强度12)
- TOTP双因素认证(RFC 6238标准,Google Authenticator)
- Redis Session分布式会话

**理由**: 无状态JWT适合微服务,Refresh Token降低安全风险,TOTP提供额外安全层

### 3. KYC认证体系
- L0(未认证): 仅注册,禁止提现
- L1(初级): 身份证OCR,日提现1万USDT
- L2(高级): 人脸识别,日提现10万USDT
- L3(VIP): 视频认证,无限额

**理由**: 分级认证平衡用户体验和合规要求,OCR/人脸识别自动化提升审核效率

### 4. 数据存储策略
- PostgreSQL分库分表(user_id % 4)
- Redis缓存(用户信息热数据,TTL 1小时)
- MongoDB审计日志(敏感操作,保存7年)

**理由**: 分库分表应对大规模用户,Redis提升查询性能,MongoDB审计满足合规

## Technical Approach

### Backend Services

**user-service** (Java 21 + Spring Boot 3.2):
- 注册接口: 邮箱/手机号注册 + 验证码验证(Redis 5分钟TTL)
- 登录接口: 密码登录 + 验证码登录 + OAuth2社交登录
- 密码管理: BCrypt加密 + 密码历史(最近3次不可重复) + 90天过期策略
- 会话管理: Redis Session + 多设备管理 + 异地登录预警
- 用户信息: Profile CRUD + 偏好设置(语言/时区/货币)

**kyc-service** (Java 21 + Spring Boot 3.2 + Activiti):
- L1认证: 身份证上传 → OCR识别(阿里云/AWS Textract) → 自动审核(5分钟)
- L2认证: 人脸识别(活体检测+人证对比>90%) → 人工审核(1-3工作日)
- L3认证: 视频认证 → 资产证明 → 人工审核
- Activiti工作流: 定义审核流程,支持驳回重新提交
- 文件管理: AWS S3/阿里云OSS存储

**auth-service** (Java 21 + Spring Boot 3.2 + Spring Security):
- JWT Token: 生成/验证/刷新
- TOTP MFA: 生成密钥 → 二维码 → 验证6位动态码 → 10个备份码
- RBAC权限: 角色(普通/VIP/管理员) → 权限(资源级控制)
- Token黑名单: Redis存储(支持强制登出)
- 审计日志: 所有敏感操作记录MongoDB

### Infrastructure

**数据库**:
- PostgreSQL 16主从集群(1主2从)
- 分库分表: users表按user_id % 4分4个库
- 索引: email(UNIQUE), phone(UNIQUE), user_id, status

**缓存**:
- Redis 7.2集群模式
- Caffeine本地缓存(5分钟) + Redis二级缓存(1小时)
- 缓存Key: `user:info:{userId}`, `user:tokens:{userId}`

**消息队列**:
- Kafka: user.registered, user.kyc.approved, user.login.suspicious

**监控**:
- Skywalking APM链路追踪
- Prometheus + Grafana监控
- 告警: 登录失败率>10%, KYC审核积压>100

## Implementation Strategy

**Phase 1 (2周)**: user-service核心功能
- 注册/登录API
- 密码管理
- 数据库Schema + Flyway迁移
- 单元测试(覆盖率>80%)

**Phase 2 (2周)**: kyc-service KYC认证
- L1/L2认证API
- OCR/人脸识别集成
- Activiti工作流
- 单元测试

**Phase 3 (1周)**: auth-service认证授权
- JWT Token管理
- TOTP MFA
- RBAC权限
- 单元测试

**Phase 4 (1周)**: 集成测试 + 性能优化
- 注册-登录流程测试
- KYC认证流程测试
- 性能压测(登录5000 TPS,注册1000 TPS)
- Redis缓存优化

**风险缓解**:
- OCR/人脸识别服务不可用 → 降级人工审核
- 第三方邮件/短信服务延迟 → 异步重试(最多3次)
- 数据库分库分表复杂性 → 使用ShardingSphere简化

## Task Breakdown Preview

1. **user-service核心功能** (40h) - 注册/登录/密码管理/会话管理/用户信息
2. **kyc-service KYC认证** (32h) - L1/L2认证/OCR集成/人脸识别/Activiti工作流
3. **auth-service认证授权** (24h) - JWT Token/TOTP MFA/RBAC权限
4. **数据库Schema设计** (8h) - users/user_profiles/kyc_applications/security_settings/login_logs表 + Flyway
5. **集成测试** (16h) - 注册-登录流程/KYC流程/MFA流程/异地登录预警
6. **性能压测与优化** (16h) - JMeter压测/Redis缓存优化/慢查询优化
7. **安全加固** (12h) - SQL注入防护/XSS防护/CSRF防护/敏感数据加密
8. **文档编写** (8h) - API文档(OpenAPI 3.0)/部署文档/运维手册

**总估算**: 156小时 ≈ 6-8周 (按2人并行开发)

## Dependencies

**外部服务依赖**:
- notify-service: 发送邮件/短信验证码(SLA 99.9%)
- risk-service: 登录风控检查(SLA 99.95%)
- account-service: 用户注册后创建账户(异步Kafka)

**第三方服务**:
- 阿里云OCR / AWS Textract: 身份证识别
- 人脸识别SDK: 活体检测+人证对比
- SendGrid / AWS SES: 邮件发送
- Twilio / 阿里云短信: 短信发送

**基础设施**:
- PostgreSQL 16
- Redis 7.2
- Kafka 3.6
- Nacos 2.3

## Success Criteria (Technical)

**功能**:
- ✅ 邮箱/手机号注册成功率 > 99%
- ✅ 登录成功率 > 99.9%
- ✅ KYC L1自动审核准确率 > 95%
- ✅ KYC L2人脸识别准确率 > 98%
- ✅ 异地登录预警触发率 100%

**性能**:
- ✅ 注册API P99 < 500ms, TPS ≥ 1000
- ✅ 登录API P99 < 200ms, TPS ≥ 5000
- ✅ 用户查询API P99 < 50ms

**安全**:
- ✅ 渗透测试0个高危漏洞
- ✅ BCrypt密码加密,无明文密码
- ✅ 敏感数据AES-256加密
- ✅ 100%记录敏感操作审计日志

**可用性**:
- ✅ 服务可用性99.95%
- ✅ 数据零丢失(主从复制+定时备份)
- ✅ 故障恢复< 5分钟

## Estimated Effort

**总工时**: 156小时

**时间线** (按2人并行开发):
- Week 1-2: user-service + 数据库Schema
- Week 3-4: kyc-service + auth-service
- Week 5: 集成测试
- Week 6: 性能优化 + 安全加固 + 文档

**资源需求**:
- Java开发工程师 × 2
- QA测试工程师 × 1 (兼职)
- DevOps工程师 × 1 (兼职)

**关键路径**:
1. 数据库Schema设计 (阻塞所有开发)
2. user-service注册/登录 (阻塞kyc-service/auth-service测试)
3. OCR/人脸识别集成 (外部服务接入耗时)
