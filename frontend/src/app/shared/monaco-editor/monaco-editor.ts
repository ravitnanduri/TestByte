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
  /** 1-based line number to render invisible (used to conceal the AI-cheating trap line). */
  @Input() hiddenLineNumber: number | null = null;

  @Output() valueChange = new EventEmitter<string>();

  private editor: import('monaco-editor').editor.IStandaloneCodeEditor | undefined;
  private monacoApi: typeof import('monaco-editor') | undefined;
  private hiddenLineDecorations: import('monaco-editor').editor.IEditorDecorationsCollection | undefined;

  async ngAfterViewInit(): Promise<void> {
    if (!environmentReady) {
      setupMonacoEnvironment();
      environmentReady = true;
    }

    const monaco = await import('monaco-editor');
    this.monacoApi = monaco;

    this.editor = monaco.editor.create(this.hostRef.nativeElement, {
      value: this.value,
      language: this.language,
      readOnly: this.readOnly,
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: 14,
      scrollBeyondLastLine: false,
    });

    this.editor.onDidChangeModelContent(() => {
      this.valueChange.emit(this.editor!.getValue());
    });

    this.applyHiddenLineDecoration();
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
    if (changes['hiddenLineNumber'] || changes['value']) {
      this.applyHiddenLineDecoration();
    }
  }

  private applyHiddenLineDecoration(): void {
    if (!this.editor || !this.monacoApi) return;

    if (!this.hiddenLineDecorations) {
      this.hiddenLineDecorations = this.editor.createDecorationsCollection();
    }

    if (this.hiddenLineNumber == null) {
      this.hiddenLineDecorations.set([]);
      return;
    }

    this.hiddenLineDecorations.set([
      {
        range: new this.monacoApi.Range(this.hiddenLineNumber, 1, this.hiddenLineNumber, 1),
        options: {
          isWholeLine: true,
          inlineClassName: 'ai-trap-hidden-line',
          className: 'ai-trap-hidden-line-bg',
        },
      },
    ]);
  }

  ngOnDestroy(): void {
    this.editor?.dispose();
  }
}
