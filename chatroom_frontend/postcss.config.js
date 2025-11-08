const purgecss = require('@fullhuman/postcss-purgecss').default

const plugins = []

if (process.env.NODE_ENV === 'production') {
  plugins.push(
    purgecss({
      content: [
        './public/index.html',
        './src/**/*.vue',
        './src/**/*.js'
      ],
      defaultExtractor: content => content.match(/[A-Za-z0-9-_:/]+/g) || [],
      safelist: [
        /^(btn|badge|table|alert|pagination|modal|nav|card|form|col|row|text-|bg-|d-|flex|gap-)/
      ]
    })
  )
}

module.exports = { plugins }