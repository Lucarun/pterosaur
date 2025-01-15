import re
import xml.etree.ElementTree as ET

def escape_condition(condition):
    """转义Condition标签中的特殊字符"""
    condition = condition.replace("<=", "&lt;=").replace(">=", "&gt;=")
    condition = condition.replace("&", "&amp;")
    condition = re.sub(r'(?<=\s)<(?=\s)', '&lt;', condition)
    condition = re.sub(r'(?<=\s)>(?=\s)', '&gt;', condition)
    return condition

def replace_special_tags(content):
    """替换特殊标签"""
    content = content.replace("&lt;init&gt;", "init")
    content = content.replace("<init>", "&lt;init&gt;")
    return content

def check_out_arguments(content):
    """检查 OutArguments 并返回有问题的 RuleID"""
    problematic_rule_ids = []
    root = ET.fromstring(content)

    for rule in root.findall('PropagationRule'):
        rule_id = rule.find('RuleID').text
        out_arguments = rule.find('OutArguments').text

        if out_arguments == 'service fields in service implementations':
            problematic_rule_ids.append(rule_id)

    return problematic_rule_ids

def update_rule_ids(file_path, start_number):
    """更新文件中所有的 RuleID，根据给定的起始数字递增"""
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()

    # 检查 OutArguments
    problematic_ids = check_out_arguments(content)
    if problematic_ids:
        print("有问题的 RuleID:", problematic_ids)

    pattern = re.compile(r'RULE-ID-PROPAGATION-(\d{8})')

    def replacement(match):
        nonlocal start_number
        new_rule_id, start_number = increment_rule_id(match, start_number)
        return new_rule_id

    updated_content = re.sub(pattern, replacement, content)
    updated_content = re.sub(r'<Condition>(.*?)</Condition>', lambda m: f'<Condition>{escape_condition(m.group(1))}</Condition>', updated_content)
    updated_content = replace_special_tags(updated_content)

    with open(file_path, 'w', encoding='utf-8') as file:
        file.write(updated_content)

def increment_rule_id(match, start_number):
    """根据给定的起始数字递增 RuleID"""
    current_id = int(match.group(1))
    new_id = start_number
    start_number += 1
    return f'RULE-ID-PROPAGATION-{new_id:08d}', start_number

def main():
    file_path = "/Users/luca/dev/2025/pterosaur/llm/output/rules/propagation-rule-weixin-java-miniapp.txt"  # 这里可以修改为你的文件路径
    start_number = 1  # 设置开始的 RuleID 数字

    update_rule_ids(file_path, start_number)
    print(f"File '{file_path}' has been updated with RuleIDs starting from {start_number}.")

if __name__ == '__main__':
    main()