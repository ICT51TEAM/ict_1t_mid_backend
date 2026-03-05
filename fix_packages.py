import os

base_dir = r"c:\Users\ICT-09\Documents\GitHub\ICT5-semi-proj-own\semi_final_v2_1\backend\src\main\java\com\example\backend"

count = 0
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith(".java"):
            filepath = os.path.join(root, f)
            try:
                with open(filepath, 'r', encoding='utf-8') as file:
                    content = file.read()
            except UnicodeDecodeError:
                with open(filepath, 'r', encoding='euc-kr') as file:
                    content = file.read()
            
            new_content = content.replace("com.project.semi.domain.", "com.example.backend.")
            new_content = new_content.replace("com.project.semi.", "com.example.backend.")
            
            if content != new_content:
                try:
                    with open(filepath, 'w', encoding='utf-8') as file:
                        file.write(new_content)
                except Exception as e:
                    print(f"Failed to write {filepath}: {e}")
                else:
                    count += 1
                    print(f"Updated {filepath}")

print(f"Total files updated: {count}")
