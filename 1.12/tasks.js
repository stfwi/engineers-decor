#!/usr/bin/djs
// Note for reviewers/clones: This file is a auxiliary script for my setup. It's not needed to build the mod.
"use strict";
const constants = include("../meta/lib/constants.js")();
const libtask = include("../meta/lib/libtask.js")(constants);
const liblang = include("../meta/lib/liblang.1.12.js")(constants);
const liblang13 = include("../meta/lib/liblang.1.13.js")(constants);
const liblang14 = include("../meta/lib/liblang.1.13.js")(constants);
var tasks = {};

tasks["sync-languages"] = function() {
  liblang.sync_languages();
};

tasks["port-languages"] = function() {
  fs.find("src/main/resources/assets/"+ constants.mod_registry_name() +"/lang", '*.lang', function(path){
    const unified = liblang.load(path);
    path = path.replace(/\.lang$/,"");
    liblang13.save("../1.13/"+path+".json", unified);
    liblang14.save("../1.14/"+path+".json", unified);
    return false;
  });
};

libtask.run(tasks, sys.args);
