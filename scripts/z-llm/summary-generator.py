import copy
import json

from prettytable import PrettyTable
import time
import openai
from tqdm import tqdm
import itertools
import wandb
from tenacity import retry, stop_after_attempt, wait_exponential
import os
import parser

api_key = "sk-yOWN4OQzmCgLBemn8e6355340b9e4b438c13632aC7D6Ad37"  # enter your OpenAI API key here
EVAL_MODEL = "gpt-4o"

use_wandb = False  # set to True if you want to use wandb to log your config and results

use_portkey = False  # set to True if you want to use Portkey to log all the prompt chains and their responses Check https://portkey
# .ai/

EVAL_MODEL_TEMPERATURE = 0.2
EVAL_MODEL_MAX_TOKENS = 1000

from openai import OpenAI

client = OpenAI(
    # defaults to os.environ.get("OPENAI_API_KEY")
    base_url="https://api.zhiyungpt.com/v1",
    api_key=api_key,
)

prompts = [{
    "role": "system",
    "content": """
You are a professional program analysis aid to summarize what the target method is doing and
what kind of data flow there in relates to the target method. """},
    {"role": "user",
     "content": """

Please adhere to the following conventions in your summary:

(1)Parameter Notation: Use p1,p2,p3 to represent the first, second, third parameters of the method
E.g.: 'p1' represents 'a', 'p2' represents 'b', there are no 'p3 or p4'
private void assignValue(String a, String b){
    a = b
}

(2)Object Reference: Use this to refer to the current object instance.
E.g.: 'this' represents the current object instantiated through class A
public class A{
    String name;
    private void setValue(String a){
        this.name = a;
    }
}

(3)Field Access: Use dot notation to represent nested fields, e.g., this.labelA.labelB
refers to field labelB within labelA of this.
E.g.: this.foo.name
public class A{
    Foo foo;
    private void assignValue(String a){
        this.foo.name = a;
    }
    
    class Foo{
        String name;
    }
}


A summary for a leaf procedure has been provided as a comment on the
corresponding call-site line within the code. You should:
(1)Evaluate the Condition: First, determine whether the condition specified in the
comment is satisfied within the current code context.
(2)Generate Summary: If the condition is satisfied, use it to help generate a
summary for the current code block.
(3)Ignore Irrelevant Dataflows: If the condition is not satisfied, disregard this
dataflow in your summary.
(4)Handle Uncertainty: If you are unsure whether the condition is satisfied,
reorganize and reinterpret the condition in a manner that aligns with the input
states of the current code.




Here is a few examples，and the analysis result will provide a summary in the form of annotations above the method.
Data Flow Summary and conditions should only contains the flow about param, field of the param, this, filed of this, ret, 
field of the ret which should not include local variables in the method. （Please pay attention to this rule!!!）.

There are several predetermined conditions here.
(1)null checks : CT1
e.g. 
if (p1 != null){ p2 = p1 }

(2)value comparisons : CT2
e.g. 
if (p1.length > 0){ p2 = p1 }
e.g. 
if (p1.flag){ p2 = p1 }
e.g. 
if (p1 == 10){ p2 = p1 }

(3)collection membership : CT3
As long as it involves the judgment of collection elements, it can be considered as this type uniformly. 
The highest priority, even if it can be defined as CT1 or CT2
e.g. [p1 is a collection, p1[3] means the element with index 3]
if (p1[3].contains("pwd")){ p2 = p1 }

(4)class hierarchy : CT5
e.g. 
if (p1 instanceof Foo){ p2 = p1 }

(5)catch exception : CT6
e.g. 
try{
    p1.operateThrowException();
}catch(Exception ex){
    p2 = p1
}
(6)others : OCT
unknown condition
(7)No condition : NOC
e.g.
void call(String p1, String p2){
   p2 = p1;
}



Example 1 :

[Code to be analyzed]:

package com.test;
public class A{
    Flag flag;
    String call(String a, Foo b, int c){
        String tmp1 = a + "test1";
        String tmp2 = a + "test2";
        String finalStr = a + "user";
        if (!flag.value.equals("ABC")){
            if (flag.name.equals(b.name)){
                assignValueMethodA(tmp1, b); // This line involves a dataflow from 'p1(tmp1) to p2(b).name
            }
        }
        assignValueMethodB(tmp2, b); // This line involves a dataflow from p1(tmp2) to p2(b).content
        this.flag.score = c;
        return finalStr
    }
    class Flag{
        String value;
        String name;
        int score;
    }
}

package com.test;
public Foo{
    String name;
    String content;
}


[Positive Analysis Result]: Please pay more attention to !!! [If there are multiple parameter data streams, they should be recorded separately, such as p1-->this.f1, p2-->this.f2, there will be two PropagationRule xml records. <InArguments>This tag can only have one parameter</InArguments>] 

<PropagationRule>
            <RuleID>RULE-ID-PROPAGATION-JHKLIBGFFG000008</RuleID>  # This RuleID should be randomly generated.The prefix "RULE-ID-PROPAGATION-" remains unchanged.
            <FunctionIdentifier>
                <NamespaceName>
                    <Value>com.test</Value>
                </NamespaceName>
                <ClassName>
                    <Value>A</Value>
                </ClassName>
                <FunctionName>
                    <Value>call</Value>
                </FunctionName>
                <ApplyTo implements="true" overrides="true" extends="true"/>
            </FunctionIdentifier>
            <Parameters>
                <ParamType>java.lang.String</ParamType>
                <ParamType>com.test.Foo</ParamType>
                <ParamType>int</ParamType>
            </Parameters>
            <HasPropagation>true</HasPropagation>
            <Condition>!this.flag.value.equals(p1) && this.flag.name.equals(p1.name)</Condition>
            <InArguments>0</InArguments> # If there are multiple parameter data streams, they should be recorded separately, such as p1-->this.f1, p2-->this.f2, there will be two PropagationRule xml records
            <OutArguments>1.name</OutArguments>
            <Comment>
            - The data flow shows that `p1` (`a`) is transferred into the `tmp1`, and the `tmp1` is transferred into 'p2'('b').name  
            - The flow is conditional on !this.flag.value.equals("ABC") && this.flag.name.equals(p2.name)
            </Comment>
</PropagationRule>

<PropagationRule>
            <RuleID>RULE-ID-PROPAGATION-00000002</RuleID>
            <FunctionIdentifier>
                <NamespaceName>
                    <Value>com.test</Value>
                </NamespaceName>
                <ClassName>
                    <Value>A</Value>
                </ClassName>
                <FunctionName>
                    <Value>call</Value>
                </FunctionName>
                <ApplyTo implements="true" overrides="true" extends="true"/>
            </FunctionIdentifier>
            <Parameters>
                <ParamType>java.lang.String</ParamType>
                <ParamType>com.test.Foo</ParamType>
                <ParamType>int</ParamType>
            </Parameters>
            <HasPropagation>true</HasPropagation>
            <Condition>NOC</Condition>
            <InArguments>0</InArguments>
            <OutArguments>1.content</OutArguments>
            <Comment>
            - The data flow shows that `p1` (`a`) is transferred into the `tmp2`, and the `tmp2` is transferred into 'p2'('b').content
            - The flow is no condition
            </Comment>
</PropagationRule>

<PropagationRule>
            <RuleID>RULE-ID-PROPAGATION-00000003</RuleID>
            <FunctionIdentifier>
                <NamespaceName>
                    <Value>com.test</Value>
                </NamespaceName>
                <ClassName>
                    <Value>A</Value>
                </ClassName>
                <FunctionName>
                    <Value>call</Value>
                </FunctionName>
                <ApplyTo implements="true" overrides="true" extends="true"/>
            </FunctionIdentifier>
            <Parameters>
                <ParamType>java.lang.String</ParamType>
                <ParamType>com.test.Foo</ParamType>
                <ParamType>int</ParamType>
            </Parameters>
            <HasPropagation>true</HasPropagation>
            <Condition>NOC</Condition>
            <InArguments>2</InArguments>
            <OutArguments>this.flag.score</OutArguments>
            <Comment>
            - The data flow shows that `p3` (`c`) is transferred into this.flag.score
            - The flow is no condition
            </Comment>
</PropagationRule>


<PropagationRule>
            <RuleID>RULE-ID-PROPAGATION-00000003</RuleID>
            <FunctionIdentifier>
                <NamespaceName>
                    <Value>com.test</Value>
                </NamespaceName>
                <ClassName>
                    <Value>A</Value>
                </ClassName>
                <FunctionName>
                    <Value>call</Value>
                </FunctionName>
                <ApplyTo implements="true" overrides="true" extends="true"/>
            </FunctionIdentifier>
            <Parameters>
                <ParamType>java.lang.String</ParamType>
                <ParamType>com.test.Foo</ParamType>
                <ParamType>int</ParamType>
            </Parameters>
            <HasPropagation>true</HasPropagation>
            <Condition>NOC</Condition>
            <InArguments>0</InArguments>
            <OutArguments>return</OutArguments>
            <Comment>
            - The data flow shows that `p1` (`a`) is transferred into 'return'
            - The flow is no condition
            </Comment>
</PropagationRule>


Example 2 :

[Code to be analyzed]: <com.test:A void call(java.lang.String)>

package com.test;
public class A{
    void call(String a){
        String tmp1 = a + "test1";
        System.out.println(tmp1);
    }
}


[Positive Analysis Result]:

<PropagationRule>
            <RuleID>RULE-ID-PROPAGATION-00000002</RuleID>
            <FunctionIdentifier>
                <NamespaceName>
                    <Value>com.test</Value>
                </NamespaceName>
                <ClassName>
                    <Value>A</Value>
                </ClassName>
                <FunctionName>
                    <Value>call</Value>
                </FunctionName>
                <ApplyTo implements="true" overrides="true" extends="true"/>
            </FunctionIdentifier>
            <Parameters>
                <ParamType>java.lang.String</ParamType>
            </Parameters>
            <HasPropagation>false</HasPropagation>
            <Comment>
            - There is no obvious data flow here as parameter p1 does not flow to other parameters, this, or return.
            </Comment>
</PropagationRule>



Now the final question, tell me the summary of this method. 
(1)Please provide the analysis results strictly in this XML format according to the example. 
(2)Don't forget to merge the result (p1-->a-->b--->p2 should be p1 --> p2)
(3)Please think step by step and provide the detailed analysis results for each step. 





"""
     }]



