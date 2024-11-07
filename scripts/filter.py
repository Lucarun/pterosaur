import re

def filter_boolean_parameter_methods():
    folder_path = '/Users/luca/dev/2025/pterosaur/output/popular-components/hutool-core/output/'
    # 打开并读取 a.txt 文件内容
    with open(f'{folder_path}tpl_sort.txt', 'r') as input_file:
        lines = input_file.readlines()

    # 打开 b.txt 文件用于写入过滤后的内容
    with open(f'{folder_path}filter-boolean.txt', 'w') as output_file:
        for line in lines:
            # 将行分割为 "<---" 前后的两部分
            parts = line.split('<---')
            if len(parts) < 2:
                continue  # 如果没有 <---，跳过该行

            # 仅检查 "<---" 前面的部分是否包含参数 java.lang.Boolean
            left_part = parts[0]
            if re.search(r'\(.*?(java\.lang\.Boolean|boolean).*?\)', left_part):
                output_file.write(line)

# 执行过滤函数
if __name__ == '__main__':
    filter_boolean_parameter_methods()
