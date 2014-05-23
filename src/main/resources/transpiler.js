(function () {

//    "use strict";

    var args = process.argv,
        fs = require("fs"),
        es6Transpiler = require("es6-module-transpiler"),
        mkdirp = require("mkdirp"),
        path = require("path");

    var SOURCE_FILE_MAPPINGS_ARG = 2;
    var TARGET_ARG = 3;
    var OPTIONS_ARG = 4;

    var sourceFileMappings = JSON.parse(args[SOURCE_FILE_MAPPINGS_ARG]);
    var target = args[TARGET_ARG];
    var options = JSON.parse(args[OPTIONS_ARG]);

//    console.log(sourceFileMappings, "sourceFileMappings");
//    console.log(options, "options");


    var sourcesToProcess = sourceFileMappings.length;
    var results = [];
    var problems = [];

    function compileDone() {
        if (--sourcesToProcess === 0) {
            console.log("\u0010" + JSON.stringify({results: results, problems: problems}));
        }
    }

    var Try = function (value) {
        var tryObj = {
            getOrElse: function (val) {
                try {
                    if (typeof value == 'function') {
                        return value();
                    }
                    return value;
                } catch (e) {
                    if (typeof val == 'function') {
                        return val();
                    }
                    return val;
                }
            }
        };
        return tryObj;
    };

    var Option = function (value) {
        var opt = {
            val: function () {
                return Try(value).getOrElse(function () {
                    throw "NoSuchElementException";
                });
            },
            getVal: function () {
                var val = opt.val();
                if (opt._isEmpty(val)) {
                    throw "NoSuchElementException";
                }
                return val;
            },
            getOrElse: function (elseVal) {
                return Try(opt.getVal).getOrElse(elseVal);
            },
            _isEmpty: function (val) {
                return typeof val == 'undefined' || val === null;
            },
            isEmpty: function () {
                return opt._isEmpty(opt.val());
            },
            fold: function (zero) {
                return function (transform) {
                    var val = opt.val();
                    if (opt._isEmpty(val)) {
                        return zero;
                    } else if (typeof transform == 'function') {
                        return transform(val);
                    } else {
                        return transform;
                    }
                }
            }
        };
        return opt;
    };


    function compile(compiler, opts) {
        var moduleType = Option(function () {
            return opts.moduleType;
        }).getOrElse('AMD');

        if (moduleType == 'CJS') {
            return compiler.toCJS();
        } else if (moduleType == 'YUI') {
            return compiler.toYUI();
        } else {
            return compiler.toAMD();
        }
    }

    function throwIfErr(e) {
        if (e) throw e;
    }

    sourceFileMappings.forEach(function (sourceFileMapping) {

        var input = sourceFileMapping[0];

        // Hopefully always 'js/moduleName'
        var outputFile = sourceFileMapping[1];
        var output = path.join(target, outputFile);

        var moduleName = outputFile.replace(/^js\//i, '').replace(/\.js$/i, '');
        moduleName = Option(function () {
            return options.prefix;
        }).fold(moduleName)(function (prefix) {
            return prefix + "/" + moduleName;
        });

        mkdirp(path.dirname(output), function (e) {
            fs.readFile(input, "utf8", function (e, contents) {
                throwIfErr(e);

                var Compiler = es6Transpiler.Compiler;

                try {
                    var compiled = compile(new Compiler(contents, moduleName, {}), options);
                    fs.writeFile(output, compiled, "utf8", function (e) {
                        throwIfErr(e);

                        results.push({
                            source: input,
                            result: {
                                filesRead: [input],
                                filesWritten: [output]
                            }
                        });

                        compileDone();
                    });
                } catch (e) {
                    problems.push({
                        message: err.message,
                        severity: "error",
                        lineNumber: err.location.first_line,
                        characterOffset: err.location.first_column,
                        lineContent: contents.split("\n")[err.location.first_line],
                        source: input
                    });
                    results.push({
                        source: input,
                        result: null
                    });
                    compileDone();
                }
            });
        });
    });
})();