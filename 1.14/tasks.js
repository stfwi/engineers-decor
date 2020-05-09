#!/usr/bin/djs
"use strict";
const constants  = include("../meta/lib/constants.js")();
const libtask    = include("../meta/lib/libtask.js")(constants);
libtask.run({}, sys.args);
