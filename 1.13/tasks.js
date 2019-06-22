#!/usr/bin/djs
// Note for reviewers/clones: This file is a auxiliary script for my setup. It's not needed to build the mod.
"use strict";
const constants = include("../meta/lib/constants.js")();
const libtask = include("../meta/lib/libtask.js")(constants);
const liblang = include("../meta/lib/liblang.1.13.js")(constants);
var tasks = {};

tasks["sync-languages"] = function() {
  liblang.sync_languages();
};

libtask.run(tasks, sys.args);
