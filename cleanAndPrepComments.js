import { promises as fs } from "fs";
import { resolve, relative, extname, posix } from "path";
import fg from "fast-glob";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const cwd = process.cwd();
const scriptRelPath = relative(cwd, __filename).replace(/\\/g, '/');

// Utilities
function toPosixPath(path) {
  return path.replace(/\\/g, '/');
}

function getFileType(ext) {
  ext = ext.toLowerCase();
  if (ext === '.js' || ext === '.kt') return 'code';
  if (ext === '.xml') return 'xml';
  return null;
}

function buildExactPathCommentRegex(commentLine) {
  const escaped = commentLine
    .trim()
    .replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    .replace(/-->$/, '-->\\s*$');
  return new RegExp(`^\\s*${escaped}`, 'm');
}

async function processFile(filePath) {
  const absPath = resolve(filePath);
  const relPath = toPosixPath(relative(cwd, absPath));
  const ext = extname(filePath);
  const fileType = getFileType(ext);
  if (!fileType) return;

  let content;
  try {
    content = await fs.readFile(absPath, 'utf8');
  } catch (err) {
    console.error(`Failed to read: ${relPath}`, err);
    return;
  }

  let commentLine = fileType === 'xml'
    ? `<!-- @path: ${relPath} -->\n`
    : `// @path: ${relPath}\n`;

  const pathCommentRegex = buildExactPathCommentRegex(commentLine);
  const header = content.slice(0, 500);

  // Prepend path comment if not already present
  if (!pathCommentRegex.test(header)) {
    if (fileType === 'xml' && content.startsWith('<?xml')) {
      const endDecl = content.indexOf('?>');
      if (endDecl !== -1) {
        const before = content.slice(0, endDecl + 2);
        const after = content.slice(endDecl + 2).replace(/^\r?\n/, '');
        content = `${before}\n${commentLine}${after}`;
      } else {
        content = `${commentLine}${content}`;
      }
    } else {
      content = `${commentLine}${content}`;
    }
    console.log(`Prepended @path to: ${relPath}`);
  } else {
    console.log(`Skipping (already has @path): ${relPath}`);
  }

  // Remove unwanted comments
  if (fileType === 'code') {
    content = content
      .replace(/\/\*[\s\S]*?\*\//g, m => m.includes('@path:') ? m : '')
      .replace(/^\s*\/\/.*$/gm, line => line.includes('@path:') ? line : '')
      .replace(/([^:"'\n])\/\/(?!.*@path:).*$/gm, (_, p) => p.trimEnd())
      .replace(/\[cite(?:_start|_end)?(?:\s*:\s*\d+)?\]/g, '');
  } else if (fileType === 'xml') {
    content = content.replace(/<!--[\s\S]*?-->/g, m => m.includes('@path:') ? m : '');
  }

  // Normalize spacing
  content = content.replace(/\n{3,}/g, '\n\n');
  if (!content.endsWith('\n')) content += '\n';

  try {
    await fs.writeFile(absPath, content, 'utf8');
    console.log(`Cleaned: ${relPath}`);
  } catch (err) {
    console.error(`Failed to write: ${relPath}`, err);
  }
}

async function main() {
  const pattern = process.argv[2] || '**/*.{js,kt,xml}';
  let entries = await fg(pattern, {
    dot: true,
    ignore: ['node_modules/**'],
  });

  entries = entries.map(toPosixPath).filter(f => f !== scriptRelPath);

  if (!entries.length) {
    console.warn('No files found for pattern:', pattern);
    return;
  }

  // Process files in parallel (limited concurrency could be added here)
  await Promise.allSettled(entries.map(processFile));

  console.log('All done!');
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
