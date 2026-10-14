import { useContext } from 'react'
import { DesignContext } from './context'

export function useQrDesign() {
  const value = useContext(DesignContext)
  if (!value) throw new Error('useQrDesign precisa estar dentro de <DesignProvider>')
  return value
}
