import GithubSlugger from 'github-slugger';

export interface Heading {
  depth: number;
  text: string;
  slug: string;
}

const headingRegex = /^(#{1,6})\s+(.+)$/gm;

export function extractHeadings(markdown: string, maxDepth = 3): Heading[] {
  const slugger = new GithubSlugger();
  const headings: Heading[] = [];

  let match: RegExpExecArray | null;

  while ((match = headingRegex.exec(markdown))) {
    const [, hashes, rawText] = match;
    const depth = hashes.length;

    if (depth > maxDepth) {
      continue;
    }

    const text = rawText.trim();
    const slug = slugger.slug(text);

    headings.push({ depth, text, slug });
  }

  return headings;
}

export function extractTitle(markdown: string): string | null {
  const match = /^#\s+(.+)$/m.exec(markdown);
  return match ? match[1].trim() : null;
}

export function fallbackTitleFromPath(path: string): string {
  const withoutExt = path.replace(/\.md$/i, '');
  const fileName = withoutExt.split('/').pop() ?? withoutExt;

  return fileName
    .split(/[-_]/g)
    .map((chunk) => chunk.charAt(0).toUpperCase() + chunk.slice(1))
    .join(' ');
}
















