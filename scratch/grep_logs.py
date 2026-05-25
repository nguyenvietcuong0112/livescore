import os
import json

logs_path = r"C:\Users\CuongNV1\.gemini\antigravity\brain\ec1cf059-d228-49e0-b51a-ff05b413211c\.system_generated\logs\transcript.jsonl"

target_lines = [341, 363, 375, 379, 383]

with open(logs_path, "r", encoding="utf-8") as f:
    for i, line in enumerate(f):
        if i in target_lines:
            print(f"--- Line {i} ---")
            try:
                data = json.loads(line)
                tool_calls = data.get("tool_calls", [])
                for tc in tool_calls:
                    print(f"  Tool Name: {tc.get('name')}")
                    args = tc.get('arguments', {})
                    for k, v in args.items():
                        # We print fully if it's layout or code content
                        print(f"    {k}: {str(v)}")
            except Exception as e:
                print(f"Error parsing line {i}: {e}")
