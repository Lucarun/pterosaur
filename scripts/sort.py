from collections import defaultdict

def sort_methods_by_consistency():
    # 定义文件路径
    folder_path = '/Users/luca/dev/2025/pterosaur/output/popular-components/hutool-core/output/'
    input_file = folder_path + 'tpl.txt'
    output_file = folder_path + 'tpl_sort.txt'

    # 创建一个字典，以 `--->` 前面的方法签名为键，将对应的行添加到列表
    method_groups = defaultdict(list)
    with open(input_file, 'r') as file:
        lines = file.readlines()
        for line in lines:
            if '--->' in line:
                parts = line.split('--->')
                if len(parts) == 2:
                    after_signature = parts[1].strip()  # 获取 `--->` 前的方法签名
                    # 将行内容按 `before_signature` 归类
                    method_groups[after_signature].append(parts[1].strip() + "<---" + parts[0])

    # 按 `before_signature` 出现的次数从高到低排序
    sorted_methods = sorted(method_groups.items(), key=lambda x: len(x[1]), reverse=True)

    # 将排序后的结果写入 `tpl_sort.txt`
    with open(output_file, 'w') as file:
        for before_signature, grouped_lines in sorted_methods:
            for line in grouped_lines:
                file.write(line + '\n')

    print(f"已按一致性数量排序并写入文件: {output_file}")

# 执行函数
if __name__ == '__main__':
    sort_methods_by_consistency()
