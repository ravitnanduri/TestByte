import { Component, ElementRef, ViewChild, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MarkdownPipe } from '../markdown/markdown.pipe';

/**
 * Plain-text question/instructions editor with a lightweight formatting toolbar (bold/italic/code/
 * lists), so recruiters get basic formatting without needing to hand-write HTML. Implements
 * ControlValueAccessor so it drops into a reactive form like any other control (formControlName="prompt").
 */
@Component({
  selector: 'app-prompt-editor',
  imports: [MarkdownPipe],
  template: `
    <div class="prompt-editor">
      <div class="prompt-toolbar">
        <button type="button" class="btn btn-secondary" (click)="wrap('**')" title="Bold">B</button>
        <button type="button" class="btn btn-secondary" (click)="wrap('*')" title="Italic"><em>I</em></button>
        <button type="button" class="btn btn-secondary" (click)="wrap('\`')" title="Code">Code</button>
        <button type="button" class="btn btn-secondary" (click)="prefixLines('- ')" title="Bullet list">
          &bull; List
        </button>
        <button type="button" class="btn btn-secondary" (click)="prefixLines('', true)" title="Numbered list">
          1. List
        </button>
        <span class="prompt-toolbar-spacer"></span>
        <button type="button" class="btn btn-secondary" (click)="showPreview.set(!showPreview())">
          {{ showPreview() ? 'Edit' : 'Preview' }}
        </button>
      </div>
      @if (showPreview()) {
        <div class="prompt-preview" [innerHTML]="text() | markdown"></div>
      } @else {
        <textarea
          #ta
          rows="6"
          [value]="text()"
          (input)="onInput($any($event.target).value)"
          placeholder="Question text or instructions..."
        ></textarea>
      }
    </div>
  `,
  styles: [
    `
      .prompt-toolbar {
        display: flex;
        gap: 6px;
        margin-bottom: 6px;
        flex-wrap: wrap;
      }
      .prompt-toolbar .btn {
        padding: 4px 10px;
        font-size: 12px;
      }
      .prompt-toolbar-spacer {
        flex: 1;
      }
      textarea {
        width: 100%;
        padding: 10px 12px;
        border: 1px solid var(--color-border, #e2e8f0);
        border-radius: 8px;
        font-size: 14px;
        font-family: inherit;
      }
      .prompt-preview {
        min-height: 100px;
        padding: 10px 12px;
        border: 1px dashed var(--color-border, #e2e8f0);
        border-radius: 8px;
        font-size: 14px;
      }
    `,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PromptEditor),
      multi: true,
    },
  ],
})
export class PromptEditor implements ControlValueAccessor {
  @ViewChild('ta') private textareaRef?: ElementRef<HTMLTextAreaElement>;

  text = signal('');
  showPreview = signal(false);

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: string): void {
    this.text.set(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  onInput(value: string): void {
    this.apply(value);
  }

  wrap(marker: string): void {
    const ta = this.textareaRef?.nativeElement;
    if (!ta) return;
    const { selectionStart, selectionEnd, value } = ta;
    const selected = value.slice(selectionStart, selectionEnd) || 'text';
    const next = value.slice(0, selectionStart) + marker + selected + marker + value.slice(selectionEnd);
    this.applyAndFocus(ta, next, selectionStart + marker.length, selectionStart + marker.length + selected.length);
  }

  prefixLines(prefix: string, numbered = false): void {
    const ta = this.textareaRef?.nativeElement;
    if (!ta) return;
    const { selectionStart, selectionEnd, value } = ta;
    const lineStart = value.lastIndexOf('\n', selectionStart - 1) + 1;
    let lineEnd = value.indexOf('\n', selectionEnd);
    if (lineEnd === -1) lineEnd = value.length;

    const lines = value.slice(lineStart, lineEnd).split('\n');
    const transformed = lines.map((line, i) => (numbered ? `${i + 1}. ${line}` : `${prefix}${line}`)).join('\n');
    const next = value.slice(0, lineStart) + transformed + value.slice(lineEnd);
    this.applyAndFocus(ta, next, lineStart, lineStart + transformed.length);
  }

  private applyAndFocus(ta: HTMLTextAreaElement, next: string, selStart: number, selEnd: number): void {
    this.apply(next);
    queueMicrotask(() => {
      ta.value = next;
      ta.focus();
      ta.setSelectionRange(selStart, selEnd);
    });
  }

  private apply(value: string): void {
    this.text.set(value);
    this.onChange(value);
    this.onTouched();
  }
}
