import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Sidebar from '@/components/Sidebar';
import HeadingToc from '@/components/HeadingToc';
import MarkdownRenderer from '@/components/MarkdownRenderer';
import { extractHeadings, extractTitle, fallbackTitleFromPath, type Heading } from '@/utils/markdown';

type MarkdownModule = Record<string, string>;

type Language = 'zh' | 'en';

interface Doc {
  id: string;
  path: string;
  title: string;
  content: string;
  headings: Heading[];
  language: Language;
}

const zhMarkdownModules = import.meta.glob('../zh/**/*.md', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as MarkdownModule;

const enMarkdownModules = import.meta.glob('../en/**/*.md', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as MarkdownModule;

function parseInitialHash(
  ids: string[],
  languages: Language[],
): { docId: string | undefined; headingSlug?: string; language?: Language } {
  if (typeof window === 'undefined') {
    return { docId: ids[0], language: languages[0] };
  }
  const hash = window.location.hash.replace('#', '');
  if (!hash) {
    return { docId: ids[0], language: languages[0] };
  }

  const parts = hash.split(':').map((piece) => decodeURIComponent(piece));
  const firstPart = parts[0];
  
  // Check if it's a language-prefixed format (zh:docId or en:docId or zh:docId:headingSlug)
  if (languages.includes(firstPart as Language)) {
    const language = firstPart as Language;
    const docId = `${language}:${parts[1]}`;
    const headingSlug = parts[2];
    if (docId && ids.includes(docId)) {
      return { docId, headingSlug, language };
    }
    // If full ID not found, try using filename only
    const filename = parts[1];
    const fallbackId = ids.find((id) => id.startsWith(`${language}:`) && id.endsWith(`:${filename}`) || id === `${language}:${filename}`);
    if (fallbackId) {
      return { docId: fallbackId, headingSlug, language };
    }
    return { docId: ids.find((id) => id.startsWith(`${language}:`)) ?? ids[0], language };
  }

  // Compatible with old format (docId:headingSlug) or just docId
  const [docId, headingSlug] = parts;
  if (docId && ids.includes(docId)) {
    // Extract language from docId
    const lang = docId.startsWith('zh:') ? 'zh' : docId.startsWith('en:') ? 'en' : undefined;
    return { docId, headingSlug, language: lang };
  }

  return { docId: ids[0], language: languages[0] };
}

function App() {
  const allDocs = useMemo<Doc[]>(() => {
    const docs: Doc[] = [];

    // Load Chinese documents
    Object.entries(zhMarkdownModules).forEach(([path, content]) => {
      const normalizedPath = path.replace(/^\.\.\/zh\//, '').replace(/^\.\/zh\//, '');
      const id = `zh:${normalizedPath.replace(/\.md$/i, '')}`;
      const title = extractTitle(content) ?? fallbackTitleFromPath(normalizedPath);
      const headings = extractHeadings(content);

      docs.push({
        id,
        path: normalizedPath,
        title,
        content,
        headings,
        language: 'zh',
      });
    });

    // Load English documents
    Object.entries(enMarkdownModules).forEach(([path, content]) => {
      const normalizedPath = path.replace(/^\.\.\/en\//, '').replace(/^\.\/en\//, '');
      const id = `en:${normalizedPath.replace(/\.md$/i, '')}`;
      const title = extractTitle(content) ?? fallbackTitleFromPath(normalizedPath);
      const headings = extractHeadings(content);

      docs.push({
        id,
        path: normalizedPath,
        title,
        content,
        headings,
        language: 'en',
      });
    });


    return docs;
  }, []);

  const [language, setLanguage] = useState<Language>(() => {
    // Read from localStorage or default to Chinese
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('cookbook-language') as Language;
      if (saved === 'zh' || saved === 'en') {
        return saved;
      }
    }
    return 'zh';
  });

  // Use ref to mark if language is being changed manually, to avoid useEffect override
  const isManualLanguageChange = useRef(false);

  const docs = useMemo(() => {
    const filtered = allDocs
      .filter((doc) => doc.language === language)
      .sort((a, b) => {
        // Sort Chinese by pinyin, English by alphabet
        const locale = language === 'zh' ? 'zh-Hans' : 'en';
        return a.title.localeCompare(b.title, locale);
      });
    
    return filtered;
  }, [allDocs, language]);

  const docIds = useMemo(() => docs.map((doc) => doc.id), [docs]);
  const allDocIds = useMemo(() => allDocs.map((doc) => doc.id), [allDocs]);

  const [{ docId: initialDocId, headingSlug: initialHeadingSlug, language: initialLanguage }] =
    useState(() => parseInitialHash(allDocIds, ['zh', 'en']));

  const [activeDocId, setActiveDocId] = useState<string | undefined>(initialDocId);
  const [activeHeadingSlug, setActiveHeadingSlug] = useState<string | undefined>(
    initialHeadingSlug,
  );

  // If language is specified in URL, use it (only runs once on initialization)
  const hasAppliedInitialLanguage = useRef(false);
  useEffect(() => {
    if (!hasAppliedInitialLanguage.current && initialLanguage && initialLanguage !== language) {
      hasAppliedInitialLanguage.current = true;
      setLanguage(initialLanguage);
    }
  }, [initialLanguage, language]);

  // When language changes, ensure activeDocId is in the document list of current language
  // Note: If language is being changed manually, skip this logic
  useEffect(() => {
    if (isManualLanguageChange.current) {
      // Reset the flag
      isManualLanguageChange.current = false;
      return;
    }

    if (!activeDocId || docs.length === 0) {
      // If no activeDocId or no documents, select the first one
      if (docs.length > 0 && !activeDocId) {
        setActiveDocId(docs[0].id);
        setActiveHeadingSlug(undefined);
      }
      return;
    }
    
    // If current activeDocId is not in the document list of current language, need to switch
    const isInCurrentLanguage = docs.some((doc) => doc.id === activeDocId);
    
    if (!isInCurrentLanguage) {
      // Try to find document with the same filename
      const currentDoc = allDocs.find((doc) => doc.id === activeDocId);
      if (currentDoc) {
        const sameNameDoc = allDocs.find(
          (doc) => doc.language === language && doc.path === currentDoc.path,
        );
        if (sameNameDoc) {
          setActiveDocId(sameNameDoc.id);
          setActiveHeadingSlug(undefined);
        } else {
          // If no matching document found, select the first document of current language
          if (docs.length > 0) {
            setActiveDocId(docs[0].id);
            setActiveHeadingSlug(undefined);
          }
        }
      } else {
        // If current document not found, select the first document of current language
        if (docs.length > 0) {
          setActiveDocId(docs[0].id);
          setActiveHeadingSlug(undefined);
        }
      }
    }
  }, [language, activeDocId, docs, allDocs]);

  const activeDoc = useMemo(
    () => docs.find((doc) => doc.id === activeDocId) ?? docs[0],
    [docs, activeDocId],
  );

  useEffect(() => {
    if (!activeDocId && docs.length > 0) {
      setActiveDocId(docs[0].id);
    }
  }, [activeDocId, docs]);

  useEffect(() => {
    if (!activeDoc) {
      return;
    }
    document.title = `${activeDoc.title} · Agentscope Cookbook`;
  }, [activeDoc]);

  // Save language selection to localStorage
  useEffect(() => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('cookbook-language', language);
    }
  }, [language]);

  const handleLanguageChange = useCallback(
    (newLanguage: Language) => {
      if (newLanguage === language) {
        return;
      }
      
      // First find the document to switch to
      let targetDocId: string | undefined;
      
      // Try to find document with the same filename
      const currentDoc = activeDoc;
      if (currentDoc) {
        const basePath = currentDoc.path;
        const newDoc = allDocs.find(
          (doc) => doc.language === newLanguage && doc.path === basePath,
        );
        if (newDoc) {
          targetDocId = newDoc.id;
        }
      }
      
      // If no matching document found, select the first document of new language
      if (!targetDocId) {
        const newLanguageDocs = allDocs.filter((doc) => doc.language === newLanguage);
        if (newLanguageDocs.length > 0) {
          targetDocId = newLanguageDocs[0].id;
        }
      }
      
      // Update language and document ID
      // Mark that language is being changed manually (must be set before state update)
      isManualLanguageChange.current = true;
      
      // Set both activeDocId and language
      // React will batch these updates, so useEffect will only run once after all updates complete
      if (targetDocId) {
        setActiveDocId(targetDocId);
        setActiveHeadingSlug(undefined);
      }
      setLanguage(newLanguage);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    [language, activeDoc, activeDocId, allDocs],
  );

  useEffect(() => {
    if (!activeDoc) {
      return;
    }

    // Use new format: language:docId:headingSlug
    const docId = activeDoc.path.replace(/\.md$/i, '');
    const nextHash = activeHeadingSlug
      ? `${activeDoc.language}:${encodeURIComponent(docId)}:${encodeURIComponent(activeHeadingSlug)}`
      : `${activeDoc.language}:${encodeURIComponent(docId)}`;

    const hash = `#${nextHash}`;

    if (window.location.hash !== hash) {
      history.replaceState(null, '', hash);
    }
  }, [activeDoc, activeHeadingSlug]);

  const handleDocSelect = useCallback(
    (nextId: string) => {
      if (nextId === activeDocId) {
        return;
      }
      setActiveDocId(nextId);
      setActiveHeadingSlug(undefined);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    [activeDocId],
  );

  const handleHeadingNavigate = useCallback((slug: string) => {
    const element = document.getElementById(slug);
    if (!element) {
      return;
    }
    setActiveHeadingSlug(slug);

    const header = document.querySelector('.layout__header') as HTMLElement | null;
    const headerHeight = header?.offsetHeight ?? 64;
    const offset = 12;
    const top = element.getBoundingClientRect().top + window.scrollY - headerHeight - offset;
    window.scrollTo({ top, behavior: 'smooth' });
  }, []);

  useEffect(() => {
    if (!activeDoc) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio);

        if (visible.length > 0) {
          const currentSlug = visible[0].target.getAttribute('id') ?? undefined;
          if (currentSlug && currentSlug !== activeHeadingSlug) {
            setActiveHeadingSlug(currentSlug);
          }
          return;
        }

        const fallback = [...activeDoc.headings]
          .map((heading) => document.getElementById(heading.slug))
          .find((element) => {
            if (!element) {
              return false;
            }
            const rect = element.getBoundingClientRect();
            return rect.top >= 0 && rect.top < window.innerHeight / 2;
          });

        if (fallback) {
          const slug = fallback.getAttribute('id') ?? undefined;
          if (slug && slug !== activeHeadingSlug) {
            setActiveHeadingSlug(slug);
          }
        }
      },
      {
        rootMargin: '0px 0px -70% 0px',
        threshold: [0.1, 0.3, 0.6],
      },
    );

    const elements = activeDoc.headings
      .map((heading) => document.getElementById(heading.slug))
      .filter((element): element is HTMLElement => element !== null);

    elements.forEach((element) => observer.observe(element));

    return () => observer.disconnect();
  }, [activeDoc, activeHeadingSlug]);

  useEffect(() => {
    if (!activeDoc) {
      return;
    }

    setActiveHeadingSlug((current) => {
      if (current && activeDoc.headings.some((heading) => heading.slug === current)) {
        return current;
      }
      return undefined;
    });
  }, [activeDoc]);

  useEffect(() => {
    if (!activeDoc || !initialHeadingSlug) {
      return;
    }
    if (!activeDoc.headings.some((heading) => heading.slug === initialHeadingSlug)) {
      return;
    }

    const timer = window.setTimeout(() => handleHeadingNavigate(initialHeadingSlug), 120);

    return () => window.clearTimeout(timer);
  }, [activeDoc, handleHeadingNavigate, initialHeadingSlug]);

  return (
    <div className="layout">
      <header className="layout__header">
        <div className="layout__brand">
          <span className="layout__logo">Agentscope</span>
          <span className="layout__subtitle">Cookbook</span>
        </div>
        <div className="layout__active-title">{activeDoc?.title ?? 'Please select a document'}</div>
        <div className="layout__language-switcher">
          <button
            className={`layout__lang-btn ${language === 'zh' ? 'is-active' : ''}`}
            onClick={() => handleLanguageChange('zh')}
            aria-label="Switch to Chinese"
          >
            中文
          </button>
          <button
            className={`layout__lang-btn ${language === 'en' ? 'is-active' : ''}`}
            onClick={() => handleLanguageChange('en')}
            aria-label="Switch to English"
          >
            English
          </button>
        </div>
      </header>

      <div className="layout__body">
        <Sidebar
          items={docs.map((doc) => ({ id: doc.id, title: doc.title }))}
          activeId={activeDoc?.id}
          onSelect={handleDocSelect}
        />
        <main className="layout__content">
          {activeDoc ? (
            <MarkdownRenderer key={activeDoc.id} content={activeDoc.content} />
          ) : (
            <div className="layout__empty">No documents available</div>
          )}
        </main>
        <HeadingToc
          headings={activeDoc?.headings ?? []}
          activeSlug={activeHeadingSlug}
          onNavigate={handleHeadingNavigate}
        />
      </div>
    </div>
  );
}

export default App;

