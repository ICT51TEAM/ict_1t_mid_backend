const fs = require('fs');
const path = require('path');

const baseDir = path.join(__dirname, 'src', 'main', 'java', 'com', 'example', 'backend');

let count = 0;

function walkDir(dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            walkDir(fullPath);
        } else if (fullPath.endsWith('.java')) {
            let content;
            try {
                content = fs.readFileSync(fullPath, 'utf8');
            } catch (e) {
                console.error(`Skipping ${fullPath} due to encoding issue.`);
                continue;
            }

            let newContent = content.replace(/com\.project\.semi\.domain\./g, 'com.example.backend.');
            newContent = newContent.replace(/com\.project\.semi\./g, 'com.example.backend.');

            if (content !== newContent) {
                fs.writeFileSync(fullPath, newContent, 'utf8');
                count++;
                console.log(`Updated ${fullPath}`);
            }
        }
    }
}

walkDir(baseDir);
console.log(`Total files updated: ${count}`);
