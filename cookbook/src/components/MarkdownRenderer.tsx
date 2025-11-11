import ReactMarkdown from 'react-markdown';
import type { Components } from 'react-markdown';
import type { DetailedHTMLProps, HTMLAttributes, ReactNode } from 'react';
import remarkGfm from 'remark-gfm';
import rehypeSlug from 'rehype-slug';
import rehypeAutolinkHeadings from 'rehype-autolink-headings';

interface MarkdownRendererProps {
  content: string;
}

type CodeProps = DetailedHTMLProps<HTMLAttributes<HTMLElement>, HTMLElement> & {
  inline?: boolean;
  children?: ReactNode;
};

const Code = ({ inline, className, children, ...props }: CodeProps) => {
  const languageMatch =
    typeof className === 'string' ? /language-(\w+)/.exec(className) : null;
  const languageClass = languageMatch ? `language-${languageMatch[1]}` : undefined;

  if (inline) {
    return (
      <code {...props} className="markdown-inline-code">
        {children}
      </code>
    );
  }

  return (
    <pre className={languageClass}>
      <code {...props} className="markdown-code-block">
        {children}
      </code>
    </pre>
  );
};

// Custom paragraph component to avoid wrapping code blocks in <p>
const Paragraph = ({ children, node, ...props }: DetailedHTMLProps<HTMLAttributes<HTMLParagraphElement>, HTMLParagraphElement> & { node?: any }) => {
  // Check if AST node contains code blocks (non-inline code)
  // If paragraph contains code blocks, don't wrap with <p> tag
  if (node && node.children) {
    // Check if there's a code element that's not inline (inline code's parent is usually text)
    const hasCodeBlock = node.children.some((child: any) => {
      // Check if it's a code element
      if (child.type === 'element' && child.tagName === 'code') {
        // Check if there's a className (code blocks usually have language-xxx class name)
        const className = child.properties?.className;
        if (Array.isArray(className)) {
          // If there's a language-xxx class name, it's a code block
          return className.some((cls: string) => typeof cls === 'string' && cls.startsWith('language-'));
        }
        // Check if it's a code block: if code element has properties.data, it's usually a code block
        // Or check if parent element is pre (shouldn't happen, but as a fallback check)
        return false;
      }
      // Check if it's a pre element
      if (child.type === 'element' && child.tagName === 'pre') {
        return true;
      }
      return false;
    });
    
    // If paragraph only contains one child element and it's a code block, don't wrap with <p> either
    const isSingleCodeBlock = node.children.length === 1 && 
      node.children[0].type === 'element' && 
      (node.children[0].tagName === 'code' || node.children[0].tagName === 'pre');
    
    if (hasCodeBlock || isSingleCodeBlock) {
      // Return children directly without wrapping in <p>
      return <>{children}</>;
    }
  }

  // Fallback check: check if there's a pre element in React children
  // This may be checked during rendering
  const checkForPreInChildren = (children: ReactNode): boolean => {
    if (!children) return false;
    
    if (Array.isArray(children)) {
      return children.some(checkForPreInChildren);
    }
    
    if (typeof children === 'object' && children !== null) {
      // Check if it's a React element
      if ('type' in children) {
        const type = (children as any).type;
        // Check if it's a pre element
        if (type === 'pre' || (typeof type === 'string' && type.toLowerCase() === 'pre')) {
          return true;
        }
        // Check if it's a Code component (non-inline)
        if (typeof type === 'function' && type.name === 'Code') {
          // Check if there's an inline property in props
          const props = (children as any).props;
          if (props && props.inline !== true) {
            // This is a code block, not inline code
            return true;
          }
        }
        // Recursively check child elements
        const nodeWithProps = children as { props?: { children?: ReactNode } };
        if (nodeWithProps.props && 'children' in nodeWithProps.props) {
          return checkForPreInChildren(nodeWithProps.props.children);
        }
      }
    }
    return false;
  };

  const hasPre = checkForPreInChildren(children);

  if (hasPre) {
    // If contains <pre>, return children directly without wrapping in <p>
    return <>{children}</>;
  }

  return <p {...props} className="markdown-paragraph">{children}</p>;
};

const markdownComponents: Components = {
  h1: ({ node, ...props }) => <h1 {...props} className="markdown-heading" />,
  h2: ({ node, ...props }) => <h2 {...props} className="markdown-heading" />,
  h3: ({ node, ...props }) => <h3 {...props} className="markdown-heading" />,
  h4: ({ node, ...props }) => <h4 {...props} className="markdown-heading" />,
  h5: ({ node, ...props }) => <h5 {...props} className="markdown-heading" />,
  h6: ({ node, ...props }) => <h6 {...props} className="markdown-heading" />,
  a: ({ node, ...props }) => <a {...props} className="markdown-link" />,
  p: Paragraph,
  code: Code,
  table: ({ node, ...props }) => <table {...props} className="markdown-table" />,
  th: ({ node, ...props }) => <th {...props} className="markdown-table-header" />,
  td: ({ node, ...props }) => <td {...props} className="markdown-table-cell" />,
};

export function MarkdownRenderer({ content }: MarkdownRendererProps) {
  return (
    <div className="markdown-body">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[
          rehypeSlug,
          [
            rehypeAutolinkHeadings,
            {
              behavior: 'wrap',
            },
          ],
        ]}
        components={markdownComponents}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}

export default MarkdownRenderer;

