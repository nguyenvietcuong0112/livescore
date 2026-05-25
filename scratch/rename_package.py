import os
import shutil

PROJECT_ROOT = r"c:\csc-project\livescore"
OLD_PKG = "com.livescore.app.myapplication.livescore"
NEW_PKG = "com.livescore.football.livescores.footballscores"

def replace_in_file(file_path):
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()
        
        new_content = content.replace(OLD_PKG, NEW_PKG)
        
        # Also replace path style package references if any (e.g. com/livescore/app/...)
        old_path_style = OLD_PKG.replace(".", "/")
        new_path_style = NEW_PKG.replace(".", "/")
        new_content = new_content.replace(old_path_style, new_path_style)
        
        if content != new_content:
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(new_content)
            print(f"Updated: {file_path}")
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

def update_file_contents():
    print("Performing global text search and replace...")
    for root, dirs, files in os.walk(PROJECT_ROOT):
        # Skip certain directories
        skip_dirs = [".git", ".gradle", ".idea", "build", "scratch", ".kotlin"]
        dirs[:] = [d for d in dirs if d not in skip_dirs]
        
        for file in files:
            ext = os.path.splitext(file)[1]
            if ext in [".kt", ".xml", ".kts", ".properties", ".pro"]:
                file_path = os.path.join(root, file)
                replace_in_file(file_path)

def move_directories(source_set_dir):
    # source_set_dir can be "main", "test", "androidTest"
    old_dir_path = os.path.join(PROJECT_ROOT, "app", "src", source_set_dir, "java", *OLD_PKG.split("."))
    new_dir_path = os.path.join(PROJECT_ROOT, "app", "src", source_set_dir, "java", *NEW_PKG.split("."))
    
    if not os.path.exists(old_dir_path):
        print(f"Source directory does not exist, skipping: {old_dir_path}")
        return
        
    print(f"Moving {source_set_dir} directories from {old_dir_path} to {new_dir_path}...")
    os.makedirs(new_dir_path, exist_ok=True)
    
    for item in os.listdir(old_dir_path):
        s_item = os.path.join(old_dir_path, item)
        d_item = os.path.join(new_dir_path, item)
        if os.path.isdir(s_item):
            if os.path.exists(d_item):
                # Merge directories
                for sub_item in os.listdir(s_item):
                    shutil.move(os.path.join(s_item, sub_item), os.path.join(d_item, sub_item))
                os.rmdir(s_item)
            else:
                shutil.move(s_item, d_item)
        else:
            if os.path.exists(d_item):
                os.remove(d_item)
            shutil.move(s_item, d_item)
            
    # Clean up empty parent directories of the old package
    # OLD_PKG is "com.livescore.app.myapplication.livescore"
    # we want to delete livescore, myapplication, app under java/com/livescore
    parts = OLD_PKG.split(".")
    for i in range(len(parts), 2, -1):
        del_path = os.path.join(PROJECT_ROOT, "app", "src", source_set_dir, "java", *parts[:i])
        if os.path.exists(del_path) and not os.listdir(del_path):
            os.rmdir(del_path)
            print(f"Cleaned up empty directory: {del_path}")

if __name__ == "__main__":
    # 1. Update text content in all files
    update_file_contents()
    
    # 2. Move physical directories
    for source_set in ["main", "test", "androidTest"]:
        move_directories(source_set)
        
    print("Package rename script finished successfully!")
