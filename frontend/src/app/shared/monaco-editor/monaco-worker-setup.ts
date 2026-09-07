// Configures Monaco's web workers for the esbuild-based Angular builder (no webpack loader available).
// esbuild's `new Worker(new URL(...))` detection only follows literal relative paths, so these
// point straight into node_modules rather than using the bare "monaco-editor/..." specifier.
// Only base editor + TS/JS language services are wired up; Java/Python/SQL get Monarch syntax
// highlighting only (bundled with monaco-editor), which needs no dedicated worker.
export function setupMonacoEnvironment(): void {
  (self as any).MonacoEnvironment = {
    getWorker(_moduleId: string, label: string) {
      if (label === 'typescript' || label === 'javascript') {
        return new Worker(
          new URL('../../../../node_modules/monaco-editor/esm/vs/language/typescript/ts.worker.js', import.meta.url),
          { type: 'module' }
        );
      }
      return new Worker(
        new URL('../../../../node_modules/monaco-editor/esm/vs/editor/editor.worker.js', import.meta.url),
        { type: 'module' }
      );
    },
  };
}
