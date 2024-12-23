import re

def escape_condition(condition):
    """替换 XML 中的 `&` 符号为 `&amp;`"""
    # 使用正则替换所有 `&` 为 `&amp;`
    return re.sub(r'&', '&amp;', condition)

def update_rule_ids(file_path, start_number):
    """更新文件中所有的 RuleID，根据给定的起始数字递增"""
    # 读取文件内容
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()

    # 使用正则表达式匹配所有的 RuleID
    pattern = re.compile(r'RULE-ID-PROPAGATION-(\d{8})')

    # 替换 RuleID
    def replacement(match):
        nonlocal start_number
        # 获取新的 RuleID
        new_rule_id, start_number = increment_rule_id(match, start_number)
        return new_rule_id

    # 替换所有 RuleID
    updated_content = re.sub(pattern, replacement, content)

    # 替换 <Condition> 标签中的 & 为 &amp;
    updated_content = re.sub(r'<Condition>(.*?)</Condition>', lambda m: f'<Condition>{escape_condition(m.group(1))}</Condition>', updated_content)

    # 将修改后的内容写回文件
    with open(file_path, 'w', encoding='utf-8') as file:
        file.write(updated_content)

def increment_rule_id(match, start_number):
    """根据给定的起始数字递增 RuleID"""
    # 获取当前 RuleID 的数字部分
    current_id = int(match.group(1))
    # 计算新的 RuleID
    new_id = start_number
    start_number += 1
    # 格式化为 8 位数字
    return f'RULE-ID-PROPAGATION-{new_id:08d}', start_number

def main():
    # 设置文件路径和起始的 RuleID
    file_path = "/Users/luca/dev/2025/pterosaur/llm/output/rules/propagation-rule-guava.txt"  # 这里可以修改为你的文件路径
    start_number = 100  # 设置开始的 RuleID 数字

    # 调用函数更新文件
    update_rule_ids(file_path, start_number)
    print(f"File '{file_path}' has been updated with RuleIDs starting from {start_number}.")

if __name__ == '__main__':
    main()
