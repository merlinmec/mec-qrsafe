import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Sem os globais do Vitest o Testing Library nao limpa o DOM sozinho entre testes.
afterEach(() => cleanup())