def get_analusis_code():
    analysis_code = """
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.rabbitmq.client;

public class Envelope {
    public long getDeliveryTag() {
        return this._deliveryTag;
    }

}
"""
    return analysis_code

def get_code_from_file(file_path):
    try:
        with open(file_path, 'r') as file:
            code = file.read()
            return code
    except FileNotFoundError:
        print(f"Error: The file {file_path} was not found.")
        return None
    except Exception as e:
        print(f"An error occurred while reading the file: {e}")
        return None


def generate():
    output_file = '/Users/luca/dev/2025/pterosaur/llm/output/conversation-amq.txt'
    methods = parser.parse_methods_by_file("/Users/luca/dev/2025/pterosaur/llm/input/code/pilot-8.txt")

    # 初始化原始prompts
    original_prompts = prompts

    # 遍历methods列表
    for analysis_code in methods:
        # 深拷贝prompts，保证每次循环都是独立的
        conversation = copy.deepcopy(original_prompts)

        # 拼接analysis_code到prompts数组中第二个元素的content后面
        conversation[1]["content"] += analysis_code

        # 调用GPT模型
        response = client.chat.completions.create(
            model=EVAL_MODEL,
            messages=conversation,
            max_tokens=EVAL_MODEL_MAX_TOKENS,  # 限制返回值大小到一两句话长度
            temperature=EVAL_MODEL_TEMPERATURE  # 使生成的文本更加简洁和准确
        ).choices[0].message.content

        # 在控制台输出返回值
        print(f"Response from GPT:\n{response}\n")  # 打印返回值

        # 保存当前对话到文件
        conversation.append(
            {
                "role": "assistant",
                "content": response
            }
        )
        save_conversation_to_file(output_file, conversation)


def save_conversation_to_file(filename, conversation):
    with open(filename, 'a', encoding='utf-8') as f:  # 使用 'a' 模式
        for entry in conversation:
            f.write(f"=== {entry['role']} ===\n")
            f.write(f"{entry['content']}\n")


if __name__ == "__main__":
    generate()
