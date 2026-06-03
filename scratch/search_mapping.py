import os

mapping_path = r'c:\csc-project\livescore\app\build\outputs\mapping\release\mapping.txt'
if os.path.exists(mapping_path):
    print("Found mapping.txt")
    with open(mapping_path, 'r', encoding='utf-8') as f:
        for line_num, line in enumerate(f, 1):
            if 'CryptoUtils' in line:
                print(f"{line_num}: {line.strip()}")
else:
    print("mapping.txt not found")
