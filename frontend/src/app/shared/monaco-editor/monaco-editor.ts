import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { setupMonacoEnvironment } from './monaco-worker-setup';

let environmentReady = false;
const injectedColorClasses = new Set<string>();

/**
 * Monaco's theme is a single global concept shared by every editor instance on the page, so it can't be
 * used for a per-question font color (a test page can show several CODE questions with different colors
 * at once, e.g. in the authoring form). A CSS class + full-document inline decoration is applied per
 * editor instance instead, injected once per distinct color into a shared <style> tag.
 */
function colorClassFor(color: string): string {
  const className = `monaco-force-color-${color.replace(/[^a-zA-Z0-9]/g, '')}`;
  if (!injectedColorClasses.has(className)) {
    const style = document.createElement('style');
    style.textContent = `.${className} { color: ${color} !important; }`;
    document.head.appendChild(style);
    injectedColorClasses.add(className);
  }
  return className;
}

@Component({
  selector: 'app-monaco-editor',
  template: `<div #host class="monaco-host"></div>`,
  styles: [
    `
      .monaco-host {
        position: relative;
        width: 100%;
        height: 100%;
        min-height: 320px;
        border: 1px solid var(--color-border, #e2e8f0);
        border-radius: 8px;
        overflow: hidden;
      }
    `,
  ],
})
export class MonacoEditor implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('host', { static: true }) hostRef!: ElementRef<HTMLDivElement>;

  @Input() value = '';
  @Input() language = 'plaintext';
  @Input() readOnly = false;
  @Input() fontSize?: number | null;
  @Input() fontColor?: string | null;

  @Output() valueChange = new EventEmitter<string>();
  /**
   * Fires on every paste Monaco recognizes, via Monaco's own `onDidPaste` API rather than a plain
   * `document`-level `paste` listener. Monaco's newer input path (the EditContext API some browsers use
   * for the actual text-insertion side of typing/pasting) doesn't reliably bubble a native ClipboardEvent
   * up to `document` the way a plain <textarea> does, so a page-level paste listener alone can silently
   * miss pastes into this editor -- this is the reliable source of truth for "was something pasted here".
   * Emits the text that was actually inserted (read back from the model via the paste event's range,
   * not the source ClipboardEvent, since that's not always populated) so a caller can tell an external
   * paste apart from one that just re-inserts something copied from elsewhere on the same page.
   */
  @Output() pasteDetected = new EventEmitter<string>();
  @Output() copyDetected = new EventEmitter<string>();
  @Output() cutDetected = new EventEmitter<string>();

  private editor: import('monaco-editor').editor.IStandaloneCodeEditor | undefined;
  private colorDecorations: import('monaco-editor').editor.IEditorDecorationsCollection | undefined;

  async ngAfterViewInit(): Promise<void> {
    if (!environmentReady) {
      setupMonacoEnvironment();
      environmentReady = true;
    }

    const monaco = await import('monaco-editor');

    this.editor = monaco.editor.create(this.hostRef.nativeElement, {
      value: this.value,
      language: this.language,
      readOnly: this.readOnly,
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: this.fontSize ?? 14,
      scrollBeyondLastLine: false,
    });

    this.editor.onDidChangeModelContent(() => {
      this.valueChange.emit(this.editor!.getValue());
      this.applyFontColor();
    });

    this.editor.onDidPaste((e) => {
      const pasted = this.editor?.getModel()?.getValueInRange(e.range) ?? '';
      this.pasteDetected.emit(pasted);
    });

    // Monaco has no public onDidCopy/onDidCut, so these read the model's selection directly on the
    // native browser copy/cut event -- the selection is still intact at that point.
    this.hostRef.nativeElement.addEventListener('copy', () => this.emitSelection(this.copyDetected));
    this.hostRef.nativeElement.addEventListener('cut', () => this.emitSelection(this.cutDetected));

    this.applyFontColor();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.editor) return;

    if (changes['value'] && this.value !== this.editor.getValue()) {
      this.editor.setValue(this.value ?? '');
    }
    if (changes['language']) {
      const model = this.editor.getModel();
      if (model) {
        import('monaco-editor').then((monaco) => monaco.editor.setModelLanguage(model, this.language));
      }
    }
    if (changes['readOnly']) {
      this.editor.updateOptions({ readOnly: this.readOnly });
    }
    if (changes['fontSize']) {
      this.editor.updateOptions({ fontSize: this.fontSize ?? 14 });
    }
    if (changes['fontColor']) {
      this.applyFontColor();
    }
  }

  private applyFontColor(): void {
    if (!this.editor) return;
    const model = this.editor.getModel();
    if (!model) return;

    if (!this.fontColor) {
      this.colorDecorations?.clear();
      return;
    }

    const lastLine = model.getLineCount();
    const lastColumn = model.getLineMaxColumn(lastLine);
    const decoration = {
      range: { startLineNumber: 1, startColumn: 1, endLineNumber: lastLine, endColumn: lastColumn },
      options: { inlineClassName: colorClassFor(this.fontColor) },
    };

    if (this.colorDecorations) {
      this.colorDecorations.set([decoration]);
    } else {
      this.colorDecorations = this.editor.createDecorationsCollection([decoration]);
    }
  }

  private emitSelection(emitter: EventEmitter<string>): void {
    const selection = this.editor?.getSelection();
    const text = selection ? (this.editor?.getModel()?.getValueInRange(selection) ?? '') : '';
    if (text) emitter.emit(text);
  }

  ngOnDestroy(): void {
    this.editor?.dispose();
  }
}
