def generate_dependencies():
    # 读取 a.txt 文件
    with open('/Users/luca/dev/2025/pterosaur/scripts/hutool-core/pilots.txt', 'r') as input_file:
        lines = input_file.readlines()

    # 写入生成的依赖到 output.txt
    with open('/Users/luca/dev/2025/pterosaur/scripts/hutool-core/pom.xml', 'w') as output_file:
        for line in lines:
            line = line.strip()
            if not line:
                continue  # 跳过空行

            # 按照冒号分割每一行，分别提取 group, artifact 和 version
            parts = line.split(':')
            group = parts[0] if len(parts) > 0 else ''
            artifact = parts[1] if len(parts) > 1 else ''
            version = parts[2] if len(parts) > 2 else 'latest'  # 缺少版本时使用 "latest"

            # 写入到 output.txt 文件中，格式化成 Maven 依赖格式
            dependency = f"""<dependency>
    <groupId>{group}</groupId>
    <artifactId>{artifact}</artifactId>
    <version>{version}</version>
</dependency>
"""
            output_file.write(dependency + "\n")

# 执行函数
if __name__ == '__main__':
    generate_dependencies()
