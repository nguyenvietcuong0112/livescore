import json
import os

log_path = r"C:\Users\CuongNV1\.gemini\antigravity\brain\b8486f3a-2c4b-43ae-9d99-11055a48308c\.system_generated\logs\transcript.jsonl"

if not os.path.exists(log_path):
    print("Log path does not exist:", log_path)
    exit()

print("Searching in transcript.jsonl...")
with open(log_path, 'r', encoding='utf-8') as f:
    for i, line in enumerate(f):
        if "Carolina Ascent" in line:
            print(f"Line {i}: {line[:500]}...")
            # Let's find the exact JSON block
            if "data" in line:
                try:
                    data = json.loads(line)
                    # Try to find the exact match details
                    content = data.get("content", "")
                    if "Carolina Ascent" in content:
                        print("FOUND IN CONTENT:")
                        print(content[:1000])
                except:
                    pass
