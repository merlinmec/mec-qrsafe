import { createContext, type Dispatch } from 'react'
import type { DesignAction, QrDesign } from './state'

export const DesignContext = createContext<{ design: QrDesign; dispatch: Dispatch<DesignAction> } | null>(null)
