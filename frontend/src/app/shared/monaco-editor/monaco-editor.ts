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

  @Output() valueChange = new EventEmitter<string>();

  private editor: import('monaco-editor').editor.IStandaloneCodeEditor | undefined;

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
      fontSize: 14,
      scrollBeyondLastLine: false,
    });

    this.editor.onDidChangeModelContent(() => {
      this.valueChange.emit(this.editor!.getValue());
    });
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
  }

  ngOnDestroy(): void {
    this.editor?.dispose();
  }
}
