#!/usr/bin/env python3
"""
auth-service 测试脚本
测试 HTTP 端点和 Token 生成
"""
import requests
import json
import sys

def test_health():
    """测试健康检查端点"""
    print("=" * 60)
    print("测试1: 健康检查")
    print("=" * 60)
    try:
        response = requests.get("http://localhost:8013/test/health", timeout=5)
        print(f"状态码: {response.status_code}")
        print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        return True
    except Exception as e:
        print(f"❌ 错误: {e}")
        return False

def generate_test_token(user_id="user123", username="zhangsan", email="zhangsan@example.com"):
    """生成测试 Token"""
    print("\n" + "=" * 60)
    print("测试2: 生成测试 Token")
    print("=" * 60)
    try:
        url = f"http://localhost:8013/test/generate-token?userId={user_id}&username={username}&email={email}"
        print(f"请求 URL: {url}")
        response = requests.get(url, timeout=5)
        print(f"状态码: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"\n✅ Token 生成成功!")
            print(f"\n用户信息:")
            print(f"  - User ID: {data.get('user_id')}")
            print(f"  - Username: {data.get('username')}")
            print(f"  - Email: {data.get('email')}")
            print(f"\nToken 信息:")
            print(f"  - Token Type: {data.get('token_type')}")
            print(f"  - Expires In: {data.get('expires_in')}秒 ({data.get('expires_in')//60}分钟)")
            print(f"\n🔑 Access Token (前100字符):")
            print(f"  {data.get('access_token', '')[:100]}...")
            print(f"\n🔄 Refresh Token (前100字符):")
            print(f"  {data.get('refresh_token', '')[:100]}...")

            # 保存完整Token到文件
            with open('/tmp/auth_test_token.txt', 'w') as f:
                f.write(data.get('access_token', ''))
            print(f"\n💾 完整 Access Token 已保存到: /tmp/auth_test_token.txt")

            return data.get('access_token')
        else:
            print(f"❌ Token 生成失败")
            print(f"响应: {response.text}")
            return None
    except Exception as e:
        print(f"❌ 错误: {e}")
        return None

def main():
    print("\n")
    print("🚀 auth-service 测试开始")
    print("=" * 60)

    # 测试健康检查
    if not test_health():
        print("\n❌ 健康检查失败，请检查服务是否正在运行")
        sys.exit(1)

    # 生成测试 Token
    access_token = generate_test_token()
    if not access_token:
        print("\n❌ Token 生成失败")
        sys.exit(1)

    print("\n" + "=" * 60)
    print("✅ 所有 HTTP 测试通过!")
    print("=" * 60)
    print("\n下一步: 使用 grpcurl 测试 gRPC 服务")
    print(f"\n运行以下命令测试 ValidateToken:")
    print(f"```bash")
    print(f'grpcurl -plaintext -d \'{{"access_token": "$(cat /tmp/auth_test_token.txt)"}}\' \\')
    print(f'  localhost:9013 hkd.auth.v1.AuthService/ValidateToken')
    print(f"```\n")

if __name__ == "__main__":
    main()
