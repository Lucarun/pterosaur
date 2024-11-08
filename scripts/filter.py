import re
from collections import defaultdict

def filter_parameter_methods(pattern, file_path):
    folder_path = '/Users/luca/dev/2025/pterosaur/output/popular-components/hutool-core/output/'
    # 打开并读取 tpl_sort.txt 文件内容
    with open(f'{folder_path}tpl_sort.txt', 'r') as input_file:
        lines = input_file.readlines()

    # 打开指定的输出文件用于写入过滤后的内容
    with open(f'{folder_path}{file_path}', 'w') as output_file:
        for line in lines:
            # 将行分割为 "<---" 前后的两部分
            parts = line.split('<---')
            if len(parts) < 2:
                continue  # 如果没有 <---，跳过该行

            # 仅检查 "<---" 前面的部分是否包含指定的参数类型
            left_part = parts[0]
            if re.search(pattern, left_part):
                output_file.write(line)

# 执行过滤函数
if __name__ == '__main__':
    # 使用更严格的正则表达式来匹配独立的 `boolean` 或 `java.lang.Boolean` 参数
    # filter_parameter_methods(r"\((?:[^)]*,\s*)*(?:java\.lang\.Boolean|boolean)(?:\s*,[^)]*)*\)", "filter_boolean.txt")
    filter_parameter_methods(r"\((?:[^)]*,\s*)*(?:java\.lang\.Integer|int)(?:\s*,[^)]*)*\)", "filter_int.txt")
