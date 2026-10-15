/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Origem da API quando o editor é servido de outro lugar (docs/DEMO.md); vazio = mesma origem. */
  readonly VITE_API_ORIGIN?: string
}
