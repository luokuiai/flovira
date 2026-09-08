import { readFileSync, writeFileSync } from 'node:fs'
import { pathToFileURL } from 'node:url'

/** Migrate only workflow references; business variables and extension data remain untouched. */
export function migrateFormReferences(definition, mappings) {
  if (!definition || typeof definition !== 'object' || Array.isArray(definition)
    || !Array.isArray(definition.nodeList)) throw new Error('Expected a workflow definition with nodeList')
  const result = structuredClone(definition)
  for (const owner of [result, ...result.nodeList]) {
    if (!owner || typeof owner !== 'object' || Array.isArray(owner)) throw new Error('Invalid workflow node')
    const legacyPath = owner.formPath
    if (legacyPath != null && legacyPath !== '') {
      const matches = mappings.filter((entry) => entry.formPath === String(legacyPath)
        && (entry.formCustom ?? null) === (owner.formCustom ?? null))
      if (matches.length !== 1) throw new Error(`Expected exactly one mapping for ${owner.nodeCode || 'definition'}: ${legacyPath}`)
      const formId = matches[0].formId
      if (typeof formId !== 'string' || !formId.trim() || formId.length > 100) throw new Error(`Invalid formId for ${legacyPath}`)
      if (owner.formId != null && owner.formId !== '' && owner.formId !== formId) throw new Error(`Conflicting formId for ${legacyPath}`)
      owner.formId = formId
    }
    delete owner.formPath
    delete owner.formCustom
  }
  delete result.formPathList
  return result
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const [input, mappingFile, output] = process.argv.slice(2)
  if (!input || !mappingFile || !output) {
    throw new Error('Usage: node scripts/migrate-form-references.mjs input.json mappings.json output.json')
  }
  const definition = JSON.parse(readFileSync(input, 'utf8'))
  const mappings = JSON.parse(readFileSync(mappingFile, 'utf8'))
  if (!Array.isArray(mappings)) throw new Error('Mappings must be an array')
  // Refuse to overwrite any existing file, including the input or mapping file.
  writeFileSync(output, JSON.stringify(migrateFormReferences(definition, mappings), null, 2) + '\n', { flag: 'wx' })
}
