#!/usr/bin/djs
"use strict";

if((!fs.chdir(fs.dirname(fs.realpath(sys.script))+"/..")) || (!fs.isdir("src"))) throw new Error("Failed to switch to mod base directory.");

var version_minecraft="";
var version_forge="";
var version_engineersdecor="";

fs.readfile("gradle.properties", function(line){
  if(line.search(/^[\s]*version_minecraft[\s]*=/i) >= 0) {
    version_minecraft = line.replace(/^[^=]+=/,"").trim();
  } else if(line.search(/^[\s]*version_forge[\s]*=/i) >= 0) {
    version_forge = line.replace(/^[^=]+=/,"").trim();
  } else if(line.search(/^[\s]*version_engineersdecor[\s]*=/i) >= 0) {
    version_engineersdecor = line.replace(/^[^=]+=/,"").trim();
  }
  return false;
})

const combined_version = version_minecraft + "-" + version_engineersdecor;

var readme_version_found = fs.readfile("readme.md", function(line){
  var m = line.match(/^[\s]+-[\s]+v([\d]+[\.][\d]+[\.][\d]+[-][abrc][\d]+)/i);
  if((!m) || (!m.length) || (m.length < 2)) {
    m = line.match(/^[\s]+-[\s]+v([\d]+[\.][\d]+[\.][\d]+)/i);
    if((!m) || (!m.length) || (m.length < 2)) return false;
  }
  return m[1]==version_engineersdecor;
});

var ok=true;
if(!readme_version_found) {
  alert("Version 'v" + version_engineersdecor + "' not found in the readme changelog.");
  ok = false;
}
if(!ok) {
  alert("Version data:");
  alert(" - combined_version  : '" + combined_version + "'");
  alert(" - version_forge     : '" + version_forge + "'");
  exit(1);
}
