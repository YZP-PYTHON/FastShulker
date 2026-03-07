import os
import requests

# 获取环境变量
GITHUB_TOKEN = os.getenv('GITHUB_TOKEN')
MODRINTH_API_KEY = os.getenv('MODRINTH_API_KEY')
REPO_OWNER = "YZP-PYTHON"  # 修改为你的 GitHub 用户名
REPO_NAME = "FastShulker"  # 修改为你的仓库名
RELEASE_TAG = os.getenv('GITHUB_REF').split('/')[-1]  # 获取发布版本标签
VERSION_NUMBER = os.getenv('VERSION_NUMBER', RELEASE_TAG)  # 默认为 RELEASE_TAG，但可覆盖
LOADERS = os.getenv('LOADERS', 'java').split(',')  # 默认加载器为 "java"，但支持多个加载器（逗号分隔）
GAME_VERSIONS = os.getenv('GAME_VERSIONS', '1.21.3').split(',')  # 默认游戏版本为 "1.21.3"，支持多个游戏版本（逗号分隔）

# Modrinth API URL
MODRINTH_API_URL = "https://api.modrinth.com/v2/projects/{project_id}/versions"

def get_release_assets():
    # 获取 GitHub Release 详细信息
    release_url = f'https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases/tags/{RELEASE_TAG}'
    headers = {'Authorization': f'token {GITHUB_TOKEN}'}
    response = requests.get(release_url, headers=headers)
    release_data = response.json()

    # 获取所有资源文件（附加在 Release 上的文件）
    assets = release_data.get('assets', [])
    asset_urls = [asset['browser_download_url'] for asset in assets]
    print("get github asset success",assets)
    return asset_urls

def upload_to_modrinth():
    # 获取 Release 的所有资产文件 URL
    asset_urls = get_release_assets()

    for asset_url in asset_urls:
        # 准备发送到 Modrinth 的请求数据
        headers = {
            'Authorization': f'Bearer {MODRINTH_API_KEY}',
            'Content-Type': 'application/json',
        }
        data = {
            "version_number": VERSION_NUMBER,  # 自动获取的版本号
            "loaders": LOADERS,  # 从环境变量获取加载器
            "game_versions": GAME_VERSIONS,  # 从环境变量获取游戏版本
            "files": [{"url": asset_url}],
            # 你可以在此添加更多参数，如 changelog、version_type 等
        }

        # 发送请求到 Modrinth
        response = requests.post(MODRINTH_API_URL, json=data, headers=headers)

        if response.status_code == 201:
            print(f"Successfully uploaded {asset_url} to Modrinth.")
        else:
            print(f"Failed to upload {asset_url}: {response.text}")

if __name__ == '__main__':
    upload_to_modrinth()