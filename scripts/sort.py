import os
from collections import defaultdict

def group_and_sort_methods_by_signature():
    folder_path = '/Users/luca/dev/2025/pterosaur/output/popular-components/hutool-core/output/'
    # 读取 a.txt 文件中的数据
    with open(f'{folder_path}tpl.txt', 'r') as file:
        lines = file.readlines()

    # 创建一个字典，以 `--->` 后面的方法签名为键，将对应的原始行作为值的列表
    method_groups = defaultdict(list)
    for line in lines:
        if '--->' in line:
            # 将 `--->` 前后的部分分离
            parts = line.split('--->')
            if len(parts) == 2:
                method_signature = parts[1].strip()  # 获取 `--->` 后面的部分
                method_groups[method_signature].append(parts[1].strip() + " <--- " + parts[0].strip())

    target_file = folder_path + 'tpl_sort.txt';
    # 对方法签名排序并写入 b.txt
    with open(target_file, 'w') as file:
        for method_signature in sorted(method_groups.keys()):
            for original_line in method_groups[method_signature]:
                file.write(original_line + '\n')

# 执行函数
if __name__ == '__main__':
    group_and_sort_methods_by_signature()
