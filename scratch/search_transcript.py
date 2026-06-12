import json
import os

log_path = r"C:\Users\CuongNV1\.gemini\antigravity\brain\b8204b72-95fb-44b9-9184-443b3b17945d\.system_generated\logs\transcript.jsonl"

if not os.path.exists(log_path):
    print("Log path does not exist:", log_path)
    exit()

print("Searching in transcript.jsonl...")
with open(log_path, 'r', encoding='utf-8') as f:
    for i, line in enumerate(f):
        if "South Korea" in line or "Czech" in line or "Mexico" in line:
            print(f"Line {i}: {line[:1000]}...")

