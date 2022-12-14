import buildmessage from '../utils/buildmessage.js';
import {
  optimisticReadFile,
  optimisticHashOrNull,
} from "../fs/optimistic";
import {
  convertToPosixPath
} from '../fs/files';
const buildPluginModule = require('./build-plugin.js');

class InputFile extends buildPluginModule.InputFile {
  constructor(source, options = {}) {
    super();

    this._source = source;
    this._arch = options.arch;
    this._watchSet = options.watchSet;
    this._minifiedFiles = [];
  }

  getContentsAsBuffer() {
    return this._source.contents();
  }
  getPathInPackage() {
    throw new Error("Compiled files don't belong to any package");
  }
  getPackageName() {
    throw new Error("Compiled files don't belong to any package");
  }
  getSourceHash() {
    return this._source.hash();
  }
  getArch() {
    return this._arch;
  }
  getSourcePath() {
    return this._source.sourcePath || null;
  }

  error({message, sourcePath, line, column, func}) {
    const relPath = this.getPathInBundle();
    buildmessage.error(message || ('error minifying ' + relPath), {
      file: sourcePath || relPath,
      line: line ? line : undefined,
      column: column ? column : undefined,
      func: func ? func : undefined
    });
  }

  /**
   * @summary Returns the path of the compiled file in the bundle.
   * @memberof InputFile
   * @returns {String}
   */
  getPathInBundle() {
    return this._source.targetPath;
  }

  /**
   * @summary Returns the source-map associated with the file.
   * @memberof InputFile
   * @returns {String}
   */
  getSourceMap() {
    return this._source.sourceMap;
  }
}

export class JsFile extends InputFile {
  // - data
  // - sourceMap
  // - path
  // - hash?
  // - stats?
  addJavaScript(options) {
    this._minifiedFiles.push({ ...options });
  }
}

export class CssFile extends InputFile {
  // - data
  // - sourceMap
  // - path
  // - hash?
  // - stats?
  addStylesheet(options) {
    this._minifiedFiles.push({ ...options });
  }

  readAndWatchFileWithHash(path) {
    const filePath = convertToPosixPath(path);

    const hash = optimisticHashOrNull(filePath);
    const contents = optimisticReadFile(filePath);
    this._watchSet.addFile(filePath, hash);

    return {
      hash,
      contents
    }
  }
}
