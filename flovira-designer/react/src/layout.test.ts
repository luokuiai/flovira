import { readFileSync } from 'node:fs'
import { expect, test } from 'vitest'

test('designer fills its parent without a fixed or viewport-based height floor', () => {
  const css = readFileSync(new URL('./styles.css', import.meta.url), 'utf8')
  const root = css.match(/\.flovira-react-designer\s*\{([^}]+)\}/)![1]
  expect(root).toMatch(/\n\s*height: 100%;/)
  expect(root).toMatch(/\n\s*min-height: 0;/)
  expect(root).not.toMatch(/\b(?:d?vh|560px|820px)\b/)
  expect(css.match(/\.frd-workspace\s*\{([^}]+)\}/)![1]).toContain('min-height: 0;')
  expect(css.match(/\.flovira-react-canvas\s*\{([^}]+)\}/)![1]).toContain('overflow: auto;')
})

test('embedded appearance removes only the outer card decoration', () => {
  const css = readFileSync(new URL('./styles.css', import.meta.url), 'utf8')
  const embedded = css.match(/\.flovira-react-designer\[data-appearance='embedded'\]\s*\{([^}]+)\}/)![1]
  expect(embedded).toContain('border: 0;')
  expect(embedded).toContain('border-radius: 0;')
  expect(embedded).toContain('box-shadow: none;')
  expect(css.match(/\.flovira-react-designer\s*\{([^}]+)\}/)![1]).toContain('border-radius: 8px;')
})

test('sizes each branch to its own content instead of stretching siblings equally', () => {
  const css = readFileSync(new URL('./styles.css', import.meta.url), 'utf8')
  const grid = css.match(/\.flovira-react-branch-grid\s*\{([^}]+)\}/)![1]
  expect(grid).toContain('grid-auto-columns: minmax(var(--frd-branch-column-width), max-content);')
  expect(grid).not.toContain('1fr')
  expect(grid).toContain('align-items: stretch;')
  expect(grid).toContain('gap: 40px;')
})
