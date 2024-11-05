import requests
import os

def download_jar_from_maven_central(group_id, artifact_id, version, output_dir):
    group_path = group_id.replace('.', '/')
    jar_url = f"https://repo1.maven.org/maven2/{group_path}/{artifact_id}/{version}/{artifact_id}-{version}.jar"

    os.makedirs(output_dir, exist_ok=True)
    jar_path = os.path.join(output_dir, f"{artifact_id}-{version}.jar")

    response = requests.get(jar_url, stream=True)
    if response.status_code == 200:
        with open(jar_path, 'wb') as file:
            for chunk in response.iter_content(chunk_size=1024):
                file.write(chunk)
        print(f"Downloaded {artifact_id}-{version}.jar to {output_dir}")
    else:
        print(f"Failed to download {artifact_id}-{version}.jar:", response.status_code)

def main():
    input_file = "pilots.txt"  # 包含 GAV 列表的文件
    output_dir = "/Users/luca/dev/2025/pterosaur/output/popular-components"  # 下载的 jar 包存放路径

    # 读取 a.txt 文件
    with open(input_file, 'r') as file:
        for line in file:
            # 去除行尾的换行符并分割 GAV 信息
            gav = line.strip()
            if gav:
                parts = gav.split(':')
                if len(parts) == 3:
                    group_id, artifact_id, version = parts
                    print(f"Downloading {group_id}:{artifact_id}:{version}...")
                    download_jar_from_maven_central(group_id, artifact_id, version, output_dir)
                else:
                    print(f"Invalid GAV format in line: {gav}")

if __name__ == "__main__":
    main()
