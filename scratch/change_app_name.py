import os
import re

PROJECT_ROOT = r"c:\csc-project\livescore"
NEW_APP_NAME = "Football Live Score: MatchLive"

def update_strings_xml():
    res_path = os.path.join(PROJECT_ROOT, "app", "src", "main", "res")
    for root, dirs, files in os.walk(res_path):
        for file in files:
            if file == "strings.xml":
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, "r", encoding="utf-8") as f:
                        content = f.read()
                    
                    # Pattern to match <string name="app_name">...</string>
                    new_content, count = re.subn(
                        r'<string name="app_name">.*?</string>',
                        f'<string name="app_name">{NEW_APP_NAME}</string>',
                        content
                    )
                    
                    if count > 0:
                        with open(file_path, "w", encoding="utf-8") as f:
                            f.write(new_content)
                        print(f"Updated app name in {file_path}")
                except Exception as e:
                    print(f"Error processing {file_path}: {e}")

if __name__ == "__main__":
    update_strings_xml()
    print("App name updated successfully in all strings.xml files!")
