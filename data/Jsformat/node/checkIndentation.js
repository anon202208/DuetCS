"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _iterateJsdoc = _interopRequireDefault(require("../iterateJsdoc"));
function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }
const maskExcludedContent = (str, excludeTags) => {
  const regContent = new RegExp(`([ \\t]+\\*)[ \\t]@(?:${excludeTags.join('|')})(?=[ \\n])([\\w|\\W]*?\\n)(?=[ \\t]*\\*(?:[ \\t]*@\\w+\\s|\\/))`, 'gu');
  return str.replace(regContent, (_match, margin, code) => {
    return (margin + '\n').repeat(code.match(/\n/gu).length);
  });
};
const maskCodeBlocks = str => {
  const regContent = /([ \t]+\*)[ \t]```[^\n]*?([\w|\W]*?\n)(?=[ \t]*\*(?:[ \t]*(?:```|@\w+\s)|\/))/gu;
  return str.replace(regContent, (_match, margin, code) => {
    return (margin + '\n').repeat(code.match(/\n/gu).length);
  });
};
var _default = (0, _iterateJsdoc.default)(({
  sourceCode,
  jsdocNode,
  report,
  context
}) => {
  const options = context.options[0] || {};
  const {
    excludeTags = ['example']
  } = options;
  const reg = /^(?:\/?\**|[ \t]*)\*[ \t]{2}/gmu;
  const textWithoutCodeBlocks = maskCodeBlocks(sourceCode.getText(jsdocNode));
  const text = excludeTags.length ? maskExcludedContent(textWithoutCodeBlocks, excludeTags) : textWithoutCodeBlocks;
  if (reg.test(text)) {
    const lineBreaks = text.slice(0, reg.lastIndex).match(/\n/gu) || [];
    report('There must be no indentation.', null, {
      line: lineBreaks.length
    });
  }
}, {
  iterateAllJsdocs: true,
  meta: {
    docs: {
      description: 'Reports invalid padding inside JSDoc blocks.',
      url: 'https://github.com/gajus/eslint-plugin-jsdoc#eslint-plugin-jsdoc-rules-check-indentation'
    },
    schema: [{
      additionalProperties: false,
      properties: {
        excludeTags: {
          items: {
            pattern: '^\\S+$',
            type: 'string'
          },
          type: 'array'
        }
      },
      type: 'object'
    }],
    type: 'layout'
  }
});
exports.default = _default;
module.exports = exports.default;
//# sourceMappingURL=checkIndentation.js.map