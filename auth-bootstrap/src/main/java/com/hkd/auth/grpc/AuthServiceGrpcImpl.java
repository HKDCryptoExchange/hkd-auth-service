package com.hkd.auth.grpc;

import com.hkd.auth.application.service.JwtTokenProvider;
import com.hkd.auth.application.service.TotpService;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Date;
import java.util.List;

/**
 * gRPC Auth Service Implementation
 * 实现认证服务的gRPC接口
 *
 * @author HKD Team
 * @since 1.0.0
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthServiceGrpcImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final JwtTokenProvider jwtTokenProvider;
    private final TotpService totpService;

    /**
     * 验证JWT Token
     * 这是最关键的服务，被所有微服务调用以验证用户身份
     */
    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        log.debug("收到ValidateToken请求");

        ValidateTokenResponse.Builder responseBuilder = ValidateTokenResponse.newBuilder();

        try {
            String token = request.getAccessToken();

            if (token == null || token.isEmpty()) {
                responseBuilder
                        .setValid(false)
                        .setErrorMessage("Token不能为空");
            } else {
                // 验证并解析Token
                Claims claims = jwtTokenProvider.validateAndParseToken(token);

                // 检查是否为Access Token
                String tokenType = claims.get("type", String.class);
                if (!"access".equals(tokenType)) {
                    responseBuilder
                            .setValid(false)
                            .setErrorMessage("Token类型错误，需要Access Token");
                } else {
                    // Token有效，提取用户信息
                    String userId = claims.getSubject();
                    String username = claims.get("username", String.class);
                    String email = claims.get("email", String.class);
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);
                    Date expiration = claims.getExpiration();

                    responseBuilder
                            .setValid(true)
                            .setUserId(userId)
                            .setUsername(username != null ? username : "")
                            .setEmail(email != null ? email : "")
                            .addAllRoles(roles != null ? roles : List.of())
                            .setExpiresAt(expiration.getTime() / 1000); // 转换为Unix时间戳（秒）

                    log.debug("Token验证成功: userId={}, username={}", userId, username);
                }
            }
        } catch (JwtException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            responseBuilder
                    .setValid(false)
                    .setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Token验证异常: {}", e.getMessage(), e);
            responseBuilder
                    .setValid(false)
                    .setErrorMessage("Token验证失败");
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * 验证TOTP双因素认证码
     * 用于高风险操作（如提现、修改安全设置）
     *
     * TODO: 需要实现数据库访问以获取用户的TOTP密钥
     */
    @Override
    public void validateTOTP(ValidateTOTPRequest request, StreamObserver<ValidateTOTPResponse> responseObserver) {
        log.debug("收到ValidateTOTP请求: userId={}", request.getUserId());

        ValidateTOTPResponse.Builder responseBuilder = ValidateTOTPResponse.newBuilder();

        try {
            String userId = request.getUserId();
            String totpCode = request.getTotpCode();

            if (userId == null || userId.isEmpty()) {
                responseBuilder
                        .setValid(false)
                        .setErrorMessage("用户ID不能为空");
            } else if (totpCode == null || totpCode.isEmpty()) {
                responseBuilder
                        .setValid(false)
                        .setErrorMessage("TOTP验证码不能为空");
            } else {
                // TODO: 从数据库获取用户的TOTP密钥
                // String totpSecret = userTotpRepository.findSecretByUserId(userId);
                //
                // if (totpSecret == null) {
                //     responseBuilder
                //             .setValid(false)
                //             .setErrorMessage("用户未启用双因素认证");
                // } else {
                //     boolean isValid = totpService.validateCode(totpSecret, totpCode);
                //     if (isValid) {
                //         responseBuilder.setValid(true);
                //         log.debug("TOTP验证成功: userId={}", userId);
                //     } else {
                //         responseBuilder
                //                 .setValid(false)
                //                 .setErrorMessage("验证码错误");
                //     }
                // }

                // 临时实现：返回未实现错误
                log.warn("ValidateTOTP尚未完全实现，需要数据库支持");
                responseBuilder
                        .setValid(false)
                        .setErrorMessage("TOTP验证服务尚未完全实现");
            }
        } catch (Exception e) {
            log.error("TOTP验证异常: {}", e.getMessage(), e);
            responseBuilder
                    .setValid(false)
                    .setErrorMessage("TOTP验证失败");
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * 检查用户权限
     * 用于细粒度的权限控制
     *
     * TODO: 需要实现权限系统和数据库访问
     */
    @Override
    public void checkPermission(CheckPermissionRequest request, StreamObserver<CheckPermissionResponse> responseObserver) {
        log.debug("收到CheckPermission请求: userId={}, resource={}, action={}",
                request.getUserId(), request.getResource(), request.getAction());

        CheckPermissionResponse.Builder responseBuilder = CheckPermissionResponse.newBuilder();

        try {
            String userId = request.getUserId();
            String resource = request.getResource();
            String action = request.getAction();

            if (userId == null || userId.isEmpty()) {
                responseBuilder
                        .setAllowed(false)
                        .setReason("用户ID不能为空");
            } else if (resource == null || resource.isEmpty()) {
                responseBuilder
                        .setAllowed(false)
                        .setReason("资源标识不能为空");
            } else if (action == null || action.isEmpty()) {
                responseBuilder
                        .setAllowed(false)
                        .setReason("操作类型不能为空");
            } else {
                // TODO: 实现权限检查逻辑
                // 1. 从数据库获取用户角色
                // 2. 根据角色和资源/操作判断是否有权限
                // 3. 支持RBAC（基于角色的访问控制）
                //
                // Example:
                // List<String> userRoles = userRoleRepository.findRolesByUserId(userId);
                // boolean hasPermission = permissionChecker.check(userRoles, resource, action);
                //
                // if (hasPermission) {
                //     responseBuilder.setAllowed(true);
                // } else {
                //     responseBuilder
                //             .setAllowed(false)
                //             .setReason("用户无此操作权限");
                // }

                // 临时实现：返回未实现错误
                log.warn("CheckPermission尚未完全实现，需要权限系统支持");
                responseBuilder
                        .setAllowed(false)
                        .setReason("权限检查服务尚未完全实现");
            }
        } catch (Exception e) {
            log.error("权限检查异常: {}", e.getMessage(), e);
            responseBuilder
                    .setAllowed(false)
                    .setReason("权限检查失败");
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
