import { defineShikiSetup } from '@slidev/types'

/**
 * JetBrains "Islands Dark" / Darcula syntax colours, as a TextMate theme for Shiki.
 * Mirrors the new-UI Darcula scheme so code blocks read like the IDE:
 *   keyword  #CF8E6D (orange)   string   #6AAB73 (green)    number  #2AACB8 (teal)
 *   comment  #7A7E85 (grey)     function #56A8F5 (blue)     const   #C77DBB (purple)
 *   annotation #B3AE60 (olive)  tag      #E8BF6A (gold)     default #BCBEC4
 */
const darcula = {
  name: 'islands-darcula',
  type: 'dark',
  colors: {
    'editor.background': '#1E1F22',
    'editor.foreground': '#BCBEC4',
  },
  settings: [
    { settings: { background: '#1E1F22', foreground: '#BCBEC4' } },

    // Comments
    {
      scope: ['comment', 'punctuation.definition.comment', 'string.comment'],
      settings: { foreground: '#7A7E85' },
    },

    // Keywords, storage, modifiers, control flow
    {
      scope: [
        'keyword',
        'keyword.control',
        'keyword.other',
        'storage',
        'storage.type',
        'storage.modifier',
        'constant.language', // true / false / null
        'keyword.operator.new',
        'variable.language.this',
      ],
      settings: { foreground: '#CF8E6D' },
    },

    // Strings + escapes
    {
      scope: [
        'string',
        'string.quoted',
        'string.template',
        'punctuation.definition.string',
      ],
      settings: { foreground: '#6AAB73' },
    },
    {
      scope: ['constant.character.escape', 'string.regexp'],
      settings: { foreground: '#CF8E6D' },
    },

    // Numbers
    {
      scope: ['constant.numeric', 'constant.language.boolean'],
      settings: { foreground: '#2AACB8' },
    },

    // Functions / method calls
    {
      scope: [
        'entity.name.function',
        'support.function',
        'meta.function-call',
        'meta.function-call.generic',
        'entity.name.function.call',
      ],
      settings: { foreground: '#56A8F5' },
    },

    // Constants, enum members, static-ish
    {
      scope: [
        'variable.other.constant',
        'constant.other',
        'variable.other.enummember',
        'entity.name.constant',
      ],
      settings: { foreground: '#C77DBB' },
    },

    // Properties / instance fields
    {
      scope: ['variable.other.property', 'variable.other.object.property', 'meta.property.object'],
      settings: { foreground: '#C77DBB' },
    },

    // Annotations (@Composable, @Inject, …)
    {
      scope: [
        'meta.annotation',
        'storage.type.annotation',
        'punctuation.definition.annotation',
        'entity.name.function.annotation',
        'meta.declaration.annotation',
      ],
      settings: { foreground: '#B3AE60' },
    },

    // Types / classes stay default-fg in Darcula, but keep support types subtle
    {
      scope: ['entity.name.type', 'entity.name.class', 'support.type', 'support.class'],
      settings: { foreground: '#BCBEC4' },
    },

    // Default variables / parameters
    {
      scope: ['variable', 'variable.other', 'variable.parameter', 'meta.parameter'],
      settings: { foreground: '#BCBEC4' },
    },

    // Markup: XML / HTML tags + attributes
    {
      scope: ['entity.name.tag', 'punctuation.definition.tag'],
      settings: { foreground: '#E8BF6A' },
    },
    {
      scope: ['entity.other.attribute-name'],
      settings: { foreground: '#BCBEC4' },
    },

    // Operators / punctuation stay neutral, like the IDE
    {
      scope: ['keyword.operator', 'punctuation'],
      settings: { foreground: '#BCBEC4' },
    },
  ],
}

export default defineShikiSetup(() => ({
  themes: {
    dark: darcula,
    light: darcula,
  },
}))
