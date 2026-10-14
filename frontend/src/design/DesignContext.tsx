import { useReducer, type ReactNode } from 'react'
import { DesignContext } from './context'
import { designReducer, initialDesign, type QrDesign } from './state'

// Context + useReducer (seção 7): uma página só, sem estado entre rotas.
export function DesignProvider({ children, initial = initialDesign }: { children: ReactNode; initial?: QrDesign }) {
  const [design, dispatch] = useReducer(designReducer, initial)
  return <DesignContext.Provider value={{ design, dispatch }}>{children}</DesignContext.Provider>
}
