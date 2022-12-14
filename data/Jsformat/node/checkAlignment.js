"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _iterateJsdoc = _interopRequireDefault(require("../iterateJsdoc"));
function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }
const trimStart = string => {
  return string.replace(/^\s+/u, '');
};
var _default = (0, _iterateJsdoc.default)(({
  sourceCode,
  jsdocNode,
  report,
  indent
}) => {
  // `indent` is whitespace from line 1 (`/**`), so slice and account for "/".
  const indentLevel = indent.length + 1;
  const sourceLines = sourceCode.getText(jsdocNode).split('\n').slice(1).map(line => {
    return line.split('*')[0];
  }).filter(line => {
    return !trimStart(line).length;
  });
  const fix = fixer => {
    const replacement = sourceCode.getText(jsdocNode).split('\n').map((line, index) => {
      // Ignore the first line and all lines not starting with `*`
      const ignored = !index || trimStart(line.split('*')[0]).length;
      return ignored ? line : `${indent} ${trimStart(line)}`;
    }).join('\n');
    return fixer.replaceText(jsdocNode, replacement);
  };
  sourceLines.some((line, lineNum) => {
    if (line.length !== indentLevel) {
      report('Expected JSDoc block to be aligned.', fix, {
        line: lineNum + 1
      });
      return true;
    }
    return false;
  });
}, {
  iterateAllJsdocs: true,
  meta: {
    docs: {
      description: 'Reports invalid alignment of JSDoc block asterisks.',
      url: 'https://github.com/gajus/eslint-plugin-jsdoc#eslint-plugin-jsdoc-rules-check-alignment'
    },
    fixable: 'code',
    type: 'layout'
  }
});
exports.default = _default;
module.exports = exports.default;
//# sourceMappingURL=checkAlignment.js.map