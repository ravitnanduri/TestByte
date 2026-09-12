/**
 * Renders a small, fixed markdown-lite subset to safe HTML. The raw text is HTML-escaped first, so
 * nothing in it can inject live markup -- only the syntax below is ever turned back into tags. Kept in
 * sync with the toolbar in shared/prompt-editor/prompt-editor.ts.
 */
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderInline(escapedLine: string): string {
  return escapedLine
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>');
}

export function renderMarkdown(raw: string | null | undefined): string {
  if (!raw) return '';
  const escaped = escapeHtml(raw);
  const blocks = escaped.split(/\n{2,}/);

  return blocks
    .map((block) => {
      const lines = block.split('\n').filter((l) => l.length > 0);
      if (lines.length === 0) return '';

      if (lines.every((l) => /^- /.test(l))) {
        const items = lines.map((l) => `<li>${renderInline(l.slice(2))}</li>`).join('');
        return `<ul>${items}</ul>`;
      }

      if (lines.every((l) => /^\d+\.\s/.test(l))) {
        const items = lines.map((l) => `<li>${renderInline(l.replace(/^\d+\.\s/, ''))}</li>`).join('');
        return `<ol>${items}</ol>`;
      }

      return `<p>${lines.map(renderInline).join('<br>')}</p>`;
    })
    .join('');
}
