// addPathComments.js
import { promises as fs } from "fs";
import { resolve, relative, extname } from "path";
import fg from "fast-glob";

// Regex to detect existing path comments for JS/Kotlin (//) and XML ()
const pathCommentRegex = /^\s*(\/\/|)?/;

async function prependCommentToFile(filePath) {
  const fileExtension = extname(filePath);
  const absPath = resolve(filePath);
  const projectRoot = process.cwd();
  const relPath = relative(projectRoot, absPath);

  let commentLine;
  // Determine the correct comment syntax based on file extension
  switch (fileExtension) {
    case ".js":
    case ".kt":
      commentLine = `// ${relPath}\n`;
      break;
    case ".xml":
      commentLine = `\n`;
      break;
    default:
      // Silently skip unsupported file types that might slip through the glob
      return;
  }

  let content = await fs.readFile(absPath, "utf8");

  // Check the first few lines for an existing path comment
  const firstLines = content.split(/\r?\n/).slice(0, 5);
  const hasPathComment = firstLines.some(line => pathCommentRegex.test(line));

  if (hasPathComment) {
    console.log(`Skipping (already has comment): ${relPath}`);
    return;
  }

  await fs.writeFile(absPath, commentLine + content, "utf8");
  console.log(`Prepended comment to ${relPath}`);
}

async function main() {
  // Updated glob to include .js, .kt, and .xml files
  const entries = await fg("**/*.{js,kt,xml}", {
    dot: true,
    ignore: ["node_modules/**"],
  });

  await Promise.all(entries.map(prependCommentToFile));
  console.log("Done!");
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
