import urllib.request
import json

url = "https://api.1teps.com/livescore/api/v1/leagues"
api_key = "eeb82da4384bf7352f346c9371fe3dad"

print("--- Request 1: Without Header ---")
try:
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        html = response.read()
        print(f"Status: {response.status}")
        data = json.loads(html.decode('utf-8'))
        print(f"Code: {data.get('code')}, Data count: {len(data.get('data', []))}")
except Exception as e:
    print(f"Failed: {e}")

print("\n--- Request 2: With x-apisports-key Header ---")
try:
    req = urllib.request.Request(url)
    req.add_header("x-apisports-key", api_key)
    with urllib.request.urlopen(req) as response:
        html = response.read()
        print(f"Status: {response.status}")
        data = json.loads(html.decode('utf-8'))
        print(f"Code: {data.get('code')}, Data count: {len(data.get('data', []))}")
except Exception as e:
    print(f"Failed: {e}")
