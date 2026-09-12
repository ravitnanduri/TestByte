import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { renderMarkdown } from './render-markdown';

@Pipe({ name: 'markdown', standalone: true })
export class MarkdownPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(raw: string | null | undefined): SafeHtml {
    // Safe to bypass sanitization here: renderMarkdown only ever emits a fixed set of tags
    // (p/br/ul/ol/li/code/strong/em) built from HTML-escaped input -- see render-markdown.ts.
    return this.sanitizer.bypassSecurityTrustHtml(renderMarkdown(raw));
  }
}
